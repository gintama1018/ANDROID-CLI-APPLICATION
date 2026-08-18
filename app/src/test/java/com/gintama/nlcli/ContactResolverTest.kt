package com.gintama.nlcli

import com.gintama.nlcli.contacts.ContactResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactResolverTest {

    @Test
    fun testExactSimilarity() {
        val score = ContactResolver.calculateSimilarity("rahul", "rahul")
        assertEquals(1.0f, score, 0.001f)
    }

    @Test
    fun testSubstringSimilarity() {
        val score = ContactResolver.calculateSimilarity("rahul", "rahul sharma")
        assertTrue("Expected similarity > 0.8, got $score", score >= 0.85f)
    }

    @Test
    fun testLevenshteinFuzzySimilarity() {
        val score = ContactResolver.calculateSimilarity("rahul", "rahul")
        assertTrue(score > 0.7f)

        val typoScore = ContactResolver.calculateSimilarity("alexa", "alex")
        assertTrue("Typo score should be high, got $typoScore", typoScore > 0.7f)
    }

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, ContactResolver.levenshteinDistance("kitten", "kitten"))
        assertEquals(3, ContactResolver.levenshteinDistance("kitten", "sitting"))
        assertEquals(1, ContactResolver.levenshteinDistance("rahul", "rahol"))
    }
}
