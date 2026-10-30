package com.messenger.prime

/**
 * Utility object for normalising contact identifiers.
 * Removes non‑alphanumeric characters, lower‑cases and trims whitespace.
 */
object ContactHelper {
    /**
     * Normalises a raw contact key (e.g., phone number, user id) to a stable form.
     */
    fun normalizeKey(rawKey: String?): String {
        if (rawKey == null) return ""
        return rawKey.replace(Regex("[^A-Za-z0-9]"), "")
            .lowercase()
            .trim()
    }
}
