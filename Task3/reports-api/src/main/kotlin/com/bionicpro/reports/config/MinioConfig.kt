package com.bionicpro.reports.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig {

    @Value("\${minio.endpoint}")
    lateinit var endpoint: String

    @Value("\${minio.access-key}")
    lateinit var accessKey: String

    @Value("\${minio.secret-key}")
    lateinit var secretKey: String

    @Bean
    fun minioClient() = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()
}
