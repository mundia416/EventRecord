package com.nosetrap.eventrecord

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nosetrap.draglib.overlay.DraggableOverlayOnTouchListener
import com.nosetrap.draglib.overlay.DraggableOverlayService
import com.nosetrap.draglib.overlay.OnDragListener
import com.nosetrap.eventrecordlib.ActionTriggerListener
import com.nosetrap.eventrecordlib.callback.RecorderCallback
import com.nosetrap.eventrecordlib.RecorderManager
import com.nosetrap.eventrecordlib.recorder.ActionRecorder

class OService : DraggableOverlayService() {
    private lateinit var playButtonOnTouchListener : DraggableOverlayOnTouchListener
    private lateinit var viewOnTouchListener: DraggableOverlayOnTouchListener
    private lateinit var playbackOnTouchListener:DraggableOverlayOnTouchListener

    private lateinit var recorder: ActionRecorder<RecordingData>

        override fun code(intent: Intent) {
        val view = inflateView(R.layout.overlay)
        // val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                else WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT)

        val recorderManager = RecorderManager.getInstance(this)
        recorderManager.releaseAll()

        windowManager.addView(view, params)


        //prepare the action trigger
        recorder = ActionRecorder(this)
        val trigger = view.findViewById<Switch>(R.id.trigger)

        trigger.setOnCheckedChangeListener { _, isChecked ->
            val recordingData = RecordingData(params.x, params.y, isChecked)
            recorder.actionPerformed(recordingData)
        }

        /**
         *
         */
        val onActionTrigger = object: ActionTriggerListener{
            override fun onTrigger(data: JsonObject) {
                val data = Gson().fromJson<RecordingData>(data,RecordingData::class.java)
                trigger.isChecked = data.trigger
                params.x = data.x
                params.y = data.y
                windowManager.updateViewLayout(view, params)
            }

        }


        //instatiating the draggableOverlay OnTouchListeners
        playButtonOnTouchListener = DraggableOverlayOnTouchListener(view,params)
        playbackOnTouchListener = DraggableOverlayOnTouchListener(view,params)
        viewOnTouchListener = DraggableOverlayOnTouchListener(view,params)

        //onDragListener
        val onDragListener = object: OnDragListener{
            override fun onDrag(view: View) {
                val recordingData = RecordingData(params.x, params.y, trigger.isChecked)
                recorder.actionPerformed(recordingData)
            }

        }

        //settings on drag listener
        viewOnTouchListener.setOnDragListener(onDragListener)
        playbackOnTouchListener.setOnDragListener(onDragListener)
        playButtonOnTouchListener.setOnDragListener(onDragListener)


        //setting onClickListeners
        playbackOnTouchListener.setOnClickListener(View.OnClickListener {
            if(recorder.isInPlayBackMode){
                recorder.stopPlayback()
            }else{
                recorder.startPlayback(onActionTrigger)
            }
        })
        //playback button
        playButtonOnTouchListener.setOnClickListener(View.OnClickListener {
            if(recorder.isInRecordMode){
                recorder.stopRecording()
            }else{
                recorder.clearRecordingData()
                recorder.startRecording()
            }
        })



//setting onTouchListeners to the views
        view.setOnTouchListener(viewOnTouchListener)
        (view.findViewById<Button>(R.id.btn)).setOnTouchListener(playButtonOnTouchListener)
        (view.findViewById<Button>(R.id.playback)).setOnTouchListener(playbackOnTouchListener)
    }

    override fun registerDraggableTouchListener() {
       registerOnTouchListener(playButtonOnTouchListener)
        registerOnTouchListener(viewOnTouchListener)
        registerOnTouchListener(playbackOnTouchListener)

        recorder.setRecorderCallback(object : RecorderCallback {

            override fun onRecordingSaved() {
                super.onRecordingSaved()
                Toast.makeText(this@OService,"Saved Recording",Toast.LENGTH_SHORT).show()

            }

            override fun onRecordingSaveProgress(progress: Double) {
                super.onRecordingSaveProgress(progress)
            }

            override fun onRecordingStopped() {
                super.onRecordingStopped()
                Toast.makeText(this@OService,"Stopped Recording",Toast.LENGTH_SHORT).show()

            }

            override fun onPrePlayback() {
                super.onPrePlayback()
                Toast.makeText(this@OService,"PrePlayback",Toast.LENGTH_SHORT).show()

            }

            override fun onError(e: Exception) {
                Toast.makeText(this@OService,"Error",Toast.LENGTH_SHORT).show()

            }

            override fun onRecordingStarted() {
                super.onRecordingStarted()
                Toast.makeText(this@OService,"Started Recording",Toast.LENGTH_SHORT).show()
            }



            override fun onPlaybackStopped() {
                super.onPlaybackStopped()
                Toast.makeText(this@OService,"Stopped Playback",Toast.LENGTH_SHORT).show()
            }

            override fun onPlaybackStarted() {
                super.onPlaybackStarted()
                Toast.makeText(this@OService,"Started Playback",Toast.LENGTH_SHORT).show()

            }
        })
    }
}