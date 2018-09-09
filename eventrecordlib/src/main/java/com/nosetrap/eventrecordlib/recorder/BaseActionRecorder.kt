package com.nosetrap.eventrecordlib.recorder

import android.content.Context
import android.os.Handler
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.RecorderManager
import com.nosetrap.eventrecordlib.callback.BaseRecorderCallback
import com.nosetrap.eventrecordlib.callback.RecorderCallback
import com.nosetrap.eventrecordlib.util.PlaybackUtil
import com.nosetrap.storage.pojo.PojoExtension
import com.nosetrap.storage.sql.DatabaseHandler

/**
 * base class for action recorders
 *  * @classType T specifies the pojo object class type which will be used to store data
 */
abstract class BaseActionRecorder<T>(context: Context) {
    companion object {
        /**
         * playback unlimited times
         */
        const val PLAYBACK_UNLIMITED = -1
    }

    protected val tableName: String

    /**
     * shows the current times it has played back
     */
    private val currentPlaybackRun: Int
        get() = playbackUtil.currentPlaybackRun

    /**
     * set a limit on the number of times the recorder should playback,
     *  -1 means it will playback unlimited
     */
    var playbackLimit = PLAYBACK_UNLIMITED

    /**
     * the action trigger listener which triggers when in playback
     */
    protected var  actionTriggerListener: ActionTriggerListener? = null

    private val playbackUtil = PlaybackUtil<T>(this)

    /**
     * the recorder callback to receive callback methods
     */
    internal var recorderCallback: BaseRecorderCallback? = object :BaseRecorderCallback{}


    /**
     * the key for the different pojo object entries which will be inserted into the database
     */
    internal val keyPojoData = "pojo_key_entry_"

    internal val pojo: PojoExtension
    //protected val databaseHandler = DatabaseHandler(context)


    /**
     *  determines whether the actions are being played back or not
     *  */
    var isInPlayBackMode = false
        internal set

    protected var lastKnownException = Exception()

    protected val errorHandler = Handler(Handler.Callback {
        recorderCallback?.onError(lastKnownException)
        true
    })

    init {
        val recorderManager = RecorderManager.getInstance(context)
        tableName = "action_record_data_${recorderManager.activeRecorderCount}"
        recorderManager.actionRecorderCreated(tableName)
       // databaseHandler.createTable(tableName, arrayOf("b"),null)
        pojo = PojoExtension(context, tableName)
    }

    /**
     * get rid of all the recording data that is stored in the database
     */
    open fun clearRecordingData() {
       // databaseHandler.clearTable(tableName)
        pojo.releaseAll()
        pojo.closeConnection()
    }

    /**
     * releases any resources that are being used by this recorder
     */
    fun release() {
       // databaseHandler.deleteTable(tableName)
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
     * is called to start the playback when its ready
     */
    internal val playbackReadyListener = object: PlaybackUtil.PlaybackReadyListener{
        override fun onReady() {
            if(isInPlayBackMode) {
                playbackUtil.startPlayback(actionTriggerListener!!)
            }
        }
    }

    /**
     * get the number of entries that are currently in the database
     */
    fun getEntryCount(): Long{
       val count = pojo.count
        pojo.closeConnection()
        return count
    }

    /**
     * a data class for storing an action performed in the database, it holds the elapsed time
     */
    internal data class ActionPerformedEntry<E>(var data: E, var duration: Long)
}