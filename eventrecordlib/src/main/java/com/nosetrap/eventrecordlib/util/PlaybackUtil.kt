package com.nosetrap.eventrecordlib.util

import android.os.Handler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.recorders.ActionRecorder

/**
 * utility class to handle playback function
 * @classType T specify the type of pojo object that is being played back
 */
internal class PlaybackUtil<T>(private val actionRecorder: ActionRecorder<T>) {
    /**
     * an array that holds all the data that was recorded into the database
     */
    private var entryArray = ArrayList<ActionRecorder.ActionPerformedEntry<T>>()


    //used in the loop in the thread
    private var currentMoveIndex = 0

    private var actionTriggerListener: ActionTriggerListener? = null

    /**
     * a actionHandler that tels the recorder callback that recording has started
     */
    private val playbackStartedHandler = Handler(Handler.Callback {
        actionRecorder.recorderCallback?.onPlaybackStarted()
        true
    })

    /**
     * a actionHandler that tels the recorder callback that there is an error
     */
    private val recorderCallbackErrorHandler = Handler(Handler.Callback {
        actionRecorder.stopPlayback()
        actionRecorder.recorderCallback?.onError()
        true
    })


    /**
     * the actionHandler that tells the action trigger when to execute
     */
    private val actionHandler = Handler(Handler.Callback {
        if (actionRecorder.isInPlayBackMode) {
            val linkedTreeMap = entryArray[currentMoveIndex].data
            val data = Gson().toJsonTree(linkedTreeMap)
            actionTriggerListener!!.onTrigger(data.asJsonObject)
        }

        true
    })




    /**
     * @param dataClassType the class of the pojo to recieve in the onTrigger method
     *
     */
    fun startPlayback(actionTriggerListener: ActionTriggerListener){
        this.actionTriggerListener = actionTriggerListener
        entryArray = ArrayList()
        currentMoveIndex = 0

        getThread().start()
    }

    /**
     *initialise the thread that handles playback
     *querring the database and looping through the data is all done in a background thread
     * */
    private fun getThread(): Thread{
        return Thread(Runnable {
            try {

                for (i in 0..(actionRecorder.pojo.count - 1)) {
                    val key = actionRecorder.keyPojoData + i
                    val typeToken = object: TypeToken<ActionRecorder.ActionPerformedEntry<T>>() {}.type
                    val dataEntry = actionRecorder.pojo.get<ActionRecorder.ActionPerformedEntry<T>>(key, typeToken)
                    entryArray.add(dataEntry)
                }
                actionRecorder.pojo.closeConnection()
                playbackStartedHandler.sendEmptyMessage(0)

                //loop through the data in a background thread
                while (actionRecorder.isInPlayBackMode) {
                    try {
                        //looping to delay by the specified number of milliseconds
                        val initialTime = System.currentTimeMillis()
                        val expectedTime = initialTime + entryArray[currentMoveIndex].duration

                        while (System.currentTimeMillis() <= expectedTime) {
                            //do nothing
                        }

                        if (currentMoveIndex == (entryArray.size - 1)) {
                            //when its on the last pointer move then restart the moves starting from the first move
                            currentMoveIndex = 0
                        } else {
                            //go to next pointer move
                            currentMoveIndex++
                        }

                        actionHandler.sendEmptyMessage(0)
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recorderCallbackErrorHandler.sendEmptyMessage(0)
            }
        })
    }
}