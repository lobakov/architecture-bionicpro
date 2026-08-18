package com.bionicpro.reports.controller

import com.bionicpro.reports.model.ReportDto
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

    @GetMapping
    fun getReport(
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): ResponseEntity<List<ReportDto>> {
        val email = authentication.name
        val reports = reportService.getReportsForCurrentUser(email, from, to)
        return ResponseEntity.ok(mapOf(
            "reportUrl" to url,
            "generated" to generated.toString()
        ))
    }
}
