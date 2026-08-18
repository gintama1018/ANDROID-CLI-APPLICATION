package com.gintama.nlcli.contacts

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.gintama.nlcli.data.dao.ContactCacheDao
import com.gintama.nlcli.data.entity.ContactCacheEntity
import com.gintama.nlcli.util.Logger
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class ContactResolver(
    private val context: Context,
    private val contactCacheDao: ContactCacheDao
) {

    /**
     * Resolves a contact name or raw phone number to a ResolvedContact.
     * If query is already a phone number, returns it directly.
     * Checks Room cache first, then queries ContactsContract.
     */
    suspend fun resolveContact(query: String): ContactResolutionResult = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext ContactResolutionResult.NotFound("Empty contact query")
        }

        // 1. Direct phone number check
        if (PhoneNormalizer.isValidPhoneNumber(trimmedQuery)) {
            val normalized = PhoneNormalizer.normalizeToE164(trimmedQuery)
            return@withContext ContactResolutionResult.Found(
                ResolvedContact(
                    id = "direct_number",
                    displayName = trimmedQuery,
                    rawPhoneNumber = trimmedQuery,
                    normalizedPhoneNumber = normalized,
                    matchConfidence = 1.0f,
                    isFuzzyMatch = false
                )
            )
        }

        // 2. Check Room cache
        val lookupKey = trimmedQuery.lowercase()
        try {
            val cached = contactCacheDao.findByLookupKey(lookupKey)
            if (cached != null) {
                Logger.d("Contact '$trimmedQuery' resolved from local cache: ${cached.displayName}")
                // Update last used timestamp
                contactCacheDao.insertOrUpdate(cached.copy(lastUsedTimestampMs = System.currentTimeMillis()))
                return@withContext ContactResolutionResult.Found(
                    ResolvedContact(
                        id = "cached_${cached.lookupKey}",
                        displayName = cached.displayName,
                        rawPhoneNumber = cached.rawPhoneNumber,
                        normalizedPhoneNumber = cached.normalizedPhoneNumber,
                        matchConfidence = 1.0f,
                        isFuzzyMatch = false
                    )
                )
            }
        } catch (e: Exception) {
            Logger.w("Failed to query contact cache", e)
        }

        // 3. Check permission
        if (!PermissionHelper.hasContactsPermission(context)) {
            Logger.w("READ_CONTACTS permission not granted")
            return@withContext ContactResolutionResult.PermissionDenied(
                "READ_CONTACTS permission is required to look up '${trimmedQuery}' by name"
            )
        }

        // 4. Query ContactsContract
        val candidates = queryContactsProvider(trimmedQuery)
        if (candidates.isEmpty()) {
            return@withContext ContactResolutionResult.NotFound("No contact found matching '$trimmedQuery'")
        }

        // Find exact match (case-insensitive)
        val exactMatch = candidates.firstOrNull {
            it.displayName.equals(trimmedQuery, ignoreCase = true)
        }

        if (exactMatch != null) {
            cacheContact(lookupKey, exactMatch)
            return@withContext ContactResolutionResult.Found(exactMatch)
        }

        // Calculate Levenshtein similarity scores
        val scoredCandidates = candidates.map { contact ->
            val similarity = calculateSimilarity(lookupKey, contact.displayName.lowercase())
            contact.copy(matchConfidence = similarity, isFuzzyMatch = true)
        }.sortedByDescending { it.matchConfidence }

        val bestMatch = scoredCandidates.first()

        // If top match has high confidence
        if (bestMatch.matchConfidence >= 0.60f) {
            Logger.d("Fuzzy resolved contact '$trimmedQuery' -> '${bestMatch.displayName}' (score: ${bestMatch.matchConfidence})")
            cacheContact(lookupKey, bestMatch)

            if (scoredCandidates.size > 1 && (scoredCandidates[0].matchConfidence - scoredCandidates[1].matchConfidence) < 0.15f) {
                // Ambiguous close matches
                return@withContext ContactResolutionResult.Ambiguous(
                    bestMatch = bestMatch,
                    candidates = scoredCandidates.take(4)
                )
            }

            return@withContext ContactResolutionResult.Found(bestMatch)
        }

        return@withContext ContactResolutionResult.NotFound("Could not confidently match '$trimmedQuery'. Closest: ${bestMatch.displayName}")
    }

    private fun queryContactsProvider(searchPattern: String): List<ResolvedContact> {
        val results = mutableListOf<ResolvedContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$searchPattern%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""

                    if (name.isNotBlank() && number.isNotBlank()) {
                        val normalized = PhoneNormalizer.normalizeToE164(number)
                        results.add(
                            ResolvedContact(
                                id = id,
                                displayName = name,
                                rawPhoneNumber = number,
                                normalizedPhoneNumber = normalized,
                                matchConfidence = 1.0f,
                                isFuzzyMatch = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Error querying contacts provider", e)
        } finally {
            cursor?.close()
        }

        return results.distinctBy { it.displayName to it.normalizedPhoneNumber }
    }

    private suspend fun cacheContact(lookupKey: String, contact: ResolvedContact) {
        try {
            contactCacheDao.insertOrUpdate(
                ContactCacheEntity(
                    lookupKey = lookupKey,
                    displayName = contact.displayName,
                    rawPhoneNumber = contact.rawPhoneNumber,
                    normalizedPhoneNumber = contact.normalizedPhoneNumber,
                    lastUsedTimestampMs = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Logger.w("Failed to save contact to cache", e)
        }
    }

    companion object {
        /**
         * Calculates Levenshtein similarity score between 0.0 and 1.0.
         */
        fun calculateSimilarity(s1: String, s2: String): Float {
            if (s1 == s2) return 1.0f
            if (s1.isEmpty() || s2.isEmpty()) return 0.0f
            if (s2.contains(s1) || s1.contains(s2)) {
                return 0.85f + (0.15f * (min(s1.length, s2.length).toFloat() / max(s1.length, s2.length)))
            }

            val distance = levenshteinDistance(s1, s2)
            val maxLength = max(s1.length, s2.length)
            return (1.0f - (distance.toFloat() / maxLength)).coerceIn(0.0f, 1.0f)
        }

        fun levenshteinDistance(s1: String, s2: String): Int {
            val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

            for (i in 0..s1.length) dp[i][0] = i
            for (j in 0..s2.length) dp[0][j] = j

            for (i in 1..s1.length) {
                for (j in 1..s2.length) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    dp[i][j] = min(
                        min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                    )
                }
            }

            return dp[s1.length][s2.length]
        }
    }
}

sealed class ContactResolutionResult {
    data class Found(val contact: ResolvedContact) : ContactResolutionResult()
    data class Ambiguous(val bestMatch: ResolvedContact, val candidates: List<ResolvedContact>) : ContactResolutionResult()
    data class NotFound(val reason: String) : ContactResolutionResult()
    data class PermissionDenied(val message: String) : ContactResolutionResult()
}
