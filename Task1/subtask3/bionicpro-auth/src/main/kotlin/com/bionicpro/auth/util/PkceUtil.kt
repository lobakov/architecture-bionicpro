package com.bionicpro.auth.util

import java.security.MessageDigest
import java.util.Base64

object PkceUtil {

    fun generateCodeVerifier(length: Int = 64): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val random = java.security.SecureRandom()
        return (1..length)
            .map { charset[random.nextInt(charset.length)] }
            .joinToString("")
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
