package com.paan.safaadmin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdminApp : Application(){
    override fun onCreate() {
        super.onCreate()
        com.paan.safaadmin.push.NotificationChannels.create(this)
    }
}