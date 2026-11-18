package com.ogonggo.adminapi.advertisementreport.presentation.response

import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportDailyDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportJobRoleDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportPositionDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import java.time.LocalDate

data class AdminAdvertisementContractReportResponse(
    val contractId: Long,
    val from: LocalDate,
    val to: LocalDate,
    val summary: AdminAdvertisementReportSummaryResponse,
    val daily: List<AdminAdvertisementReportDailyResponse>,
    val positions: List<AdminAdvertisementReportPositionResponse>,
    val jobRoleDistribution: List<AdminAdvertisementReportJobRoleResponse>,
) {
    companion object {
        fun from(result: AdvertisementContractReportDto) = AdminAdvertisementContractReportResponse(
            contractId = result.contractId,
            from = result.from,
            to = result.to,
            summary = AdminAdvertisementReportSummaryResponse.from(result.summary),
            daily = result.daily.map(AdminAdvertisementReportDailyResponse::from),
            positions = result.positions.map(AdminAdvertisementReportPositionResponse::from),
            jobRoleDistribution = result.jobRoleDistribution.map(AdminAdvertisementReportJobRoleResponse::from),
        )
    }
}

data class AdminAdvertisementReportMetricsResponse(
    val total: Long,
    val uniqueUsers: Long,
    val perUser: Double,
) {
    companion object {
        fun from(result: AdvertisementReportMetricsDto) = AdminAdvertisementReportMetricsResponse(
            total = result.total,
            uniqueUsers = result.uniqueUsers,
            perUser = result.perUser,
        )
    }
}

data class AdminAdvertisementReportSummaryResponse(
    val impressions: AdminAdvertisementReportMetricsResponse,
    val clicks: AdminAdvertisementReportMetricsResponse,
) {
    companion object {
        fun from(result: AdvertisementReportSummaryDto) = AdminAdvertisementReportSummaryResponse(
            impressions = AdminAdvertisementReportMetricsResponse.from(result.impressions),
            clicks = AdminAdvertisementReportMetricsResponse.from(result.clicks),
        )
    }
}

data class AdminAdvertisementReportDailyResponse(
    val date: LocalDate,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val impressions: AdminAdvertisementReportMetricsResponse,
    val clicks: AdminAdvertisementReportMetricsResponse,
) {
    companion object {
        fun from(result: AdvertisementReportDailyDto) = AdminAdvertisementReportDailyResponse(
            date = result.date,
            category = result.category,
            position = result.position,
            impressions = AdminAdvertisementReportMetricsResponse.from(result.impressions),
            clicks = AdminAdvertisementReportMetricsResponse.from(result.clicks),
        )
    }
}

data class AdminAdvertisementReportPositionResponse(
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val clicks: AdminAdvertisementReportMetricsResponse,
) {
    companion object {
        fun from(result: AdvertisementReportPositionDto) = AdminAdvertisementReportPositionResponse(
            category = result.category,
            position = result.position,
            clicks = AdminAdvertisementReportMetricsResponse.from(result.clicks),
        )
    }
}

data class AdminAdvertisementReportJobRoleResponse(
    val key: String,
    val label: String,
    val uniqueClickers: Long,
) {
    companion object {
        fun from(result: AdvertisementReportJobRoleDto) = AdminAdvertisementReportJobRoleResponse(
            key = result.key,
            label = result.label,
            uniqueClickers = result.uniqueClickers,
        )
    }
}
