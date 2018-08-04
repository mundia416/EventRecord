package com.nosetrap.eventrecordlib.recorders

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.WindowManager
import com.nosetrap.eventrecordlib.ObjectManager
import com.nosetrap.eventrecordlib.OnPointChangeListener
import com.nosetrap.eventrecordlib.Point
import com.nosetrap.eventrecordlib.PointMove
import com.nosetrap.storage.sql.CursorCallback
import com.nosetrap.storage.sql.EasyCursor

/**
 * records the movement of a view which is an overlay
 *
 * it records the movement by storing the elapsedTimeMillis that the view spend at a specific XY coordinate
 *
 * @WARNING each view can only be assigned 1 recorder
 *
 */
class OverlayMovementRecorder(private val context: Context) : BaseRecorder(context) {
    private val tableName: String


    private val colX = "x"
    private val colY = "y"
    private val colDuration = "elapsedTimeMillis"

    private var lastStoredX = 0
    private var lastStoredY = 0

    private var layoutParams: WindowManager.LayoutParams? = null

    init {
        val objectManager = ObjectManager.getInstance(context)
        objectManager.numOverlayRecorders++
        tableName = "overlay_view_record_data_${objectManager.numOverlayRecorders}"
        databaseHandler.createTable(tableName, arrayOf(colX, colY, colDuration), null)
        setTableName(tableName)
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * assign this recorder to a view. {its actually assinged to a views layout params
     */
    private fun assignView(layoutParams: WindowManager.LayoutParams) {
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
     */
    override fun startRecording() {
        if (this.layoutParams == null) {
            throw IllegalStateException("attempted to play without a view(WindowManager.LayoutParams) attached to recorder")
        } else {
            super.startRecording()

            saveCurrentXY()
        }

    }

    /**
     * call when the views location changes in order to update the recording data
     * @return true if its in recording mode and false if not
     */
    fun viewLocationChanged(): Boolean {
        return if (isInRecordMode) {
            val elapsedTime = calculateElapsedTime()

            val values = ContentValues()
            values.put(colDuration, elapsedTime)

            databaseHandler.update(tableName, values, "$colX = '$lastStoredX' AND $colY = '$lastStoredY")

            resetMillis = System.currentTimeMillis()

            //create a new row
            saveCurrentXY()

            true
        } else {
            false
        }
    }

    /*
    *
     *
    fun invalidateView(layoutParams: WindowManager.LayoutParams){

    }*/

    /**
     * stop recording movements of the view
     */
    override fun stopRecording() {
        super.stopRecording()
        viewLocationChanged()
    }



    /**
     * save the current x/y coordinates into the database
     */
    private fun saveCurrentXY() {
        //the starting positions for the pointer when the recording is started
        lastStoredX = layoutParams!!.x
        lastStoredY = layoutParams!!.y

        val values = ContentValues()
        values.put(colX, lastStoredX)
        values.put(colY, lastStoredY)
        databaseHandler.insert(tableName, values)
    }


    /**
     * playback the recorded movements onto the view
     * recording is stopped if this method is called while recording
     * @param view the view to which the layout params are attached to
     */
     fun startPlayback(view: View, onPointChangeListener: OnPointChangeListener? = null) {
        if (this.layoutParams == null) {
            throw IllegalStateException("attempted to playBack without a view(WindowManager.LayoutParams) attached to recorder")
        } else {
            isInPlayBackMode = true
            // ensures that recording is turned off
            stopRecording()

            val pointMoves = ArrayList<PointMove>()

            databaseHandler.getAll(object : CursorCallback {
                override fun onCursorQueried(cursor: EasyCursor) {
                    cursor.moveToFirst()

                    for (i in cursor.getCount() downTo 1) {
                        val x = cursor.getString(colX)
                        val y = cursor.getString(colY)
                        val duration = cursor.getString(colDuration)

                        pointMoves.add(PointMove(Point(x.toInt(), y.toInt()),
                                if (duration.isNotEmpty() && duration != null) {
                                    duration.toLong()
                                } else {
                                    0
                                }))

                        cursor.moveToNext()
                    }


                    //used in the loop in the thread
                    var currentMoveIndex = 0

                    //the handler for the background thread
                    val handler = Handler(Handler.Callback {

                        val pointMove = pointMoves[currentMoveIndex]

                        layoutParams!!.x = pointMove.point.x
                        layoutParams!!.y = pointMove.point.x

                        if (isInPlayBackMode) {
                            //put in a try/catch in case this is called while the overlay is not on the screen
                            try {
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