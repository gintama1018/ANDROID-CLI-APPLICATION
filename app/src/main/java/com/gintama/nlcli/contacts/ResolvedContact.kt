package com.gintama.nlcli.contacts

data class ResolvedContact(
    val id: String,
    val displayName: String,
    val rawPhoneNumber: String,
    val normalizedPhoneNumber: String,
    val matchConfidence: Float = 1.0f,
    val isFuzzyMatch: Boolean = false
)
