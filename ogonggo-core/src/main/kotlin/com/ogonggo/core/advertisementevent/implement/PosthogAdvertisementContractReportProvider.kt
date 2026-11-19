package com.ogonggo.core.advertisementevent.implement

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventAudienceType
import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportDailyDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportJobRoleDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportPositionDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.job.domain.JobRole
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

/** PostHog Endpoints의 공통 rows/columns envelope. endpoint 별 row schema는 mapper가 별도로 검사한다. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PosthogEndpointsResponse(
    val results: List<List<JsonNode>>,
    val columns: List<String>,
    @JsonProperty("execution_id") val executionId: String? = null,
)

data class PosthogEndpointsRequest(
    val variables: PosthogAdvertisementReportVariables,
    val limit: Int = 1_000,
    val offset: Int = 0,
)

data class PosthogAdvertisementReportVariables(
    val from: String,
    val to: String,
    /** 계약 소유권 검사를 통과한 DB 노출 ID만 쉼표로 묶어 전달한다. */
    @JsonProperty("exposure_ids") val exposureIds: String,
)

class PosthogAdvertisementContractReportProvider(
    private val endpointsClient: PosthogEndpointsClient,
    private val mapper: PosthogAdvertisementReportEndpointMapper,
) : AdvertisementContractReportProvider {

    override fun read(
        contractId: Long,
        from: LocalDate,
        to: LocalDate,
        exposureIds: List<Long>,
    ): AdvertisementContractReportDto {
        if (exposureIds.isEmpty()) return mapper.emptyReport(contractId, from, to)
        val response = endpointsClient.run(
            PosthogEndpointsRequest(
                variables = PosthogAdvertisementReportVariables(
                    from = from.toString(),
                    to = to.toString(),
                    exposureIds = exposureIds.distinct().sorted().joinToString(","),
                ),
            ),
        )
        return mapper.report(response, contractId, from, to)
    }
}

/**
 * Version 1 endpoint row contract. The exact columns are supplied by the PostHog project endpoint SQL
 * documented alongside the integration guide. Rows are typed by record_type; unsupported fields or values fail closed.
 */
class PosthogAdvertisementReportEndpointMapper {
    fun emptyReport(contractId: Long, from: LocalDate, to: LocalDate): AdvertisementContractReportDto =
        AdvertisementContractReportDto(
            contractId = contractId,
            from = from,
            to = to,
            summary = AdvertisementReportSummaryDto(ZERO, ZERO),
            daily = emptyList(),
            positions = emptyList(),
            jobRoleDistribution = emptyList(),
        )

