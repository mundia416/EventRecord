package com.nosetrap.eventrecordlib.recorders

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.RecorderManager
import com.nosetrap.eventrecordlib.RecorderCallback
import com.nosetrap.eventrecordlib.util.PlaybackUtil
import com.nosetrap.storage.pojo.PojoExtension
import com.nosetrap.storage.sql.DatabaseHandler

/**
 * @classType T specifies the pojo object class type which will be used to store data
 */
class ActionRecorder<T>(context: Context) {

    private val tableName: String

    /**
     * the key for the different pojo object entries which will be inserted into the database
     */
    internal val keyPojoData = "pojo_key_entry_"

    internal val pojo: PojoExtension
    private val databaseHandler = DatabaseHandler(context)

    internal var recorderCallback: RecorderCallback? = null

    /**
     *
     * */
    var isInRecordMode = false
        internal set

    /**
     *  determines whether the views movements are being played back or not
     *  */
    var isInPlayBackMode = false
        internal set


    /**
     * the milliseconds that the system is currently at when the recording is started or when there
     *  is an update(when the location of the view changes)
     *  */
    private var resetMillis: Long = 0


    init {
        val recorderManager = RecorderManager.getInstance(context)
        tableName = "action_record_data_${recorderManager.activeRecorderCount}"
        recorderManager.actionRecorderCreated(tableName)
        databaseHandler.createTable(tableName, arrayOf("b"),null)
        pojo = PojoExtension(context, tableName)
    }

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
        recorderCallback?.onRecordingStarted()
    }

    /**
     * reset the elapsed time millis variable
     */
    private fun resetElapsedTime() {
        resetMillis = System.currentTimeMillis()
    }

    /**
     * calculate the time that has elapsed since the last data was inserted in the database
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
        recorderCallback?.onRecordingStopped()
        saveToDatabase()
    }

    /**
     * get rid of all the recording data that is stored in the database
     */
    open fun clearRecordingData() {
        databaseHandler.clearTable(tableName)
        pojo.releaseAll()
        pojo.closeConnection()
    }

    /**
     * releases any resources that are being used by this recorder
     */
    fun release() {
        databaseHandler.deleteTable(tableName)
        pojo.releaseAll()
        pojo.closeConnection()
    }

    /**
     * come out of playback mode
     */
    fun stopPlayback() {
        isInPlayBackMode = false
        recorderCallback?.onPlaybackStopped()
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
        recorderCallback?.onRecordingSaved()
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
        recorderCallback?.onRecordingSaveProgress(progress)
        true
    })


    /**
     * insert the recorded data into the database
     */
    private fun saveToDatabase(){
        Thread(Runnable {
            for(entry in entries){
                val entryKey = keyPojoData + pojo.count
                pojo.insert(entryKey, entry)

                val msg = Message()
                val data = Bundle()
                data.putInt(keySaveProgress,entries.indexOf(entry))
                msg.data = data
                handlerSaveProgress.sendMessage(msg)
            }
            pojo.closeConnection()
            handlerSaveComplete.sendEmptyMessage(0)
        }).start()

    }

    /**
     * a data class for storing an action performed in the database, it holds the elapsed time
     */
    internal data class ActionPerformedEntry<E>(var data: E, var duration: Long)


    private val playbackUtil = PlaybackUtil<T>(this)


    /**
     * @param dataClassType the class of the pojo to recieve in the onTrigger method
     *
     */
    fun startPlayback(actionTriggerListener: ActionTriggerListener) {
        isInPlayBackMode = true

        // ensures that recording is turned off
        if(isInRecordMode) {
            stopRecording()
        }
        recorderCallback?.onPrePlayback()

        playbackUtil.startPlayback(actionTriggerListener)
    }
}