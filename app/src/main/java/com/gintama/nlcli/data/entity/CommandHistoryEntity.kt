package com.gintama.nlcli.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawInput: String,
    val app: String,
    val action: String,
    val contact: String?,
    val sanitizedPayload: String?,
    val success: Boolean,
    val resultMessage: String,
    val source: String,
    val timestampMs: Long = System.currentTimeMillis()
)
