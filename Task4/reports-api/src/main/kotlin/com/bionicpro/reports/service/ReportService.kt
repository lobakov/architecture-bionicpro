package com.bionicpro.reports.service

import com.bionicpro.reports.repository.ReportRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val cacheService: ReportCacheService
) {

    fun getReportForUser(email: String, from: LocalDate?, to: LocalDate?): Pair<String, Boolean> {
        val userId = reportRepository.findUserIdByEmail(email)
            ?: throw IllegalArgumentException("User not found for email: $email")

        val existingKey = cacheService.findExistingReport(userId, from, to)
        if (existingKey != null) {
            return Pair(cacheService.getCdnUrl(existingKey), false)
        }

        val reportData = reportRepository.findReportsByEmail(email, from, to)

        val newKey = cacheService.saveReport(userId, from, to, reportData)

        return Pair(cacheService.getCdnUrl(newKey), true)
    }
}
