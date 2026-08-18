package com.gintama.nlcli.contacts

object PhoneNormalizer {

    private const val DEFAULT_COUNTRY_CODE = "91" // India (+91)

    /**
     * Normalizes a raw phone number into standard E.164 format with leading '+'.
     * Example: "98765 43210" -> "+919876543210"
     *          "09876543210"  -> "+919876543210"
     *          "+1 (415) 555-2671" -> "+14155552671"
     */
    fun normalizeToE164(raw: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String {
        if (raw.isBlank()) return ""

        val cleaned = raw.replace(Regex("[^0-9+]"), "")

        return when {
            cleaned.startsWith("+") -> {
                cleaned
            }
            cleaned.startsWith("00") -> {
                "+" + cleaned.substring(2)
            }
            cleaned.startsWith("0") && cleaned.length == 11 -> {
                // Leading 0 for 10-digit national number
                "+$defaultCountryCode" + cleaned.substring(1)
            }
            cleaned.length == 10 && !cleaned.startsWith("+") -> {
                // Standard 10-digit local number (e.g., India)
                "+$defaultCountryCode$cleaned"
            }
            cleaned.length > 10 && cleaned.startsWith(defaultCountryCode) -> {
                "+$cleaned"
            }
            else -> {
                if (!cleaned.startsWith("+")) "+$cleaned" else cleaned
            }
        }
    }

    /**
     * Produces the format required by WhatsApp wa.me URLs:
     * Full country code + subscriber number, strictly digits, NO leading '+'.
     * Example: "+919876543210" -> "919876543210"
     */
    fun toWhatsAppUrlNumber(raw: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String {
        val e164 = normalizeToE164(raw, defaultCountryCode)
        return e164.removePrefix("+").replace(Regex("[^0-9]"), "")
    }

    /**
     * Validates if a string looks like a valid phone number.
     */
    fun isValidPhoneNumber(raw: String): Boolean {
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        return digitsOnly.length in 7..15
    }
}
