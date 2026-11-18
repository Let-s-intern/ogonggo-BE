package com.ogonggo.userapi.advertisementevent.presentation.response

import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportDailyDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportJobRoleDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportPositionDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import java.time.LocalDate

data class AdvertisementContractReportResponse(
    val contractId: Long,
    val from: LocalDate,
    val to: LocalDate,
    val summary: AdvertisementReportSummaryResponse,
    val daily: List<AdvertisementReportDailyResponse>,
    val positions: List<AdvertisementReportPositionResponse>,
    val jobRoleDistribution: List<AdvertisementReportJobRoleResponse>,
) {
    companion object {
        fun from(result: AdvertisementContractReportDto) = AdvertisementContractReportResponse(
            contractId = result.contractId,
            from = result.from,
            to = result.to,
            summary = AdvertisementReportSummaryResponse.from(result.summary),
            daily = result.daily.map(AdvertisementReportDailyResponse::from),
            positions = result.positions.map(AdvertisementReportPositionResponse::from),
            jobRoleDistribution = result.jobRoleDistribution.map(AdvertisementReportJobRoleResponse::from),
        )
    }
}

data class AdvertisementReportMetricsResponse(
    val total: Long,
    val uniqueUsers: Long,
    val perUser: Double,
) {
    companion object {
        fun from(result: AdvertisementReportMetricsDto) = AdvertisementReportMetricsResponse(
            total = result.total,
            uniqueUsers = result.uniqueUsers,
            perUser = result.perUser,
        )
    }
}

data class AdvertisementReportSummaryResponse(
    val impressions: AdvertisementReportMetricsResponse,
    val clicks: AdvertisementReportMetricsResponse,
) {
    companion object {
        fun from(result: AdvertisementReportSummaryDto) = AdvertisementReportSummaryResponse(
            impressions = AdvertisementReportMetricsResponse.from(result.impressions),
            clicks = AdvertisementReportMetricsResponse.from(result.clicks),
        )
    }
}

data class AdvertisementReportDailyResponse(
    val date: LocalDate,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val impressions: AdvertisementReportMetricsResponse,
    val clicks: AdvertisementReportMetricsResponse,
) {
    companion object {
        fun from(result: AdvertisementReportDailyDto) = AdvertisementReportDailyResponse(
            date = result.date,
            category = result.category,
            position = result.position,
            impressions = AdvertisementReportMetricsResponse.from(result.impressions),
            clicks = AdvertisementReportMetricsResponse.from(result.clicks),
        )
    }
}

data class AdvertisementReportPositionResponse(
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val clicks: AdvertisementReportMetricsResponse,
) {
    companion object {
        fun from(result: AdvertisementReportPositionDto) = AdvertisementReportPositionResponse(
            category = result.category,
            position = result.position,
            clicks = AdvertisementReportMetricsResponse.from(result.clicks),
        )
    }
}

data class AdvertisementReportJobRoleResponse(
    val key: String,
    val label: String,
    val uniqueClickers: Long,
) {
    companion object {
        fun from(result: AdvertisementReportJobRoleDto) = AdvertisementReportJobRoleResponse(
            key = result.key,
            label = result.label,
            uniqueClickers = result.uniqueClickers,
        )
    }
}
