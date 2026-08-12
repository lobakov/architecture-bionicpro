package com.bionicpro.reports.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwkSetUri = "http://keycloak:8080/realms/reports-realm/protocol/openid-connect/certs"
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http.authorizeHttpRequests { auth ->
                auth.requestMatchers("/reports/**").authenticated()
                auth.anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }.build()

    private fun jwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> = JwtAuthenticationConverter().apply {
        setPrincipalClaimName("email")
    }
}
