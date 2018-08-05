package com.nosetrap.eventrecordlib.recorders

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.CallSuper
import com.nosetrap.storage.sql.DatabaseHandler

/**
 * parent class for recorder objects
 * @param tableName the name of the table which is storing recorded data
 * make sure to always release the recorder when done by calling release(), this
 * is to release any resources the recorder is using, failure to do this will lead
 * to unnecessary memory usage and,memory leaks and unintended consequences
 */
abstract class BaseRecorder(context: Context) {

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
    private var inRecordModeValue = false
    protected var isInRecordMode
        get() = inRecordModeValue
        set(value) {
            if(inRecordModeValue != value){
            liveOnRecordingStatusChanged.value = value
                inRecordModeValue = value
        }
        }


    /** determines whether the views movements are being played back or not*/
    private var inPlayBackModeValue = false
    protected var isInPlayBackMode
        get() = inPlayBackModeValue
        set(value) {
            if(inPlayBackModeValue != value) {
                liveOnPlaybackStatusChanged.value = value
                inPlayBackModeValue = value
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

    @CallSuper
    open fun startRecording(){
        stopPlayback()

        isInRecordMode = true

        resetMillis = System.currentTimeMillis()
    }

    /**
     * get rid of all the recording data that is stored in the database
     */
    open fun clearRecordingData() {
        databaseHandler.clearTable(tableName)
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
     */
    @CallSuper
    open fun stopRecording() {
        isInRecordMode = false
    }

    /**
     * come out of playback mode
     */
    open fun stopPlayback() {
        isInPlayBackMode = false
    }

    /**
     * repeases any resources that are being used by this recorder
     */
    fun release(){
        databaseHandler.deleteTable(tableName)
    }

}