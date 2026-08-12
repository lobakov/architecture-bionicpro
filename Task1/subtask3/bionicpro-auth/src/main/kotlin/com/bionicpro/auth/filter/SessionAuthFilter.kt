package com.bionicpro.auth.filter

import com.bionicpro.auth.service.SessionService
import com.bionicpro.auth.service.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.WebUtils

class SessionAuthFilter(
    private val tokenService: TokenService,
    private val sessionService: SessionService,

    @Value("\${app.session-ttl-seconds}")
    private val sessionTtlSeconds: Long
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val path = request.requestURI

        if (path.startsWith("/auth/") || path == "/health") {
            filterChain.doFilter(request, response)
            return
        }

        val cookie = WebUtils.getCookie(request, "session")
        if (cookie == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session cookie missing")
            return
        }

        val oldSessionId = cookie.value
        if (!tokenService.hasSession(oldSessionId)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid session")
            return
        }

        val tokens = tokenService.getTokens(oldSessionId)
        if (tokens == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session data not found")
            return
        }

        val newSessionId = sessionService.rotateSessionId()
        tokenService.storeTokens(newSessionId, tokens.first, tokens.second)
        tokenService.deleteSession(oldSessionId)

        val newCookie = Cookie("session", newSessionId)
        newCookie.isHttpOnly = true
        newCookie.secure = false   // в проде true
        newCookie.path = "/"
        newCookie.maxAge = sessionTtlSeconds.toInt()
        response.addCookie(newCookie)

        request.setAttribute("access_token", tokens.first)
        request.setAttribute("new_session_id", newSessionId)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val authentication = UsernamePasswordAuthenticationToken("user", null, authorities)
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }
}
