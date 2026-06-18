package com.smartremote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

class DeviceConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("DeviceConnectionService started")
        return START_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("DeviceConnectionService destroyed")
    }
}
