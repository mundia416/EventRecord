package com.nosetrap.eventrecordlib.util

import android.content.Context
import android.os.Handler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.recorder.BaseActionRecorder
import com.nosetrap.storage.pojo.PojoExtension

/**
 * utility class to handle playback function
 * @classType T specify the type of pojo object that is being played back
 */
internal class PlaybackUtil<T>(private val context:Context,private val actionRecorder: BaseActionRecorder<T>) {
    /**
     * an array that holds all the data that was recorded into the database
     */
    private var entryArray = ArrayList<BaseActionRecorder.ActionPerformedEntry<T>>()


    /**
     * shows the current times it has played back
     */
    var currentPlaybackRun: Int = 0

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

    private var lastKnownException = Exception()

    /**
     * a actionHandler that tels the recorder callback that there is an error
     */
    private val recorderCallbackErrorHandler = Handler(Handler.Callback {
        actionRecorder.stopPlayback()
        actionRecorder.recorderCallback?.onError(lastKnownException)
        true
    })

    private val stopPlaybackHandler = Handler(Handler.Callback {
        actionRecorder.stopPlayback()
        true
    })

    /**
     * the actionHandler that tells the action trigger when to execute
     */
    private val actionHandler = Handler(Handler.Callback {
        try {
            if (actionRecorder.isInPlayBackMode) {
                val linkedTreeMap = entryArray[currentMoveIndex].data
                val data = Gson().toJsonTree(linkedTreeMap)
                actionTriggerListener!!.onTrigger(data.asJsonObject)
            }
        }catch (e: Exception){
            lastKnownException = e
            recorderCallbackErrorHandler.sendEmptyMessage(0)
        }

        true
    })




    /**
     * @param dataClassType the class of the pojo to recieve in the onTrigger method
     * @param recordingTableName is the table where the recording is stored
     *
     */
    fun startPlayback(recordingTableName: String,actionTriggerListener: ActionTriggerListener){
        this.actionTriggerListener = actionTriggerListener
        entryArray = ArrayList()
        currentMoveIndex = 0
        currentPlaybackRun = 0

        getThread(recordingTableName).start()
    }

    /**
     *initialise the thread that handles playback
     *querring the database and looping through the data is all done in a background thread
     * */
    private fun getThread(recordingTableName: String): Thread{
        return Thread(Runnable {
            try {
                val pojo = PojoExtension(context,recordingTableName)
                for (i in 0..(pojo.count - 1)) {
                    val key = actionRecorder.keyPojoData + i
                    val typeToken = object: TypeToken<BaseActionRecorder.ActionPerformedEntry<T>>() {}.type
                    val dataEntry = pojo.get<BaseActionRecorder.ActionPerformedEntry<T>>(key, typeToken)
                    entryArray.add(dataEntry)
                }
                pojo.closeConnection()
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
                            currentPlaybackRun++
                        } else {
                            //go to next pointer move
                            currentMoveIndex++
                        }

                        actionHandler.sendEmptyMessage(0)

                        if(actionRecorder.playbackLimit != BaseActionRecorder.PLAYBACK_UNLIMITED &&
                                currentPlaybackRun >= actionRecorder.playbackLimit){
                            stopPlaybackHandler.sendEmptyMessage(0)
                        }
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                lastKnownException = e
                recorderCallbackErrorHandler.sendEmptyMessage(0)
            }
        })
    }

    /**
     * an internal interface that calls when the recorder is ready to playback
     */
    internal interface PlaybackReadyListener{
        /**
         * is called after a recording has been saved, this is when the recorder is ready to playback
         */
        fun onReady(recordingId: Int)

        /**
         * is called after a recording has been saved, this is when the recorder is ready to playback
         */
        fun onReady(recordingTableName: String)
    }
}