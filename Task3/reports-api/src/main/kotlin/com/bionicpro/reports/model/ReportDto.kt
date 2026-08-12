package com.bionicpro.reports.model

import java.time.LocalDate

data class ReportDto(
    val userId: Long,
    val eventDate: LocalDate,
    val totalActions: Long,
    val avgSignalStrength: Double,
    val maxSignalStrength: Double,
    val totalDuration: Double,
    val fullName: String,
    val email: String,
    val prosthesisModel: String
)
