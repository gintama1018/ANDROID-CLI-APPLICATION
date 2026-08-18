package com.gintama.nlcli.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey
    val name: String,
    val commandSequence: String,
    val timestampMs: Long = System.currentTimeMillis()
)
