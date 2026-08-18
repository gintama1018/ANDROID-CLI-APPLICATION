package com.gintama.nlcli.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey
    val name: String,
    val value: String,
    val timestampMs: Long = System.currentTimeMillis()
)
