package com.nosetrap.eventrecordlib.recorder

import android.content.Context
import android.os.Handler
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.RecorderManager
import com.nosetrap.eventrecordlib.callback.BaseRecorderCallback
import com.nosetrap.eventrecordlib.util.IDUtil
import com.nosetrap.eventrecordlib.util.PlaybackUtil
import com.nosetrap.storage.pojo.Pojo

/**
 * base class for action recorders
 *  * @classType T specifies the pojo object class type which will be used to store data
 *  @param id represents a single id which will be used to identify a recorder, no 2 IDs should ever
 *  have the same ID
 */
abstract class BaseActionRecorder<T>(private val context: Context,id: Int) {
    companion object {
        /**
         * playback unlimited times
         */
        const val PLAYBACK_UNLIMITED = -1
    }

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

    private val playbackUtil = PlaybackUtil<T>(context,this)

    /**
     * the recorder callback to receive callback methods
     */
    internal var recorderCallback: BaseRecorderCallback? = object :BaseRecorderCallback{}


    /**
     * the key for the different pojo object entries which will be inserted into the database
     */
    internal val keyPojoData = "pojo_key_entry_"

    protected val recorderManager = RecorderManager.getInstance(context)

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
        override fun onReady(recordingTableName: String) {
            startPlayback(recordingTableName)
        }

        override fun onReady(recordingId: Int) {
           startPlayback(IDUtil.toRecordingTableName(recordingId))
        }
    }

    /**
     *
     */
     private fun startPlayback(recordingTableName: String) {
        if (isInPlayBackMode) {
            playbackUtil.startPlayback(recordingTableName, actionTriggerListener!!)
        }
    }

    /**
     * get the number of entries that are currently in a recording
     */
    fun getEntryCount(recordingId: Int): Long{
        val pojo = Pojo(context,IDUtil.toRecordingTableName(recordingId))
       return pojo.count
    }

    /**
     * a data class for storing an action performed in the database, it holds the elapsed time
     */
    internal data class ActionPerformedEntry<E>(var data: E, var duration: Long)
}