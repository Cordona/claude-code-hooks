package com.cordona.claudecodehooks.shared.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object CryptoUtils {
    
    fun generateLookupHash(apiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(apiKey.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}