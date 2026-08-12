package com.bionicpro.reports.service

import com.bionicpro.reports.dto.ReportDto
import com.fasterxml.jackson.databind.ObjectMapper
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.StatObjectArgs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ReportCacheService(
    private val minioClient: MinioClient,
    private val objectMapper: ObjectMapper,

    @Value("\${minio.bucket}")
    private val bucket: String,

    @Value("\${minio.cdn-base-url}")
    private val cdnBaseUrl: String
) {

    fun findExistingReport(userId: Long, from: LocalDate?, to: LocalDate?): String? {
        val key = buildReportKey(userId, from, to)
        return try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(key)
                    .build()
            )
            key
        } catch (e: Exception) {
            null
        }
    }

    fun saveReport(userId: Long, from: LocalDate?, to: LocalDate?, reportData: List<ReportDto>): String {
        val json = objectMapper.writeValueAsString(reportData)
        val key = buildReportKey(userId, from, to)
        val stream = ByteArrayInputStream(json.toByteArray())
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .stream(stream, stream.available().toLong(), -1)
                .contentType("application/json")
                .build()
        )
        return key
    }

    fun getCdnUrl(key: String): String {
        return "$cdnBaseUrl/$key"
    }

    private fun buildReportKey(userId: Long, from: LocalDate?, to: LocalDate?): String {
        val datePart = "${from ?: "all"}_${to ?: "all"}"
        return "reports/$userId/${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}/report_${datePart}.json"
    }
}
