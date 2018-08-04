package com.nosetrap.eventrecordlib

interface OnActionTriggerListener {
    fun onTrigger()

    fun onError(e: Throwable)
}