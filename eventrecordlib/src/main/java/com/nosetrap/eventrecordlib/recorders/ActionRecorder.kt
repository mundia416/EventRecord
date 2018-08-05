package com.nosetrap.eventrecordlib.recorders

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.provider.BaseColumns
import android.util.Log
import com.nosetrap.eventrecordlib.RecorderManager
import com.nosetrap.eventrecordlib.OnActionTriggerListener
import com.nosetrap.eventrecordlib.RecorderCallback
import com.nosetrap.storage.sql.CursorCallback
import com.nosetrap.storage.sql.EasyCursor
import com.nosetrap.storage.sql.OrderBy

class ActionRecorder(context: Context, private val recorderCallback: RecorderCallback) : BaseRecorder(context) {

    private val tableName: String

    /**
     * column that holds a boolean of when an action is triggered
     */
    private val colTrigger = "trigger"
    private val colDuration = "duration"

    init {
        val recorderManager = RecorderManager.getInstance(context)
        tableName = "action_record_data_${recorderManager.getActiveActionRecorderCount()}"
        recorderManager.actionRecorderCreated(tableName)
        databaseHandler.createTable(tableName, arrayOf(colTrigger,colDuration), null)
        setTableName(tableName)
    }

    /**
     * when this is called,an initial trigger is put in the database and the duration
     * is recorded between when the next trigger is recorder, the loop continues
     */
    override fun startRecording() {
        super.startRecording()

        recorderCallback.onRecordingStarted()
       // triggerValues.add(true)
    }

    override fun stopRecording() {
        super.stopRecording()
        recorderCallback.onRecordingStopped()
        //updateElapsedTime()
        storeRecordingInDatabase()
    }

    //the values for the table columns
    private val triggerValues = ArrayList<Boolean>()
    private val elapsedTimeValues = ArrayList<Long>()

    /**
     * data is stored in arraylists and then when playback stops it is put in the sqlDatabase
     */
    private fun storeRecordingInDatabase(){
        for(i in 0..(triggerValues.size-1)){
            val values = ContentValues()
            values.put(colTrigger,triggerValues[i])
            values.put(colDuration,elapsedTimeValues[i])
            databaseHandler.insert(tableName,values)

            //setting the save progress
            val c: Double = (i.toDouble()/triggerValues.size.toDouble())
            val percentageProgress: Double = c * 100
            recorderCallback.onRecordingSaveProgress(percentageProgress)
        }
        recorderCallback.onRecordingSaved()
    }

    /**
     * call when an action is performed in order to record it as a trigger
     */
    fun actionPerformed(): Boolean {
        return if (isInRecordMode) {
           updateElapsedTime()
            triggerValues.add(true)

            true
        } else {
            false
        }
    }


    /**
     * update the elapsed time column for the last entry in the table
     */
    private fun updateElapsedTime(){
        val elapsedTime = calculateElapsedTime()
        elapsedTimeValues.add(elapsedTime)
        resetElapsedTime()
    }

    override fun stopPlayback() {
        super.stopPlayback()
        recorderCallback.onPlaybackStopped()
    }

    /**
     *
     */
    fun startPlayback(onActionTriggerListener: OnActionTriggerListener){
        isInPlayBackMode = true

        recorderCallback.onPrePlayback()

        // ensures that recording is turned off
        stopRecording()

        val triggers = ArrayList<Long>()

        databaseHandler.getAll(object : CursorCallback {
            override fun onCursorQueried(cursor: EasyCursor) {
                cursor.moveToFirst()

                for (i in cursor.getCount() downTo 1) {
                   // val trigger = cursor.getString(colTrigger)
                    val duration = cursor.getString(colDuration)

                    triggers.add(duration.toLong())

                    cursor.moveToNext()
                }

                recorderCallback.onPlaybackStarted()

                //used in the loop in the thread
                var currentMoveIndex = 0

                //the handler for the background thread
                val handler = Handler(Handler.Callback {

                    if (isInPlayBackMode) {
                        onActionTriggerListener.onTrigger()
                    }

                    true
                })

                //loop through the data in a background thread
                Thread(Runnable {
                    while (isInPlayBackMode) {
                        try {

                            //looping to delay by the specified number of milliseconds
                            val initialTime = System.currentTimeMillis();
                            val expectedTime = initialTime + triggers[currentMoveIndex]

                            while (System.currentTimeMillis() <= expectedTime) {
                                //do nothing
                            }

                            if (currentMoveIndex == (triggers.size - 1)) {
                                //when its on the last pointer move then restart the moves starting from the first move
                                currentMoveIndex = 0
                            } else {
                                //go to next pointer move
                                currentMoveIndex++
                            }

                            handler.sendEmptyMessage(0)
                        } catch (e: Exception) {
                            onActionTriggerListener?.onError(e)
                        }
                    }
                }).start()
            }
        }, tableName)

    }
}