package com.bionicpro.reports.repository

import com.bionicpro.reports.model.ReportDto
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate

@Repository
class ReportRepository(private val jdbcTemplate: JdbcTemplate) {

    private val log = LoggerFactory.getLogger(ReportRepository::class.java)

    fun findReportsByEmail(email: String, from: LocalDate?, to: LocalDate?): List<ReportDto> {
        val userId = findUserIdByEmail(email) ?: return emptyList()
        log.info("Found userId: {} for email: {}", userId, email)

        val sql = StringBuilder("""
            SELECT
                user_id,
                event_date,
                total_actions,
                sum_signal_strength / count_signal_strength AS avg_signal_strength,
                max_signal_strength,
                total_duration,
                full_name,
                email,
                prosthesis_model
            FROM reports.user_report_mart_cdc FINAL
            WHERE user_id = ?
        """.trimIndent())
        val params = mutableListOf<Any>(userId)

        if (from != null) {
            sql.append(" AND event_date >= ?")
            params.add(from)
        }
        if (to != null) {
            sql.append(" AND event_date <= ?")
            params.add(to)
        }
        sql.append(" ORDER BY event_date DESC")

        return try {
            jdbcTemplate.query(sql.toString(), params.toTypedArray()) { rs, _ ->
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
        } catch (e: Exception) {
            log.error("Error fetching reports for userId {}: {}", userId, e.message, e)
            throw e
        }
    }

    fun findUserIdByEmail(email: String): Long? {
        val sql = "SELECT user_id FROM reports.clients WHERE email = ? LIMIT 1"
        return try {
            val users = jdbcTemplate.query(sql, { rs, _ -> rs.getLong("user_id") }, email)
            if (users.isEmpty()) null else users.first()
        } catch (e: Exception) {
            log.error("Error finding user by email {}: {}", email, e.message, e)
            null
        }
    }
}