    fun report(
        response: PosthogEndpointsResponse,
        contractId: Long,
        from: LocalDate,
        to: LocalDate,
    ): AdvertisementContractReportDto = safely {
        require(response.columns.size == EXPECTED_COLUMNS.size)
        require(response.columns.toSet() == EXPECTED_COLUMNS.toSet())
        require(response.columns.distinct().size == response.columns.size)
        val indexes = response.columns.withIndex().associate { it.value to it.index }
        val summary = mutableMapOf<AdvertisementEventType, AdvertisementReportMetricsDto>()
        val daily = mutableMapOf<DailyKey, MutableMap<AdvertisementEventType, AdvertisementReportMetricsDto>>()
        val positions = mutableMapOf<PositionKey, AdvertisementReportMetricsDto>()
        val jobRoles = mutableMapOf<String, Long>()

        response.results.forEach { row ->
            require(row.size == response.columns.size)
            val values = indexes.mapValues { (_, index) -> row[index] }
            val type = requiredText(values, "record_type")
            when (type) {
                "SUMMARY" -> {
                    requireNull(values, setOf("event_date", "category", "position", "job_role_code", "unique_clickers"))
                    val eventType = enumValue<AdvertisementEventType>(requiredText(values, "event_type"))
                    require(summary.put(eventType, metrics(values)) == null)
                }
                "DAILY" -> {
                    requireNull(values, setOf("job_role_code", "unique_clickers"))
                    val date = LocalDate.parse(requiredText(values, "event_date"))
                    require(!date.isBefore(from) && !date.isAfter(to))
                    val key = DailyKey(
                        date,
                        enumValue(requiredText(values, "category")),
                        nullableText(values, "position")?.let { enumValue<AdvertisementProductPosition>(it) },
                    )
                    val eventType = enumValue<AdvertisementEventType>(requiredText(values, "event_type"))
                    require(daily.getOrPut(key) { mutableMapOf() }.put(eventType, metrics(values)) == null)
                }
                "POSITION" -> {
                    requireNull(values, setOf("event_date", "event_type", "job_role_code", "unique_clickers"))
                    val key = PositionKey(
                        enumValue(requiredText(values, "category")),
                        nullableText(values, "position")?.let { enumValue<AdvertisementProductPosition>(it) },
                    )
                    require(positions.put(key, metrics(values)) == null)
                }
                "JOB_ROLE" -> {
                    requireNull(values, setOf("event_date", "category", "position", "event_type", "total", "unique_users"))
                    val key = requiredText(values, "job_role_code")
                    require(key == AdvertisementEventAudienceType.NON_MEMBER.name ||
                        key == AdvertisementEventAudienceType.UNSPECIFIED.name || JobRole.entries.any { it.name == key })
                    val count = requiredLong(values, "unique_clickers")
                    require(count > 0)
                    require(jobRoles.put(key, count) == null)
                }
                else -> throw IllegalArgumentException("unknown record type")
            }
        }

        if (response.results.isNotEmpty()) {
            require(summary.keys == AdvertisementEventType.entries.toSet())
        }
        val impressions = summary[AdvertisementEventType.IMPRESSION] ?: ZERO
        val clicks = summary[AdvertisementEventType.CLICK] ?: ZERO
        val dailyReports = daily.entries.sortedWith(
            compareBy({ it.key.date }, { it.key.category.ordinal }, { it.key.position?.ordinal ?: -1 }),
        ).map { (key, metricsByType) ->
            AdvertisementReportDailyDto(
                date = key.date,
                category = key.category,
                position = key.position,
                impressions = metricsByType[AdvertisementEventType.IMPRESSION] ?: ZERO,
                clicks = metricsByType[AdvertisementEventType.CLICK] ?: ZERO,
            )
        }
        val positionReports = positions.entries.sortedWith(
            compareBy({ it.key.category.ordinal }, { it.key.position?.ordinal ?: -1 }),
        ).map { (key, value) -> AdvertisementReportPositionDto(key.category, key.position, value) }
        val roleReports = jobRoles.entries.sortedBy { (key, _) ->
            JobRole.entries.indexOfFirst { it.name == key }.let { if (it < 0) Int.MAX_VALUE else it }
        }.map { (key, count) ->
            when (key) {
                AdvertisementEventAudienceType.NON_MEMBER.name -> AdvertisementReportJobRoleDto(key, "비회원", count)
                AdvertisementEventAudienceType.UNSPECIFIED.name -> AdvertisementReportJobRoleDto(key, "희망 직무 미설정", count)
                else -> {
                    val role = JobRole.valueOf(key)
                    AdvertisementReportJobRoleDto(role.name, role.desc, count)
                }
            }
        }
        AdvertisementContractReportDto(
            contractId = contractId,
            from = from,
            to = to,
            summary = AdvertisementReportSummaryDto(impressions, clicks),
            daily = dailyReports,
            positions = positionReports,
            jobRoleDistribution = roleReports,
        )
    }

    private fun metrics(values: Map<String, JsonNode>): AdvertisementReportMetricsDto {
        val total = requiredLong(values, "total")
        val unique = requiredLong(values, "unique_users")
        require(unique <= total)
        return AdvertisementReportMetricsDto(total, unique, if (unique == 0L) 0.0 else total.toDouble() / unique)
    }

    private fun requiredText(values: Map<String, JsonNode>, name: String): String {
        val value = values.getValue(name)
        require(value.isTextual && value.textValue().isNotBlank())
        return value.textValue()
    }

    private fun nullableText(values: Map<String, JsonNode>, name: String): String? {
        val value = values.getValue(name)
        require(value.isNull || value.isTextual)
        return value.takeUnless(JsonNode::isNull)?.textValue()
    }

    private fun requiredLong(values: Map<String, JsonNode>, name: String): Long {
        val value = values.getValue(name)
        require(value.isIntegralNumber && value.canConvertToLong())
        return value.longValue().also { require(it >= 0) }
    }

    private fun requireNull(values: Map<String, JsonNode>, names: Set<String>) {
        names.forEach { require(values.getValue(it).isNull) }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValues<T>().first { it.name == value }

    private inline fun <T> safely(block: () -> T): T = try {
        block()
    } catch (_: Exception) {
        throw InternalServerException(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE)
    }

    private data class DailyKey(
        val date: LocalDate,
        val category: AdvertisementProductCategory,
        val position: AdvertisementProductPosition?,
    )

    private data class PositionKey(
        val category: AdvertisementProductCategory,
        val position: AdvertisementProductPosition?,
    )

    private companion object {
        val EXPECTED_COLUMNS = listOf(
            "record_type", "event_date", "category", "position", "event_type", "total", "unique_users",
            "job_role_code", "unique_clickers",
        )
        val ZERO = AdvertisementReportMetricsDto(total = 0, uniqueUsers = 0, perUser = 0.0)
    }
}
