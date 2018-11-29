package com.nosetrap.eventrecordlib.util

internal class IDUtil {

    companion object {
        private val tableNamePrefix = "recording_"
        fun toRecordingTableName(id: Int): String{
            return "$tableNamePrefix$id"
        }

        fun getRecordingId(recordingTable: String): Int{
            val id = recordingTable.substring(tableNamePrefix.length,recordingTable.length)
            return id.toInt()
        }
    }
}