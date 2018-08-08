package com.nosetrap.eventrecordlib.recorders

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.CallSuper
import com.nosetrap.eventrecordlib.RecorderCallback
import com.nosetrap.storage.sql.DatabaseHandler

/**
 * parent class for recorder objects
 * @param tableName the name of the table which is storing recorded data
 * make sure to always release the recorder when done by calling release(), this
 * is to release any resources the recorder is using, failure to do this will lead
 * to unnecessary memory usage and,memory leaks and unintended consequences
 */
abstract class BaseRecorder(context: Context) {

    /**
     * the recorder callback
     * only some of the methods are executed from this base class, the rest of the methods
     * would have to be executed by the child classes of this class
     * the method which are handled from this class are
     * onRecordingStarted()
     * onRecordingStopped()
     * onPlaybackStopped()
     * onRecordingDataCleared()
     */
    protected var recorderCallback: RecorderCallback? = null


    private var tableName: String = ""
    /**
     * broadcasts every time the value of isInRecordMode changes
     */
    val liveOnRecordingStatusChanged = MutableLiveData<Boolean>()

    /**
     * broadcasts every time the value of isInPlayback changes
     */
    val liveOnPlaybackStatusChanged = MutableLiveData<Boolean>()

    /**
     * set the recorder callback
     */
    fun addRecorderCallback(recorderCallback: RecorderCallback){
        this.recorderCallback = recorderCallback
    }

    /**
     * set the name of the table which will be used to store data for the recorder
     */
    protected fun setTableName(tableName: String){
        this.tableName = tableName
    }

    /**
     * the milliseconds that the system is currently at when the recording is started or when there
     *  is an update(when the location of the view changes)
     *  */
    protected var resetMillis: Long = 0


    /** determines whether the views movements are being recorded or not*/
    protected var isInRecordMode= false
        set(value) {
            if(field != value){
            liveOnRecordingStatusChanged.value = value
                field = value
        }
        }


    /** determines whether the views movements are being played back or not*/
    protected var isInPlayBackMode = false
        set(value) {
            if(field != value) {
                liveOnPlaybackStatusChanged.value = value
                field = value
            }
        }

    protected val databaseHandler = DatabaseHandler(context)

    /**
     * check whether the recorder is currently recording or not
     */
    fun isRecording(): Boolean {
        return isInRecordMode
    }

    /**
     * check whether the recorder is currently in playback or not
     */
    fun isInPlayback(): Boolean {
        return isInPlayBackMode
    }

    /**
     * @param reportRecording should the callback method for onRecordingStopped be called
     */
    @CallSuper
    open fun startRecording(reportRecording: Boolean = true){
        stopPlayback()
        isInRecordMode = true
        resetMillis = System.currentTimeMillis()

        if(reportRecording){
            recorderCallback?.onRecordingStarted()
        }
    }

    /**
     * get rid of all the recording data that is stored in the database
     */
    open fun clearRecordingData() {
        databaseHandler.clearTable(tableName)
        recorderCallback?.onRecordingDataCleared()
    }

    /**
     * calculate the time that has elapsed since the last data was inserted in the database
     */
    protected fun calculateElapsedTime(): Long {
        return System.currentTimeMillis() - resetMillis
    }

    /**
     * reset the elapsed time millis variable
     */
    protected fun resetElapsedTime(){
        resetMillis = System.currentTimeMillis()
    }

    /**
     * stop recording movements of the view
     * @param reportStopRecording should the callback method for onRecordingStopped be called
     */
    @CallSuper
    open fun stopRecording(reportStopRecording: Boolean = true) {
        isInRecordMode = false
        if(reportStopRecording) {
            recorderCallback?.onRecordingStopped()
        }
    }

    /**
     * come out of playback mode
     * @param reportStopPlayback should the callback method for onRecordingStopped be called
     */
    open fun stopPlayback(reportStopPlayback: Boolean = true) {
        isInPlayBackMode = false
        if(reportStopPlayback) {
            recorderCallback?.onPlaybackStopped()
        }
    }

    /**
     * repeases any resources that are being used by this recorder
     */
    fun release(){
        databaseHandler.deleteTable(tableName)
    }



}