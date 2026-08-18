package com.bionicpro.auth.service

import com.bionicpro.auth.util.PkceUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.*

@Service
class TokenService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val webClient: WebClient,
    private val textEncryptor: TextEncryptor,

    @Value("\${keycloak.token-url}")
    private val tokenUrl: String,

    @Value("\${keycloak.client-id}")
    private val clientId: String,

    @Value("\${keycloak.redirect-uri}")
    private val redirectUri: String,

    @Value("\${app.session-ttl-seconds}")
    private val sessionTtlSeconds: Long,

    @Value("\${app.access-token-ttl-seconds}")
    private val accessTokenTtlSeconds: Long
) {

    fun generateAndStoreCodeVerifier(state: String): String {
        val codeVerifier = PkceUtil.generateCodeVerifier()
        redisTemplate.opsForValue().set("pkce:$state", codeVerifier, java.time.Duration.ofMinutes(30))
        return codeVerifier
    }

    fun exchangeCodeForTokens(code: String, state: String): Map<String, Any> {
        val codeVerifier = redisTemplate.opsForValue().get("pkce:$state")
            ?: throw IllegalStateException("Invalid state or code verifier expired")

        try {
            val response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                    .with("code", code)
                    .with("redirect_uri", redirectUri)
                    .with("client_id", clientId)
                    .with("code_verifier", codeVerifier))
                .retrieve()
                .bodyToMono<Map<String, Any>>()
                .block() ?: throw IllegalStateException("Token exchange failed")
            redisTemplate.delete("pkce:$state")
            return response
        } catch (e: Exception) {
            throw e
        }
    }

    fun refreshAccessToken(refreshToken: String): Map<String, Any> {
        val response = webClient.post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                .with("refresh_token", refreshToken)
                .with("client_id", clientId))
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block() ?: throw IllegalStateException("Token refresh failed")
        return response
    }

    fun storeTokens(sessionId: String, accessToken: String, refreshToken: String) {
        val encryptedRefresh = textEncryptor.encrypt(refreshToken)
        val data = mapOf(
            "access_token" to accessToken,
            "refresh_token" to encryptedRefresh
        )
        redisTemplate.opsForHash<String, String>().putAll("session:$sessionId", data)
        redisTemplate.expire("session:$sessionId", java.time.Duration.ofSeconds(sessionTtlSeconds))
    }

    fun getTokens(sessionId: String): Pair<String, String>? {
        val entries = redisTemplate.opsForHash<String, String>().entries("session:$sessionId")
        val accessToken = entries["access_token"] ?: return null
        val encryptedRefresh = entries["refresh_token"] ?: return null
        val refreshToken = textEncryptor.decrypt(encryptedRefresh)
        return Pair(accessToken, refreshToken)
    }

    fun updateAccessToken(sessionId: String, newAccessToken: String, newRefreshToken: String?) {
        val encryptedRefresh = newRefreshToken?.let { textEncryptor.encrypt(it) }
        val updates = mutableMapOf("access_token" to newAccessToken)
        if (encryptedRefresh != null) updates["refresh_token"] = encryptedRefresh
        redisTemplate.opsForHash<String, String>().putAll("session:$sessionId", updates)
        redisTemplate.expire("session:$sessionId", java.time.Duration.ofSeconds(sessionTtlSeconds))
    }

    fun deleteSession(sessionId: String) {
        redisTemplate.delete("session:$sessionId")
    }

    fun hasSession(sessionId: String): Boolean {
        return redisTemplate.hasKey("session:$sessionId")
    }
}
