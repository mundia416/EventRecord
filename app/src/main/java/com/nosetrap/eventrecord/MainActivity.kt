package com.nosetrap.eventrecord

import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nosetrap.applib.Activity
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.callback.FixedRecorderCallback
import com.nosetrap.eventrecordlib.recorder.FixedActionRecorder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    lateinit var fRecorder: FixedActionRecorder<Data>
    override fun code() {
        prepareFixedActionRecorder()

    }

    override fun setOnClickListeners() {

        btn.setOnClickListener {
            if(PermissionManager.canDrawOverlays(this)) {
                startService(Intent(this, OService::class.java))
            }else{
                PermissionManager.requestDrawOverlays(this,100)
            }
        }

        btnF.setOnClickListener {
            fRecorder.startPlayback(object : ActionTriggerListener{
                override fun onTrigger(data: JsonObject) {
                    val dataObj = Gson().fromJson(data,Data::class.java)
                    btnF.text = dataObj.btnString
                }
            })
        }
    }

    data class Data(var btnString: String)
    fun prepareFixedActionRecorder(){
        val s1 = fRecorder.insertAction(FixedActionRecorder.Action(Data("1 - 1 sec"),1000))
        val s2 = fRecorder.insertAction(FixedActionRecorder.Action(Data("2 - 2 sec"),2000))
        val s3 = fRecorder.insertAction(FixedActionRecorder.Action(Data("3 - 0.5 sec"),500))
        val s4 = fRecorder.insertAction(FixedActionRecorder.Action(Data("4 - 1 sec"),1000))

        fRecorder.setRecorderCallback(object : FixedRecorderCallback{
            override fun onPrePlayback() {
                super.onPrePlayback()
            }

            override fun onPlaybackStarted() {
                super.onPlaybackStarted()
            }

            override fun onPlaybackStopped() {
                super.onPlaybackStopped()
            }

            override fun onError(e: Exception) {
                super.onError(e)
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        startService(Intent(this, OService::class.java))
    }

    override fun initVariables() {
        fRecorder = FixedActionRecorder(this)

    }

    override fun setView(): Int {
        return R.layout.activity_main
    }
}
