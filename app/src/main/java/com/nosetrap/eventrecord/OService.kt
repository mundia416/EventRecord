package com.nosetrap.eventrecord

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.nosetrap.draglib.overlay.DraggableOverlayOnTouchListener
import com.nosetrap.draglib.overlay.DraggableOverlayService
import com.nosetrap.draglib.overlay.OnDragListener
import com.nosetrap.eventrecordlib.OnActionTriggerListener
import com.nosetrap.eventrecordlib.RecorderCallback
import com.nosetrap.eventrecordlib.RecorderManager
import com.nosetrap.eventrecordlib.recorders.ActionRecorder
import com.nosetrap.eventrecordlib.recorders.OverlayMovementRecorder

class OService : DraggableOverlayService() {
    private lateinit var playButtonOnTouchListener : DraggableOverlayOnTouchListener
    private lateinit var viewOnTouchListener: DraggableOverlayOnTouchListener
    private lateinit var playbackOnTouchListener:DraggableOverlayOnTouchListener

    private lateinit var movementRecorder: OverlayMovementRecorder
    private lateinit var actionRecorder: ActionRecorder

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

        windowManager.addView(view,params)

        movementRecorder = OverlayMovementRecorder(this)
        movementRecorder.assignView(params)

        val triggerOn = "On"
        val triggerOff = "Off"

        //prepare the action trigger
        actionRecorder = ActionRecorder(this)
        val trigger = view.findViewById<Button>(R.id.trigger)
        trigger.text = triggerOff
        trigger.setOnClickListener {
            actionRecorder.actionPerformed()
            if(trigger.text.toString().equals(triggerOn)){
                trigger.text = triggerOff
            }else{
                trigger.text = triggerOn
            }
        }
        val onActionTrigger = object: OnActionTriggerListener{
            override fun onTrigger() {
                if(trigger.text.toString().equals(triggerOn)){
                    trigger.text = triggerOff
                }else{
                    trigger.text = triggerOn
                }
            }

            override fun onError(e: Throwable) {
            }
        }


        //instatiating the draggableOverlay OnTouchListeners
        playButtonOnTouchListener = DraggableOverlayOnTouchListener(view,params)
        playbackOnTouchListener = DraggableOverlayOnTouchListener(view,params)
        viewOnTouchListener = DraggableOverlayOnTouchListener(view,params)

        //onDragListener
        val onDragListener = object: OnDragListener{
            override fun onDrag(view: View) {
                movementRecorder.viewLocationChanged()
            }

        }

        //settings on drag listener
        viewOnTouchListener.setOnDragListener(onDragListener)
        playbackOnTouchListener.setOnDragListener(onDragListener)
        playButtonOnTouchListener.setOnDragListener(onDragListener)


        //setting onClickListeners
        playbackOnTouchListener.setOnClickListener(View.OnClickListener {
            if(movementRecorder.isInPlayback()){
                movementRecorder.stopPlayback()
                actionRecorder.stopPlayback()
            }else{
                movementRecorder.startPlayback(view)
                actionRecorder.startPlayback(onActionTrigger)

            }
        })
        playButtonOnTouchListener.setOnClickListener(View.OnClickListener {
            if(movementRecorder.isRecording()){
                movementRecorder.stopRecording()
                actionRecorder.stopRecording()
            }else{
                movementRecorder.clearRecordingData()
                movementRecorder.startRecording()
                actionRecorder.clearRecordingData()
                actionRecorder.startRecording()

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

        RecorderManager.getInstance(this).setRecorderCallback(arrayOf(movementRecorder,actionRecorder),
                object : RecorderCallback{
            override fun onRecordingStarted() {
                super.onRecordingStarted()
                Toast.makeText(this@OService,"Started Recording",Toast.LENGTH_SHORT).show()
            }

            override fun onRecordingSaved() {
                super.onRecordingSaved()
                Toast.makeText(this@OService,"Stopped Recording",Toast.LENGTH_SHORT).show()
                Log.d("Recording","stopped")

            }

            override fun onRecordingStopped() {
                super.onRecordingStopped()
            }

            override fun onPrePlayback() {
                super.onPrePlayback()
            }

            override fun onRecordingSaveProgress(progress: Double) {
                super.onRecordingSaveProgress(progress)
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