package com.gintama.nlcli.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_cache")
data class ContactCacheEntity(
    @PrimaryKey
    val lookupKey: String, // lowercased queried name
    val displayName: String,
    val rawPhoneNumber: String,
    val normalizedPhoneNumber: String,
    val lastUsedTimestampMs: Long = System.currentTimeMillis()
)
