package com.nosetrap.eventrecordlib.recorders

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
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
class OverlayMovementRecorder(private val context: Context)
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

    /**
     * start Recording the movements
     * playback is stopped if it is running when this method is called
     * @param reportStopRecording should the callback method for onRecordingStopped be called
     * @param reportPlayback should the callback method for playback be called
     */
    override fun startRecording(reportPlayback: Boolean,reportStopRecording: Boolean) {
        if (this.layoutParams == null) {
            throw IllegalStateException("attempted to play without a view(WindowManager.LayoutParams) attached to recorder")
        } else {
            xValues = ArrayList()
            yValues = ArrayList()
            elapsedTimeValues = ArrayList()
            super.startRecording(reportPlayback,reportStopRecording)

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

    /**
     * @param reportStopRecording should the callback method for onRecordingStopped be called
     */
    private fun saveRecordingToDatabase(reportStopRecording: Boolean){
        //the key used for the message sent to the recordingProgressHandler
        val keyProgress = "progress_key"

        val recordingSavedHandler = Handler(Handler.Callback {
            if(reportStopRecording) {
                recorderCallback?.onRecordingSaved()
            }
                true
        })

        val recordingProgressHandler = Handler(Handler.Callback {msg ->
            if(reportStopRecording) {
                val percentageProgress = msg.data.getDouble(keyProgress, 0.0)
                recorderCallback?.onRecordingSaveProgress(percentageProgress)
            }
            true
        })

        Thread(Runnable {
        for(i in 0..(xValues.size - 1)){
            val values = ContentValues()
            values.put(colX,xValues[i])
            values.put(colY,yValues[i])
            values.put(colDuration,elapsedTimeValues[i])

            databaseHandler.insert(tableName,values)

            //setting the save progress
            val c: Double = (i.toDouble()/xValues.size.toDouble())
            val percentageProgress: Double = c * 100

            val msg = Message()
            val data = Bundle()
            data.putDouble(keyProgress,percentageProgress)
            msg.data = data
            recordingProgressHandler.sendMessage(msg)

        }
            recordingSavedHandler.sendEmptyMessage(0)
        }).start()
    }

    /**
     * stop recording movements of the view
     * @param reportStopRecording should the callback method for onRecordingStopped be called

     */
    override fun stopRecording(reportStopRecording: Boolean) {
        super.stopRecording(reportStopRecording)
        saveElapsedTime()
        saveRecordingToDatabase(reportStopRecording)
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
     * @param reportStopRecording should the callback method for onRecordingStopped be called
     * @param reportPlayBack should the callback method for playback be called
     */
     fun startPlayback(view: View,reportStopRecording: Boolean = false,reportPlayBack: Boolean = true,
                       onPointChangeListener: OnPointChangeListener? = null) {
        if (!isInPlayBackMode) {
            if (this.layoutParams == null) {
                throw IllegalStateException("attempted to playBack without a view(WindowManager.LayoutParams) " +
                        "attached to recorder")
            } else {
                isInPlayBackMode = true

                if(reportPlayBack) {
                    recorderCallback?.onPrePlayback()
                }

                // ensures that recording is turned off
                if(isInRecordMode) {
                    stopRecording(reportStopRecording)
                }

                val pointMoves = ArrayList<PointMove>()

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


                val playbackStartedHandler = Handler(Handler.Callback {
                    recorderCallback?.onPlaybackStarted()
                    true
                })


                //querring the database and looping through the data is don in a background thread
                Thread(Runnable {
                databaseHandler.getAll(object : CursorCallback {
                    override fun onCursorQueried(cursor: EasyCursor) {
                        try {
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

                            if (reportPlayBack) {
                                playbackStartedHandler.sendEmptyMessage(0)
                            }

                            //loop through the data in a background thread
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
                        }catch (e:Exception){
                            recorderCallbackErrorHandler.sendEmptyMessage(0)
                        }
                    }
                    }, tableName)
                }).start()
            }
        }
    }
}