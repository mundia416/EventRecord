package com.nosetrap.eventrecordlib.recorders

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.WindowManager
import com.nosetrap.eventrecordlib.*
import com.nosetrap.storage.sql.CursorCallback
import com.nosetrap.storage.sql.EasyCursor
import com.nosetrap.eventrecordlib.PointMove
import com.nosetrap.eventrecordlib.Point

/**
 * records the movement of a view which is an overlay
 *
 * it records the movement by storing the elapsedTimeMillis that the view spend at a specific XY coordinate
 *
 * @WARNING each view can only be assigned 1 recorder
 *
 * make sure a view is assinged to the recorder before using it
 *
 */
class OverlayMovementRecorder(private val context: Context,private val recorderCallback: RecorderCallback)
    : BaseRecorder(context) {
    private val tableName: String


    private val colX = "x"
    private val colY = "y"
    private val colDuration = "elapsedTimeMillis"

   // private var lastStoredX = 0
    //private var lastStoredY = 0

    private var layoutParams: WindowManager.LayoutParams? = null

    init {
        val recorderManager = RecorderManager.getInstance(context)
        tableName = "overlay_view_record_data_${recorderManager.getActiveOverlayRecorderCount()}"
        recorderManager.overlayRecorderCreated(tableName)
        databaseHandler.createTable(tableName, arrayOf(colX, colY, colDuration), null)
        setTableName(tableName)
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * assign this recorder to a view. {its actually assinged to a views layout params
     */
     fun assignView(layoutParams: WindowManager.LayoutParams) {
        if (this.layoutParams != null) {
            if (this.layoutParams != layoutParams) {
                throw IllegalStateException("attempted to assign layout params to a recorder that already" +
                        "has a different layout param assigned to it")
            }
        } else {
            this.layoutParams = layoutParams
        }

    }

    override fun stopPlayback() {
        super.stopPlayback()
        recorderCallback.onPlaybackStopped()
    }

    /**
     * start Recording the movements
     * playback is stopped if it is running when this method is called
     */
    override fun startRecording() {
        if (this.layoutParams == null) {
            throw IllegalStateException("attempted to play without a view(WindowManager.LayoutParams) attached to recorder")
        } else {
            xValues = ArrayList()
            yValues = ArrayList()
            elapsedTimeValues = ArrayList()
            super.startRecording()
            recorderCallback.onRecordingStarted()

            saveCurrentXY()
        }

    }

    /**
     * call when the views location changes in order to update the recording data
     * @return true if its in recording mode and false if not
     */
    fun viewLocationChanged(): Boolean {
        return if (isInRecordMode) {
            if (xValues[xValues.lastIndex] != layoutParams!!.x || yValues[yValues.lastIndex] != layoutParams!!.y){
                saveElapsedTime()
                saveCurrentXY()
            }
            true
        } else {
            false
        }
    }

    /**
     * save the time that has elapsed since the last recorded value
     */
    private fun saveElapsedTime(){
        val elapsedTime = calculateElapsedTime()
        elapsedTimeValues.add(elapsedTime)
        resetElapsedTime()
    }

    private fun saveRecordingToDatabase(){
        for(i in 0..(xValues.size - 1)){
            val values = ContentValues()
            values.put(colX,xValues[i])
            values.put(colY,yValues[i])
            values.put(colDuration,elapsedTimeValues[i])

            databaseHandler.insert(tableName,values)

            //setting the save progress
            val c: Double = (i.toDouble()/xValues.size.toDouble())
            val percentageProgress: Double = c * 100
            recorderCallback.onRecordingSaveProgress(percentageProgress)
        }

        recorderCallback.onRecordingSaved()
    }

    /**
     * stop recording movements of the view
     */
    override fun stopRecording() {
        super.stopRecording()
        recorderCallback.onRecordingStopped()
        saveElapsedTime()
        saveRecordingToDatabase()
    }

    private var xValues = ArrayList<Int>()
    private var yValues = ArrayList<Int>()
    private var elapsedTimeValues = ArrayList<Long>()

    /**
     * save the current x/y coordinates into the database
     */
    private fun saveCurrentXY() {
        xValues.add(layoutParams!!.x)
        yValues.add(layoutParams!!.y)
    }


    /**
     * playback the recorded movements onto the view
     * recording is stopped if this method is called while recording
     * @param view the view to which the layout params are attached to
     */
     fun startPlayback(view: View, onPointChangeListener: OnPointChangeListener? = null) {
        if (!isInPlayBackMode) {
            if (this.layoutParams == null) {
                throw IllegalStateException("attempted to playBack without a view(WindowManager.LayoutParams) " +
                        "attached to recorder")
            } else {
                isInPlayBackMode = true

                recorderCallback.onPrePlayback()

                // ensures that recording is turned off
                stopRecording()

                val pointMoves = ArrayList<PointMove>()

                databaseHandler.getAll(object : CursorCallback {
                    override fun onCursorQueried(cursor: EasyCursor) {
                        cursor.moveToFirst()

                        for (i in cursor.getCount() downTo 1) {
                            val x = cursor.getString(colX)
                            val y = cursor.getString(colY)
                            val duration: String? = cursor.getString(colDuration)

                            pointMoves.add(PointMove(Point(x.toInt(), y.toInt()),
                                    if (duration != null) {
                                        if (duration.isNotEmpty()) {
                                            duration.toLong()
                                        } else {
                                            0
                                        }
                                    } else {
                                        0
                                    }))

                            cursor.moveToNext()
                        }

                        recorderCallback.onPlaybackStarted()

                        //used in the loop in the thread
                        var currentMoveIndex = 0

                        //the handler for the background thread
                        val handler = Handler(Handler.Callback {

                            val pointMove = pointMoves[currentMoveIndex]
                            if (isInPlayBackMode) {
                                //put in a try/catch in case this is called while the overlay is not on the screen
                                try {
                                    layoutParams!!.x = pointMove.point.x
                                    layoutParams!!.y = pointMove.point.y

                                    windowManager.updateViewLayout(view, layoutParams)

                                    onPointChangeListener?.onChange(Point(layoutParams!!.x, layoutParams!!.y))

                                } catch (e: Exception) {
                                    onPointChangeListener?.onError(e)
                                }
                            }

                            true
                        })

                        //loop through the data in a background thread
                        Thread(Runnable {
                            while (isInPlayBackMode) {
                                try {

                                    //looping to delay by the specified number of milliseconds
                                    val initialTime = System.currentTimeMillis();
                                    val expectedTime = initialTime + pointMoves[currentMoveIndex].elapsedTimeMillis

                                    while (System.currentTimeMillis() <= expectedTime) {
                                        //do nothing
                                    }

                                    if (currentMoveIndex == (pointMoves.size - 1)) {
                                        //when its on the last pointer move then restart the moves starting from the first move
                                        currentMoveIndex = 0
                                    } else {
                                        //go to next pointer move
                                        currentMoveIndex++
                                    }

                                    handler.sendEmptyMessage(0)
                                } catch (e: Exception) {
                                    onPointChangeListener?.onError(e)
                                }
                            }
                        }).start()
                    }
                }, tableName)
            }
        }
    }
}