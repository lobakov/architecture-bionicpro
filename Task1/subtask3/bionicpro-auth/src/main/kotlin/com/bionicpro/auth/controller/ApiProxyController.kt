package com.bionicpro.auth.controller

import com.bionicpro.auth.service.TokenService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@RestController
@RequestMapping("/api")
class ApiProxyController(
    private val webClient: WebClient,
    private val tokenService: TokenService,
    @Value("\${api.service-url}") private val apiServiceUrl: String
) {

    @RequestMapping("/**")
    fun proxy(request: HttpServletRequest): ResponseEntity<ByteArray> {
        val accessToken = request.getAttribute("access_token") as? String
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token not found")

        val sessionId = request.getAttribute("new_session_id") as? String
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session not found")

        val uri = UriComponentsBuilder.fromHttpUrl(apiServiceUrl)
            .path(request.requestURI.substring("/api".length))
            .query(request.queryString)
            .build(true)
            .toUri()

        val method = HttpMethod.valueOf(request.method)
        val body: ByteArray? = if (method in listOf(HttpMethod.GET, HttpMethod.HEAD)) {
            null
        } else {
            request.inputStream.readAllBytes()
        }

        var currentToken = accessToken
        var response = executeRequest(method, uri, body, currentToken)

        if (response.statusCode == HttpStatus.UNAUTHORIZED) {
            val tokens = tokenService.getTokens(sessionId)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tokens not found")

            val refreshed = tokenService.refreshAccessToken(tokens.second)
            val newAccessToken = refreshed["access_token"] as? String
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No access token in refresh response")
            val newRefreshToken = refreshed["refresh_token"] as? String

            tokenService.updateAccessToken(sessionId, newAccessToken, newRefreshToken)

            currentToken = newAccessToken
            response = executeRequest(method, uri, body, currentToken)
        }

        if (response.statusCode == HttpStatus.UNAUTHORIZED) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Downstream service returned 401 after refresh")
        }

        return response
    }

    private fun executeRequest(
        method: HttpMethod,
        uri: URI,
        body: ByteArray?,
        accessToken: String
    ): ResponseEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        headers.contentType = MediaType.APPLICATION_JSON

        return try {
            webClient.method(method)
                .uri(uri)
                .headers { httpHeaders -> httpHeaders.addAll(headers) }
                .bodyValue(body ?: ByteArray(0))
                .exchangeToMono { clientResponse ->
                    clientResponse.bodyToMono(ByteArray::class.java).map { bytes ->
                        ResponseEntity
                            .status(clientResponse.statusCode())
                            .headers(clientResponse.headers().asHttpHeaders())
                            .body(bytes)
                    }
                }
                .block() ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Proxy error: ${e.message}")
        }
    }
}
