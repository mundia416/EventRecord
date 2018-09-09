package com.nosetrap.eventrecordlib.callback

interface BaseRecorderCallback {

    /**
     * is called before query is made to the database for the recording. this is the method to
     * add any progressbars
     */
    open fun onPrePlayback() {

    }

    /**
     * isCalled when the playback is ready to start
     */
    open fun onPlaybackStarted() {

    }

    /**
     * isCalled when the playback has stopped
     */
    open fun onPlaybackStopped() {

    }

    /**
     * is called when there is an exception
     */
    open fun onError(e: Exception) {

    }
}