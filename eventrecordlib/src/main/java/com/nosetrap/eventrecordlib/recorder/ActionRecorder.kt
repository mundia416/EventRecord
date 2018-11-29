package com.nosetrap.eventrecordlib.recorder

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.callback.RecorderCallback
import com.nosetrap.eventrecordlib.data.SavedRecording
import com.nosetrap.eventrecordlib.util.IDUtil
import com.nosetrap.storage.pojo.PojoExtension

/**
 * action recorder that records the actions with the time between actions being calculated in realtime (elapsed real time)
 */
class ActionRecorder<T>(private val context: Context,private val id: Int) : BaseActionRecorder<T>(context,id) {

    /**
     *
     * */
    var isInRecordMode = false
        internal set


    /**
     * the milliseconds that the system is currently at when the recording is started or when there
     *  is an update(when the location of the view changes)
     *  */
    private var resetMillis: Long = 0


    /**
     * when this is called,an initial trigger is put in the database and the duration
     * is recorded between when the next trigger is recorder, the loop continues
     */
    fun startRecording() {
        if (isInPlayBackMode) {
            stopPlayback()
        }
        entries.clear()
        isInRecordMode = true
        resetMillis = System.currentTimeMillis()
        (recorderCallback as RecorderCallback).onRecordingStarted()
    }

    /**
     * reset the elapsed time millis variable
     */
    private fun resetElapsedTime() {
        resetMillis = System.currentTimeMillis()
    }

    /**
     * calculate the time that hasb elapsed since the last data was inserted in the database
     */
    private fun calculateElapsedTime(): Long {
        return System.currentTimeMillis() - resetMillis
    }

    /**
     *  set a recorder callback
     */
    fun setRecorderCallback(recorderCallback: RecorderCallback) {
        this.recorderCallback = recorderCallback
    }

    /**
     *
     * */
    fun stopRecording() {
        isInRecordMode = false
        (recorderCallback as RecorderCallback).onRecordingStopped()
    }



    /**
     * contains the entries of the data to be saved in the database
     */
    private val entries = ArrayList<ActionPerformedEntry<T>>()

    /**
     * call when an action is performed in order to record it as a trigger
     * @param data a pojo object which contains data to be recorded
     * @return true if the recorder is currently in record mode
     */
    fun actionPerformed(data: T): Boolean {
        return if (isInRecordMode) {
            val elapsedTime = calculateElapsedTime()
            resetElapsedTime()

            val actionPerformedEntry = ActionPerformedEntry(data, elapsedTime)
            entries.add(actionPerformedEntry)
            true
        } else {
            false
        }
    }

    /**
     * handler for the background thread which saves to database
     */
    private val handlerSaveComplete = Handler(Handler.Callback {
        (recorderCallback as RecorderCallback).onRecordingSaved()
        true
    })

    /**
     * the key used when sending a message from the thread that saves the recorded data into the database
     * to the handler that shows the progress
     */
    private val keySaveProgress = "keySaveProgress"

    /**
     * handler to show the progress for the background thread which saves to database
     */
    private val handlerSaveProgress = Handler(Handler.Callback {msg ->
        val index = msg.data.getInt(keySaveProgress)

        val progress = ((index.toDouble()+1.0)/entries.size.toDouble()) * 100.0
        (recorderCallback as RecorderCallback).onRecordingSaveProgress(progress)
        true
    })



    /**
     * insert the recorded data into the database
     */
    fun saveToDatabase(title: String? = null){
        Thread(Runnable {
            try {
                val recordingID = recorderManager.totalRecordingsCount
                val pojo = PojoExtension(context,IDUtil.toRecordingTableName(recordingID))

                for (entry in entries) {
                    val entryKey = keyPojoData + pojo.count
                    pojo.insert(entryKey, entry)

                    val msg = Message()
                    val data = Bundle()
                    data.putInt(keySaveProgress, entries.indexOf(entry))
                    msg.data = data
                    handlerSaveProgress.sendMessage(msg)
                }

                pojo.closeConnection()
                val savedRecording = SavedRecording(recordingID,title,System.currentTimeMillis())
                recorderManager.recordingCreated(id,savedRecording)
                handlerSaveComplete.sendEmptyMessage(0)
            }catch (e: Exception){
                lastKnownException = e
                errorHandler.sendEmptyMessage(0)
            }
        }).start()

    }

    /**
     * @param dataClassType the class of the pojo to recieve in the onTrigger method
     * @param recordingId is the id of the recording to playback
     *
     */
    fun togglePlayback(recordingId: Int,actionTriggerListener: ActionTriggerListener) {
        this.actionTriggerListener = actionTriggerListener
        isInPlayBackMode = true

        recorderCallback?.onPrePlayback()
        // ensures that recording is turned off
        if(isInRecordMode) {
            stopRecording()
        }else{
            playbackReadyListener.onReady(recordingId)
        }
    }
}