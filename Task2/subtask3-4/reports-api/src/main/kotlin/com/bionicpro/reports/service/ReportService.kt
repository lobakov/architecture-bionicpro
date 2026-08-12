package com.bionicpro.reports.service

import com.bionicpro.reports.model.ReportDto
import com.bionicpro.reports.repository.ReportRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReportService(private val reportRepository: ReportRepository) {

    fun getReportsForCurrentUser(
        email: String,
        from: LocalDate?,
        to: LocalDate?
    ): List<ReportDto> = reportRepository.findReportsByEmail(email, from, to)
}
