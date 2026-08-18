package com.gintama.nlcli

import android.app.Application
import com.gintama.nlcli.data.AppDatabase
import com.gintama.nlcli.util.Logger

class NlCliApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.i("NLCLI Application initialized")
        // Initialize Room Database eagerly
        AppDatabase.getInstance(this)
    }
}
