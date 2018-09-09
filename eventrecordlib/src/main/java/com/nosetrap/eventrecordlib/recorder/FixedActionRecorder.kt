package com.nosetrap.eventrecordlib.recorder

import android.content.Context
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.callback.FixedRecorderCallback

/**
 * action recorder that records the actions with the time between actions being fixed
 * the duration between actions is manually set
 */
class FixedActionRecorder<T>(context: Context) : BaseActionRecorder<T>(context) {

    /**
     *  set a recorder callback
     */
    fun setRecorderCallback(recorderCallback: FixedRecorderCallback) {
        this.recorderCallback = recorderCallback
    }

    /**
     * call when an action is performed in order to record it as a trigger
     * @param data a pojo object which contains data to be recorded
     * @return true if the action has been inserted into the database
     */
    fun insertAction(action: Action<T>): Boolean {
        return try {
            val entryKey = keyPojoData + pojo.count
            pojo.insert(entryKey, ActionPerformedEntry<T>(action.data, action.nextActionMillis))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * @param dataClassType the class of the pojo to recieve in the onTrigger method
     *
     */
    fun startPlayback(actionTriggerListener: ActionTriggerListener) {
        this.actionTriggerListener = actionTriggerListener
        isInPlayBackMode = true

        recorderCallback?.onPrePlayback()

        playbackReadyListener.onReady()
    }

    /**
     * @param data the data to insert into the database for playback
     * @param nextActionMillis defines the time that the next action entry should be triggered
     */
    data class Action<E>(var data: E, var nextActionMillis: Long)
}