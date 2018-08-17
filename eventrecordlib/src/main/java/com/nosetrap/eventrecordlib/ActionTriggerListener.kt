package com.nosetrap.eventrecordlib

import com.google.gson.JsonObject

/*
 * T must match that of the ActionRecorder otherwise a ClassCastException will be thrown
 */
interface ActionTriggerListener{
    /**
     * data is the json string of the data that was stored.
     * use Gson to convert it to your desired object
     */
    fun onTrigger(data: JsonObject)
}