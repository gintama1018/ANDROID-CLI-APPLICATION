package com.gintama.nlcli

import com.gintama.nlcli.contacts.PhoneNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNormalizerTest {

    @Test
    fun testNormalizeStandardIndianNumber() {
        val normalized = PhoneNormalizer.normalizeToE164("9876543210")
        assertEquals("+919876543210", normalized)
    }

    @Test
    fun testNormalizeWithSpacesAndDashes() {
        val normalized = PhoneNormalizer.normalizeToE164("98765 43210")
        assertEquals("+919876543210", normalized)

        val dashed = PhoneNormalizer.normalizeToE164("9876-543-210")
        assertEquals("+919876543210", dashed)
    }

    @Test
    fun testNormalizeWithLeadingZero() {
        val normalized = PhoneNormalizer.normalizeToE164("09876543210")
        assertEquals("+919876543210", normalized)
    }

    @Test
    fun testNormalizeInternationalNumber() {
        val usNumber = PhoneNormalizer.normalizeToE164("+1 (415) 555-2671")
        assertEquals("+14155552671", usNumber)
    }

    @Test
    fun testToWhatsAppUrlNumber() {
        val waNumber = PhoneNormalizer.toWhatsAppUrlNumber("9876543210")
        assertEquals("919876543210", waNumber)

        val waInternational = PhoneNormalizer.toWhatsAppUrlNumber("+1 415 555 2671")
        assertEquals("14155552671", waInternational)
    }

    @Test
    fun testIsValidPhoneNumber() {
        assertTrue(PhoneNormalizer.isValidPhoneNumber("9876543210"))
        assertTrue(PhoneNormalizer.isValidPhoneNumber("+919876543210"))
        assertTrue(PhoneNormalizer.isValidPhoneNumber("+1-800-555-0199"))
        assertFalse(PhoneNormalizer.isValidPhoneNumber("Rahul"))
        assertFalse(PhoneNormalizer.isValidPhoneNumber("123"))
    }
}
