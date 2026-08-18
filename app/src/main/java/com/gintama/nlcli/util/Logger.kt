package com.gintama.nlcli.util

import android.util.Log

object Logger {
    private const val TAG = "NLCLI"
    var verboseLoggingEnabled: Boolean = false

    fun d(message: String) {
        if (verboseLoggingEnabled) {
            Log.d(TAG, message)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }

    fun sanitizeForLog(text: String?): String {
        if (text == null) return "<null>"
        if (verboseLoggingEnabled) return text
        // Mask payload to prevent sensitive info leakage in logs
        return if (text.length <= 4) "****" else "${text.take(2)}***${text.takeLast(2)}"
    }
}
