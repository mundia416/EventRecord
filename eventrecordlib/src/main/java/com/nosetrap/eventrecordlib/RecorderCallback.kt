package com.nosetrap.eventrecordlib

/**
 * callback interface for the actions that a recorder takes
 */
abstract class RecorderCallback {

    /**
     * is called when recording is started
     */
     open fun onRecordingStarted(){
    }

    /**
     * is called after the recording is saved in the database
     * is called after onRecordingSaveProgress(...) finishes its progress
     */
    open fun onRecordingSaved(){

    }

    /**
     * is called when recording is stopped
     * is called before onRecordingSaveProgress(...) starts getting called
     *
     */
    open fun onRecordingStopped(){

    }

    /**
     * is called to show the progress of saving the data in the sql database
     * @param progress is the current progress of saving the data, it is a value between 0 and 100
     * progress bars can be put in this method
     */
    open fun onRecordingSaveProgress(progress: Double){

    }

    /**
     * is called when before querry is made to the database for the recording. this is the method to
     * add any progressbars
     */
    open fun onPrePlayback(){

    }
    /**
     * isCalled when the playback is ready to start
     */
    open fun onPlaybackStarted(){

    }

    /**
     * isCalled when the playback has stopped
     */
    open fun onPlaybackStopped(){

    }
}