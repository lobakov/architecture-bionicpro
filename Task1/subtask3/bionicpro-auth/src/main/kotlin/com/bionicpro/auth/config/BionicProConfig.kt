package com.bionicpro.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class BionicProConfig {

    @Value("\${redis.host}")
    private lateinit var redisHost: String

    @Value("\${redis.port}")
    private var redisPort: Int = 6379

    @Value("\${app.encryption.key}")
    private lateinit var encryptionKey: String

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration(redisHost, redisPort)
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        return template
    }

    @Bean
    fun textEncryptor(): TextEncryptor = Encryptors.text(encryptionKey, "deadbeef")

    @Bean
    fun webClient(): WebClient = WebClient.builder().build()
}
