package com.gintama.nlcli.model

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val command: Command? = null,
    val details: String? = null
)
