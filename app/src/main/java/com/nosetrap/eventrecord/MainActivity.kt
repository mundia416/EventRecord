package com.nosetrap.eventrecord

import android.content.Intent
import com.nosetrap.applib.Activity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    override fun code() {


        btn.setOnClickListener {
            if(PermissionManager.canDrawOverlays(this)) {
                startService(Intent(this, OService::class.java))
            }else{
                PermissionManager.requestDrawOverlays(this,100)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        startService(Intent(this, OService::class.java))
    }

    override fun initVariables() {

    }

    override fun setView(): Int {
        return R.layout.activity_main
    }
}
