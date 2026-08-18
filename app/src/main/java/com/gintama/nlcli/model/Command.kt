package com.gintama.nlcli.model

data class Command(
    val app: AppType,
    val action: ActionType,
    val contact: String? = null,
    val payload: String? = null,
    val rawInput: String,
    val source: ParseSource = ParseSource.REGEX,
    val confidence: Float = 1.0f,
    val timestampMs: Long = System.currentTimeMillis()
)
