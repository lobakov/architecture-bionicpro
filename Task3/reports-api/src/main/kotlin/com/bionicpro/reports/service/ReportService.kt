package com.bionicpro.reports.service

import com.bionicpro.reports.dto.ReportDto
import com.bionicpro.reports.repository.ReportRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val cacheService: ReportCacheService
) {

    fun getReportsForUser(email: String, from: LocalDate?, to: LocalDate?): Pair<String, Boolean> {
        val userId = reportRepository.findUserIdByEmail(email) ?: throw IllegalArgumentException("User not found")

        val existingKey = cacheService.findExistingReport(userId, from, to)
        if (existingKey != null) {
            val url = cacheService.getCdnUrl(existingKey)
            return Pair(url, false)
        }

        val reportData = reportRepository.findReportsByEmail(email, from, to)
        val newKey = cacheService.saveReport(userId, from, to, reportData)
        val url = cacheService.getCdnUrl(newKey)

        return Pair(url, true)
    }
}
