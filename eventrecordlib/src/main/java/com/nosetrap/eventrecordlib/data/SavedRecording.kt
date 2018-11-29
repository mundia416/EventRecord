package com.nosetrap.eventrecordlib.data

/**
 * @param id is the ID of the saved recording
 * @param timestamp is the time when the recording was created
 */
data class SavedRecording(var id:Int,var title: String? = null,var timestamp: Long = 0)
    : Comparable<SavedRecording> {
    override fun compareTo(other: SavedRecording): Int {
        return if(this.timestamp == other.timestamp){
            0
        }else if (timestamp > other.timestamp){
            1
        }else{
            -1
        }
    }
}