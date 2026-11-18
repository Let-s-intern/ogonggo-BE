package com.ogonggo.core.advertisementevent.implement

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventAudienceType
import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportDailyDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportJobRoleDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportPositionDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.job.domain.JobRole
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.LocalDate

@Component
class AdvertisementEventReportReader internal constructor(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun read(contractId: Long, from: LocalDate, to: LocalDate): AdvertisementContractReportDto {
        val impressionTotal = readTotal(contractId, from, to, AdvertisementEventType.IMPRESSION)
        val clickTotal = readTotal(contractId, from, to, AdvertisementEventType.CLICK)
        val uniqueCounts = readPeriodUniqueCounts(contractId, from, to)
        val dailyTotals = readDailyTotals(contractId, from, to)
        val dailyUnique = readDailyUniqueCounts(contractId, from, to)
        val positionTotals = readPositionClickTotals(contractId, from, to)
        val positionUnique = readPositionClickUniqueCounts(contractId, from, to)

        val daily = (dailyTotals.keys + dailyUnique.keys)
            .distinct()
            .sortedWith(compareBy<DailyKey>({ it.date }, { it.category.ordinal }, { it.position?.ordinal ?: -1 }))
            .map { key ->
                val totals = dailyTotals[key] ?: DailyTotals()
                val uniques = dailyUnique[key] ?: DailyUniqueCounts()
                AdvertisementReportDailyDto(
                    date = key.date,
                    category = key.category,
                    position = key.position,
                    impressions = metrics(totals.impressions, uniques.impressions),
                    clicks = metrics(totals.clicks, uniques.clicks),
                )
            }

        val placements = (positionTotals.keys + positionUnique.keys)
            .distinct()
            .sortedWith(compareBy<PositionKey>({ it.category.ordinal }, { it.position?.ordinal ?: -1 }))
            .map { key ->
                val total = positionTotals[key] ?: 0L
                val unique = positionUnique[key] ?: 0L
                AdvertisementReportPositionDto(key.category, key.position, metrics(total, unique))
            }

        return AdvertisementContractReportDto(
            contractId = contractId,
            from = from,
            to = to,
            summary = AdvertisementReportSummaryDto(
                impressions = metrics(impressionTotal, uniqueCounts[AdvertisementEventType.IMPRESSION] ?: 0L),
                clicks = metrics(clickTotal, uniqueCounts[AdvertisementEventType.CLICK] ?: 0L),
            ),
            daily = daily,
            positions = placements,
            jobRoleDistribution = readJobRoleDistribution(contractId, from, to),
        )
    }

    private fun readTotal(
        contractId: Long,
        from: LocalDate,
        to: LocalDate,
        eventType: AdvertisementEventType,
    ): Long = jdbcTemplate.queryForObject(
        """
            SELECT COALESCE(SUM(total_count), 0)
            FROM advertisement_daily_metrics
            WHERE contract_id = ? AND event_date BETWEEN ? AND ? AND event_type = ?
        """.trimIndent(),
        Long::class.java,
        contractId,
        from,
        to,
        eventType.name,
    ) ?: 0L

    private fun readPeriodUniqueCounts(contractId: Long, from: LocalDate, to: LocalDate): Map<AdvertisementEventType, Long> =
        jdbcTemplate.query(
            """
                SELECT event_type, COUNT(DISTINCT viewer_key) AS unique_count
                FROM advertisement_event_daily_viewers
                WHERE contract_id = ? AND event_date BETWEEN ? AND ?
                GROUP BY event_type
            """.trimIndent(),
            { rs, _ -> AdvertisementEventType.valueOf(rs.getString("event_type")) to rs.getLong("unique_count") },
            contractId,
            from,
            to,
        ).toMap()

    private fun readDailyTotals(contractId: Long, from: LocalDate, to: LocalDate): Map<DailyKey, DailyTotals> =
        jdbcTemplate.query(
            """
                SELECT event_date, category, position,
                    SUM(CASE WHEN event_type = 'IMPRESSION' THEN total_count ELSE 0 END) AS impressions,
                    SUM(CASE WHEN event_type = 'CLICK' THEN total_count ELSE 0 END) AS clicks
                FROM advertisement_daily_metrics
                WHERE contract_id = ? AND event_date BETWEEN ? AND ?
                GROUP BY event_date, category, position
            """.trimIndent(),
            { rs, _ ->
                val key = rs.toDailyKey()
                key to DailyTotals(rs.getLong("impressions"), rs.getLong("clicks"))
            },
            contractId,
            from,
            to,
        ).toMap()

    private fun readDailyUniqueCounts(contractId: Long, from: LocalDate, to: LocalDate): Map<DailyKey, DailyUniqueCounts> =
        jdbcTemplate.query(
            """
                SELECT event_date, category, position, event_type, COUNT(DISTINCT viewer_key) AS unique_count
                FROM advertisement_event_daily_viewers
                WHERE contract_id = ? AND event_date BETWEEN ? AND ?
                GROUP BY event_date, category, position, event_type
            """.trimIndent(),
            { rs, _ ->
                val key = rs.toDailyKey()
                key to (AdvertisementEventType.valueOf(rs.getString("event_type")) to rs.getLong("unique_count"))
            },
            contractId,
            from,
            to,
        ).groupBy({ it.first }, { it.second }).mapValues { (_, values) ->
            DailyUniqueCounts(
                impressions = values.firstOrNull { it.first == AdvertisementEventType.IMPRESSION }?.second ?: 0L,
                clicks = values.firstOrNull { it.first == AdvertisementEventType.CLICK }?.second ?: 0L,
            )
        }

    private fun readPositionClickTotals(contractId: Long, from: LocalDate, to: LocalDate): Map<PositionKey, Long> =
        jdbcTemplate.query(
            """
                SELECT category, position, SUM(total_count) AS click_count
                FROM advertisement_daily_metrics
                WHERE contract_id = ? AND event_date BETWEEN ? AND ? AND event_type = 'CLICK'
                GROUP BY category, position
            """.trimIndent(),
            { rs, _ -> rs.toPositionKey() to rs.getLong("click_count") },
            contractId,
            from,
            to,
        ).toMap()

    private fun readPositionClickUniqueCounts(contractId: Long, from: LocalDate, to: LocalDate): Map<PositionKey, Long> =
        jdbcTemplate.query(
            """
                SELECT category, position, COUNT(DISTINCT viewer_key) AS unique_count
                FROM advertisement_event_daily_viewers
                WHERE contract_id = ? AND event_date BETWEEN ? AND ? AND event_type = 'CLICK'
                GROUP BY category, position
            """.trimIndent(),
            { rs, _ -> rs.toPositionKey() to rs.getLong("unique_count") },
            contractId,
            from,
            to,
        ).toMap()

    private fun readJobRoleDistribution(contractId: Long, from: LocalDate, to: LocalDate): List<AdvertisementReportJobRoleDto> {
        val clickers = jdbcTemplate.query(
            """
                SELECT viewer_key, audience_type, job_roles
                FROM advertisement_event_daily_viewers
                WHERE contract_id = ? AND event_date BETWEEN ? AND ? AND event_type = 'CLICK'
            """.trimIndent(),
            { rs, _ ->
                ClickerSnapshot(
                    viewerKey = rs.getString("viewer_key"),
                    audienceType = AdvertisementEventAudienceType.valueOf(rs.getString("audience_type")),
                    jobRoles = rs.getString("job_roles")
                        ?.split(",")
                        ?.filter(String::isNotBlank)
                        ?.map { JobRole.valueOf(it) }
                        .orEmpty()
                        .toSet(),
                )
            },
            contractId,
            from,
            to,
        )

        val clickersByRole = mutableMapOf<JobRole, MutableSet<String>>()
        val nonMembers = mutableSetOf<String>()
        val unspecifiedMembers = mutableSetOf<String>()
        clickers.forEach { clicker ->
            clicker.jobRoles.forEach { role ->
                clickersByRole.getOrPut(role) { mutableSetOf() }.add(clicker.viewerKey)
            }
            when (clicker.audienceType) {
                AdvertisementEventAudienceType.NON_MEMBER -> nonMembers.add(clicker.viewerKey)
                AdvertisementEventAudienceType.UNSPECIFIED -> unspecifiedMembers.add(clicker.viewerKey)
                AdvertisementEventAudienceType.MEMBER -> Unit
            }
        }

        val roles = clickersByRole.entries
            .sortedBy { it.key.ordinal }
            .map { (role, viewerKeys) -> AdvertisementReportJobRoleDto(role.name, role.desc, viewerKeys.size.toLong()) }
            .toMutableList()

        if (nonMembers.isNotEmpty()) {
            roles += AdvertisementReportJobRoleDto(AdvertisementEventAudienceType.NON_MEMBER.name, "비회원", nonMembers.size.toLong())
        }
        if (unspecifiedMembers.isNotEmpty()) {
            roles += AdvertisementReportJobRoleDto(
                AdvertisementEventAudienceType.UNSPECIFIED.name,
                "희망 직무 미설정",
                unspecifiedMembers.size.toLong(),
            )
        }
        return roles
    }

    private fun metrics(total: Long, unique: Long) = AdvertisementReportMetricsDto(
        total = total,
        uniqueUsers = unique,
        perUser = if (unique == 0L) 0.0 else total.toDouble() / unique,
    )

    private fun ResultSet.toDailyKey() = DailyKey(
        date = getObject("event_date", LocalDate::class.java),
        category = AdvertisementProductCategory.valueOf(getString("category")),
        position = getString("position")?.let(AdvertisementProductPosition::valueOf),
    )

    private fun ResultSet.toPositionKey() = PositionKey(
        category = AdvertisementProductCategory.valueOf(getString("category")),
        position = getString("position")?.let(AdvertisementProductPosition::valueOf),
    )

    private data class DailyKey(
        val date: LocalDate,
        val category: AdvertisementProductCategory,
        val position: AdvertisementProductPosition?,
    )

    private data class ClickerSnapshot(
        val viewerKey: String,
        val audienceType: AdvertisementEventAudienceType,
        val jobRoles: Set<JobRole>,
    )

    private data class PositionKey(
        val category: AdvertisementProductCategory,
        val position: AdvertisementProductPosition?,
    )

    private data class DailyTotals(val impressions: Long = 0, val clicks: Long = 0)

    private data class DailyUniqueCounts(val impressions: Long = 0, val clicks: Long = 0)
}
