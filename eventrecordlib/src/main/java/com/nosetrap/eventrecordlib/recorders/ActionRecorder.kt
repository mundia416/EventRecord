package com.nosetrap.eventrecordlib.recorders

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.provider.BaseColumns
import com.nosetrap.eventrecordlib.ObjectManager
import com.nosetrap.eventrecordlib.OnActionTriggerListener
import com.nosetrap.storage.sql.CursorCallback
import com.nosetrap.storage.sql.EasyCursor

class ActionRecorder(context: Context) : BaseRecorder(context) {

    private val tableName: String

    /**
     * collumn that holds a boolean of when an action is triggered
     */
    private val colTrigger = "trigger"
    private val colDuration = "duration"

    init {
        val objectManager = ObjectManager.getInstance(context)
        objectManager.numActionRecorders++
        tableName = "action_record_data_${objectManager.numOverlayRecorders}"
        databaseHandler.createTable(tableName, arrayOf(colTrigger,colDuration), null)
        setTableName(tableName)
    }

    /**
     * when this is called,an initial trigger is put in the database and the duration
     * is recorded between when the next trigger is recorder, the loop continues
     */
    override fun startRecording() {
        super.startRecording()

        val values = ContentValues()
        values.put(colTrigger,true)
        databaseHandler.insert(tableName,values)
    }

    /**
     * call when an action is performed in order to record it as a trigger
     */
    fun actionPerformed(): Boolean {
        return if (isInRecordMode) {
            val elapsedTime = calculateElapsedTime()

            val values = ContentValues()
            values.put(colDuration, elapsedTime)
            databaseHandler.update(tableName, values, "${BaseColumns._ID} = '${databaseHandler.getCount(tableName)}'")
            true
        } else {
            false
        }
    }

    /**
     *
     */
    fun startPlayback(onActionTriggerListener: OnActionTriggerListener){
        isInPlayBackMode = true
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


                //used in the loop in the thread
                var currentMoveIndex = 0

                //the handler for the background thread
                val handler = Handler(Handler.Callback {

                    val triggerDuration = triggers[currentMoveIndex]

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