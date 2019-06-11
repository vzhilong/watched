package com.vincent.watched.demo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.vincent.watched.Watched

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService(Intent(this, TestService::class.java), object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {

            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            }
        }, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        Watched.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Watched.hide()
    }
}
