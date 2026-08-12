package com.bionicpro.reports.controller

import com.bionicpro.reports.repository.ReportRepository
import org.slf4j.LoggerFactory
import com.bionicpro.reports.service.ReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/reports")
class ReportsController(private val reportService: ReportService) {

    private val log = LoggerFactory.getLogger(ReportRepository::class.java)

    @GetMapping
    fun getReport(
        authentication: Authentication,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ResponseEntity<Map<String, String>> {
        val email = authentication.name
        val (url, generated) = reportService.getReportForUser(email, from, to)
        log.info("Principal name: {}", authentication.name)
        return ResponseEntity.ok(
            mapOf(
                "reportUrl" to url,
                "generated" to generated.toString()
            )
        )
    }
}
