package com.ogonggo.core.advertisementevent.implement.dto

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import java.time.LocalDate

data class AdvertisementContractReportDto(
    val contractId: Long,
    val from: LocalDate,
    val to: LocalDate,
    val summary: AdvertisementReportSummaryDto,
    val daily: List<AdvertisementReportDailyDto>,
    val positions: List<AdvertisementReportPositionDto>,
    val jobRoleDistribution: List<AdvertisementReportJobRoleDto>,
)

data class AdvertisementReportMetricsDto(
    val total: Long,
    val uniqueUsers: Long,
    val perUser: Double,
)

data class AdvertisementReportSummaryDto(
    val impressions: AdvertisementReportMetricsDto,
    val clicks: AdvertisementReportMetricsDto,
)

data class AdvertisementReportDailyDto(
    val date: LocalDate,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val impressions: AdvertisementReportMetricsDto,
    val clicks: AdvertisementReportMetricsDto,
)

data class AdvertisementReportPositionDto(
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val clicks: AdvertisementReportMetricsDto,
)

data class AdvertisementReportJobRoleDto(
    val key: String,
    val label: String,
    val uniqueClickers: Long,
)
