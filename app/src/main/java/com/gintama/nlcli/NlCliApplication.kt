package com.gintama.nlcli

import android.app.Application
import com.gintama.nlcli.data.AppDatabase
import com.gintama.nlcli.util.Logger

class NlCliApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.i("NLCLI Application initialized")
        try {
            AppDatabase.getInstance(this)
        } catch (e: Exception) {
            Logger.e("Failed to initialize database eagerly in Application.onCreate", e)
        }
    }
}
