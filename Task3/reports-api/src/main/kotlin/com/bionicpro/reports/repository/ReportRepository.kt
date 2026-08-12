package com.bionicpro.reports.repository

import com.bionicpro.reports.model.ReportDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate

@Repository
class ReportRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findReportsByEmail(email: String, from: LocalDate?, to: LocalDate?): List<ReportDto> {
        val sql = StringBuilder("""
            SELECT user_id, event_date, total_actions, avg_signal_strength, max_signal_strength,
                   total_duration, full_name, email, prosthesis_model
            FROM reports.user_report_mart
            WHERE email = ?
        """.trimIndent())

        val params = mutableListOf<Any>(email)

        if (from != null) {
            sql.append(" AND event_date >= ?")
            params.add(from)
        }
        if (to != null) {
            sql.append(" AND event_date <= ?")
            params.add(to)
        }

        sql.append(" ORDER BY event_date DESC")

        return jdbcTemplate.query(sql.toString(), params.toTypedArray()) { rs: ResultSet, _: Int ->
            ReportDto(
                userId = rs.getLong("user_id"),
                eventDate = rs.getDate("event_date").toLocalDate(),
                totalActions = rs.getLong("total_actions"),
                avgSignalStrength = rs.getDouble("avg_signal_strength"),
                maxSignalStrength = rs.getDouble("max_signal_strength"),
                totalDuration = rs.getDouble("total_duration"),
                fullName = rs.getString("full_name"),
                email = rs.getString("email"),
                prosthesisModel = rs.getString("prosthesis_model")
            )
        }
    }

    fun findUserIdByEmail(email: String): Long? {
        val sql = "SELECT user_id FROM reports.user_report_mart WHERE email = ? LIMIT 1"
        return try {
            jdbcTemplate.queryForObject(sql, Long::class.java, email)
        } catch (e: Exception) {
            null
        }
    }
}
