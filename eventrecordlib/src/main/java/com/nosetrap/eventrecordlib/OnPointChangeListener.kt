package com.nosetrap.eventrecordlib

interface OnPointChangeListener {

    fun onChange(point: Point)

    /**
     * called when there is an exception when tring to change the position of a view
     */
    fun onError(t: Throwable){

    }
}