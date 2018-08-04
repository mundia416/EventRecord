package com.nosetrap.eventrecordlib

import android.content.Context

/**
 * keeps track of the number of initialised objects
 */
internal class ObjectManager private constructor(context: Context){

    /**
     * keeps count of how many OverlayMovementRecorders have been created,this is useful for deciding
     * the name of the sql database
     */
    var numOverlayRecorders = 0

    /**
     * keeps count of how many OverlayMovementRecorders have been created,this is useful for deciding
     * the name of the sql database
     */
    var numActionRecorders = 0


    companion object {
        private var uniqueInstance: ObjectManager? = null

        fun getInstance(context: Context): ObjectManager {
            if(uniqueInstance == null){
                uniqueInstance = ObjectManager(context)
            }
            return  uniqueInstance!!
        }
    }
}