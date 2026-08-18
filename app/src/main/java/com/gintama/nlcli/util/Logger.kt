package com.gintama.nlcli.util

import android.util.Log

object Logger {
    private const val TAG = "NLCLI"
    var verboseLoggingEnabled: Boolean = false

    fun d(message: String) {
        if (verboseLoggingEnabled) {
            try {
                Log.d(TAG, message)
            } catch (_: Throwable) {
                println("DEBUG: [$TAG] $message")
            }
        }
    }

    fun i(message: String) {
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
            println("INFO: [$TAG] $message")
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        } catch (_: Throwable) {
            println("WARN: [$TAG] $message ${throwable?.message ?: ""}")
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        } catch (_: Throwable) {
            println("ERROR: [$TAG] $message ${throwable?.message ?: ""}")
        }
    }

    fun sanitizeForLog(text: String?): String {
        if (text == null) return "<null>"
        if (verboseLoggingEnabled) return text
        // Mask payload to prevent sensitive info leakage in logs
        return if (text.length <= 4) "****" else "${text.take(2)}***${text.takeLast(2)}"
    }

    fun maskPhoneNumber(number: String?): String {
        if (number.isNullOrBlank()) return "<null>"
        if (verboseLoggingEnabled) return number
        val digits = number.filter { it.isDigit() || it == '+' }
        return if (digits.length <= 4) "****" else "${digits.take(3)}****${digits.takeLast(2)}"
    }

    fun maskCommandInput(rawInput: String, payload: String?): String {
        if (verboseLoggingEnabled || payload.isNullOrBlank()) return rawInput
        val masked = sanitizeForLog(payload)
        return rawInput.replace(payload, masked)
    }
}
