package com.nosetrap.eventrecordlib

import android.content.ContentValues
import android.content.Context
import com.nosetrap.eventrecordlib.callback.RecorderCallback
import com.nosetrap.eventrecordlib.recorder.ActionRecorder
import com.nosetrap.storage.sql.CursorCallback
import com.nosetrap.storage.sql.DatabaseHandler
import com.nosetrap.storage.sql.EasyCursor

/**
 * keeps track of the number of initialised objects
 */
class RecorderManager private constructor(context: Context){

    /**
     * this class is a helper class which is used to help set a single recorderCalback for multiple
     * recorders
     */
     private class RecorderCallbackExtension private constructor(){
          var onRecordingStartedCount = 0
        var onPlaybackStartedCount  = 0
        var onPrePlaybackCount = 0
        var onRecordingStoppedCount = 0
        var onPlaybackStoppedCount = 0
        var onRecordingSavedCount = 0
        var onRecordingSaveProgressCount = 0


        companion object {
             private var uniqueInstance: RecorderCallbackExtension? = null

             fun getInstance(): RecorderCallbackExtension {
                 if(uniqueInstance == null){
                     uniqueInstance = RecorderCallbackExtension()
                 }
                 return  uniqueInstance!!
             }
         }
    }

    /**
     * set a single recorderCallback for multiple recorder
     * a callback method on @param recorderCallback is only executed after it is executed on
     * all the recorders
     */
    fun <T>setRecorderCallback(recorders: Array<ActionRecorder<T>>, recorderCallback: RecorderCallback){
        val callbackExtension = RecorderCallbackExtension.getInstance()
        for(recorder in recorders){
            recorder.setRecorderCallback(object : RecorderCallback {

                override fun onRecordingStarted() {
                   callbackExtension.onRecordingStartedCount++
                    if (callbackExtension.onRecordingStartedCount == recorders.size){
                        callbackExtension.onRecordingStartedCount = 0
                        recorderCallback.onRecordingStarted()
                    }
                }

                override fun onRecordingSaved() {
                    callbackExtension.onRecordingSavedCount++
                    if (callbackExtension.onRecordingSavedCount == recorders.size){
                        callbackExtension.onRecordingSavedCount = 0
                        recorderCallback.onRecordingSaved()
                    }
                }

                override fun onRecordingSaveProgress(progress: Double) {
                    callbackExtension.onRecordingSaveProgressCount++
                    if (callbackExtension.onRecordingSaveProgressCount == recorders.size) {
                        callbackExtension.onRecordingSaveProgressCount = 0
                        recorderCallback.onRecordingSaveProgress(progress)
                    }
                }

                override fun onRecordingStopped() {
                    callbackExtension.onRecordingStoppedCount++
                    if (callbackExtension.onRecordingStoppedCount == recorders.size){
                        callbackExtension.onRecordingStoppedCount = 0
                        recorderCallback.onRecordingStopped()
                    }                }

                override fun onPrePlayback() {
                    callbackExtension.onPrePlaybackCount++
                    if (callbackExtension.onPrePlaybackCount == recorders.size){
                        callbackExtension.onPrePlaybackCount = 0
                        recorderCallback.onPrePlayback()
                    }                }

                override fun onPlaybackStarted() {
                    callbackExtension.onPlaybackStartedCount++
                    if (callbackExtension.onPlaybackStartedCount == recorders.size){
                        callbackExtension.onPlaybackStartedCount = 0
                        recorderCallback.onPlaybackStarted()
                    }                }

                override fun onPlaybackStopped() {
                    callbackExtension.onPlaybackStoppedCount++
                    if (callbackExtension.onPlaybackStoppedCount == recorders.size){
                        callbackExtension.onPlaybackStoppedCount = 0
                        recorderCallback.onPlaybackStopped()
                    }
                }
            })
        }

    }

    /*
     * keeps count of how many ActionRecorders have been created,this is useful for deciding
     * the name of the sql table
     */
      var activeRecorderCount: Int = 0
    private set
    get() {
        return databaseHandler.getCount(tableNameActionRecorders).toInt()
    }



    /**
     * call to tell the recorderManager that a recorder has been created
     */
    internal fun actionRecorderCreated(recorderTableName: String){
        val values = ContentValues()
        values.put(colName,recorderTableName)
        databaseHandler.insert(tableNameActionRecorders,values)
    }

    /**
     * the table that stores the names of the overlay recorders
     */
    private val tableNameActionRecorders = "action_recorder_table_names"

    /**
     * stores the table names for the recorder
     */
    private val colName = "name"


    private val databaseHandler = DatabaseHandler(context)

    init {
        databaseHandler.createTable(tableNameActionRecorders, arrayOf(colName),null)
    }




    /**
     * contains the logic for releaseOverlay and release action
     * @param
     */
    private fun release(recorderTable:String){
        databaseHandler.query(object :CursorCallback{
            override fun onCursorQueried(cursor: EasyCursor) {
                if(cursor.getCount() > 0){
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst()
                        for (i in 0..(cursor.getCount() - 1)) {
                            //delete the table
                            val tableName = cursor.getString(colName)
                            databaseHandler.deleteTable(tableName)
                            //delete the entry in the recorderTable
                            databaseHandler.removeRows(recorderTable,"$colName = '$tableName'")
                            cursor.moveToNext()
                        }
                    }
                }
            }
        },recorderTable)
    }

    /**
     * gets rid of all resources that are being used by all the recorders
     * i.e deletes database tables
     */
    fun releaseAll(){
        release(tableNameActionRecorders)
    }


    companion object {
        private var uniqueInstance: RecorderManager? = null

        fun getInstance(context: Context): RecorderManager {
            if(uniqueInstance == null){
                uniqueInstance = RecorderManager(context)
            }
            return  uniqueInstance!!
        }
    }
}