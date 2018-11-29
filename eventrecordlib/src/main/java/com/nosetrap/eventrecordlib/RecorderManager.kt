package com.nosetrap.eventrecordlib

import android.content.ContentValues
import android.content.Context
import com.nosetrap.eventrecordlib.callback.RecorderCallback
import com.nosetrap.eventrecordlib.data.SavedRecording
import com.nosetrap.eventrecordlib.recorder.ActionRecorder
import com.nosetrap.eventrecordlib.util.IDUtil
import com.nosetrap.storage.pojo.Pojo
import com.nosetrap.storage.sql.*

/**
 * keeps track of the number of initialised objects
 */
class RecorderManager private constructor(private val context: Context){

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

    /**
     *returns the total number of recording tables saved, this is useful when deciding the name of the
     * next recording
     */
      var totalRecordingsCount: Int = 0
    private set
    get() {
        return databaseHandler.getCount(tableNameRecorderRecordings).toInt()
    }

    /**
     * @param recordingTableName is the name of the table where the recording has been saved
     */
    internal fun recordingCreated(recorderId: Int,recording: SavedRecording){
        val values = ContentValues()
        values.put(colRecorderID,recorderId)
        values.put(colTimeStamp, recording.timestamp)
        values.put(colTitle, recording.title)
        values.put(colRecordingTableName,IDUtil.toRecordingTableName(recording.id))
        databaseHandler.insert(tableNameRecorderRecordings,values)
    }

    /**
     * this keeps track of the recordings that have been stored for a specific recorder identified in
     * the [tableNameActionRecorders] table
     */
    private val tableNameRecorderRecordings = "recorder_recordings"

    /**
     * stores the id of the recorder
     */
    private val colRecorderID = "recorder_ID"
    /**
     * stores the name of the table where a recording for an action recorder is stored
     */
    private val colRecordingTableName ="recording_table_name"

    private val colTimeStamp = "timestamp"
    private val colTitle = "title"
    private val databaseHandler = DatabaseHandler(context)
    private val databaseHandlerExtension = DatabaseHandlerExtension(context)


    init {
        databaseHandler.createTable(tableNameRecorderRecordings, arrayOf(colRecordingTableName,
                colTitle,colTimeStamp), arrayOf(colRecorderID))
    }

    /**
     * get recording for a specific recorder id
     * @param recorderId is the id for which to get recordings for
     */
     fun getRecordings(recorderId: Int): ArrayList<SavedRecording>{
        val recordings = ArrayList<SavedRecording>()

        databaseHandler.query(object : CursorCallback{
            override fun onCursorQueried(cursor: EasyCursor) {
                cursor.iterate(object : IterateListener{
                    override fun onNext(cursor: EasyCursor) {
                        val recordingName = cursor.getString(colRecordingTableName)
                        val recordingId = IDUtil.getRecordingId(recordingName)
                        val title = cursor.getString(colTitle)
                        val timestamp = cursor.getString(colTimeStamp)
                        val savedRecording = SavedRecording(recordingId,title,timestamp.toLong())

                        recordings.add(savedRecording)
                    }
                })
            }
        },tableNameRecorderRecordings,null, "$colRecorderID == $recorderId")

        return recordings
    }

    /**
     * gets rid of all the recordings for all the recorders
     */
     fun clearRecordingData() {
        databaseHandlerExtension.query(object : CursorCallback{
            override fun onCursorQueried(cursor: EasyCursor) {
                cursor.iterate(object : IterateListener{
                    override fun onNext(cursor: EasyCursor) {
                        val recordingTableName = cursor.getString(colRecordingTableName)
                        databaseHandlerExtension.deleteTable(recordingTableName)
                    }
                })
            }
        },tableNameRecorderRecordings,arrayOf(colRecordingTableName))
        databaseHandlerExtension.closeConnection()
    }

        /**
     * gets rid of all the recordings of a recorder
     */
    fun deleteRecordings(recorderId: Int){
        databaseHandlerExtension.query(object : CursorCallback{
            override fun onCursorQueried(cursor: EasyCursor) {
                cursor.iterate(object : IterateListener{
                    override fun onNext(cursor: EasyCursor) {
                        val recordingTableName = cursor.getString(colRecordingTableName)
                        databaseHandlerExtension.deleteTable(recordingTableName)

                    }
                })
            }
        },tableNameRecorderRecordings,
                arrayOf(colRecordingTableName),"$colRecorderID == $recorderId")
        databaseHandlerExtension.closeConnection()
    }

    /**
     *
     */
    fun deleteRecording(recordingId: Int){
        val recordingTableName = IDUtil.toRecordingTableName(recordingId)
        databaseHandlerExtension.removeRows(tableNameRecorderRecordings,
                "$colRecordingTableName == '$recordingTableName'")

        val pojo = Pojo(context,recordingTableName)
        pojo.releaseAll()
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