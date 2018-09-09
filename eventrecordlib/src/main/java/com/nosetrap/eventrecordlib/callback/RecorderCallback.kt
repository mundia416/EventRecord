package com.nosetrap.eventrecordlib.callback

/**
 * callback interface for the actions that a recorder takes
 */
 interface RecorderCallback : BaseRecorderCallback {

    /**
     * is called when recording is started
     */
    open fun onRecordingStarted() {
    }

    /**
     * is called after the recording is saved in the database
     * is called after onRecordingSaveProgress(...) finishes its progress
     */
    open fun onRecordingSaved() {

    }

    /**
     * is called to show the progress of saving the data in the sql database
     * @param progress is the current progress of saving the data, it is a value between 0 and 100
     * progress bars can be put in this method
     */
    open fun onRecordingSaveProgress(progress: Double) {

    }

    /**
     * is called when recording is stopped
     * is called before onRecordingSaveProgress(...) starts getting called
     *
     */
    open fun onRecordingStopped() {

    }
}