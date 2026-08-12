package com.bionicpro.auth.controller

import com.bionicpro.auth.service.TokenService
import com.bionicpro.auth.service.SessionService
import com.bionicpro.auth.util.PkceUtil
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.view.RedirectView
import java.time.Duration

@RestController
@RequestMapping("/auth")
class AuthController(
    private val tokenService: TokenService,
    private val sessionService: SessionService,

    @Value("\${keycloak.auth-url}")
    private val authUrl: String,

    @Value("\${keycloak.client-id}")
    private val clientId: String,

    @Value("\${keycloak.redirect-uri}")
    private val redirectUri: String,

    @Value("\${app.session-ttl-seconds}")
    private val sessionTtlSeconds: Long
) {

    @GetMapping("/login")
    fun login(response: HttpServletResponse): RedirectView {
        val state = java.util.UUID.randomUUID().toString()
        val codeVerifier = tokenService.generateAndStoreCodeVerifier(state)
        val codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier)

        val url = "$authUrl?" +
                "response_type=code" +
                "&client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&state=$state" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256" +
                "&scope=openid+profile+email+offline_access"

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
        response.setHeader("Pragma", "no-cache")
        return RedirectView(url)
    }

    @GetMapping("/callback")
    fun callback(
        @RequestParam("code") code: String?,
        @RequestParam("state") state: String?,
        response: HttpServletResponse
    ): RedirectView {
        if (code == null || state == null) {
            return RedirectView("http://localhost:3000")
        }
        try {
            val tokens = tokenService.exchangeCodeForTokens(code, state)
            val accessToken = tokens["access_token"] as? String ?: return RedirectView("http://localhost:3000")
            val refreshToken = tokens["refresh_token"] as? String ?: return RedirectView("http://localhost:3000")

            val sessionId = sessionService.createSessionId()
            tokenService.storeTokens(sessionId, accessToken, refreshToken)

            val cookie = Cookie("session", sessionId)
            cookie.isHttpOnly = true
            cookie.secure = false
            cookie.path = "/"
            cookie.maxAge = sessionTtlSeconds.toInt()
            response.addCookie(cookie)

            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
            response.setHeader("Pragma", "no-cache")
            return RedirectView("http://localhost:3000")
        } catch (e: Exception) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
            return RedirectView("http://localhost:3000?error=auth")
        }
    }

    @GetMapping("/session")
    fun checkSession(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Map<String, String>> {
        val cookie = request.cookies?.firstOrNull { it.name == "session" }
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val sessionId = cookie.value
        if (!tokenService.hasSession(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(mapOf("status" to "authenticated"))
    }
}
