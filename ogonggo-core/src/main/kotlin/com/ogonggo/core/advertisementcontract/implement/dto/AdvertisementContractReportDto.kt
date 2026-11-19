package com.ogonggo.core.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.contentevent.domain.ContentEventSource
import java.time.LocalDate

/**
 * 계약 상품 하나의 성과 리포트다. 사용자·관리자 API가 같은 숫자를 보이도록 계산은 core가 한다.
 *
 * - [summary]·[daily]·[exposures]·[csvRows]는 상품 몫이다. 이 상품의 노출 식별자로 들어온 이벤트만 센다.
 * - [targets]는 계약 대상 공고의 전체 숫자다. 유입 경로와 상관없이 세고 경로별로 나눈다.
 * - 배너 상품은 스크롤 깊이·체류 시간을 보이지 않아 그 값이 null이다.
 */
data class AdvertisementContractReportDto(
    val product: AdvertisementContractReportProductDto,
    val from: LocalDate,
    val to: LocalDate,
    val collectedSince: LocalDate,
    val uniqueCountsApproximate: Boolean,
    val summary: AdvertisementContractReportSummaryDto,
    val daily: List<AdvertisementContractReportDailyDto>,
    val exposures: List<AdvertisementContractReportExposureDto>,
    val targets: List<AdvertisementContractReportTargetDto>,
    val csvRows: List<AdvertisementContractReportCsvRowDto>,
)

data class AdvertisementContractReportProductDto(
    val contractId: Long,
    val contractProductId: Long,
    val productId: Long,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class AdvertisementContractReportSummaryDto(
    val impressions: Long,
    val adClicks: Long,
    val adClickRate: Double?,
    val views: Long,
    val uniqueVisitors: Long,
    val memberViews: Long,
    val memberUniqueVisitors: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    val ctaUniqueMembers: Long,
    val ctaClickRate: Double?,
    val avgScrollPercent: Double?,
    val scrollFunnel: List<AdvertisementContractReportScrollStepDto>?,
    val avgDurationSeconds: Double?,
)

/** 스크롤 깊이 깔때기 한 단계다. 0% 단계는 순 방문자 전체이고, 비율은 순 방문자 대비다(분모가 0이면 null). */
data class AdvertisementContractReportScrollStepDto(
    val threshold: Int,
    val visitors: Long,
    val rate: Double?,
)

/** 하루 숫자다. 기간의 모든 날이 있고 이벤트가 없는 날은 0이다. */
data class AdvertisementContractReportDailyDto(
    val date: LocalDate,
    val impressions: Long,
    val adClicks: Long,
    val views: Long,
    val uniqueVisitors: Long,
    val memberViews: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    val avgScrollPercent: Double?,
    val avgDurationSeconds: Double?,
)

data class AdvertisementContractReportExposureDto(
    val exposureId: Long,
    val name: String,
    val imageUrl: String?,
    val impressions: Long,
    val adClicks: Long,
    val adClickRate: Double?,
)

data class AdvertisementContractReportTargetDto(
    val targetType: AdvertisementContractPromotionTarget,
    val targetId: Long,
    val title: String,
    val total: AdvertisementContractReportTargetTotalDto,
    val bySource: List<AdvertisementContractReportSourceDto>,
)

data class AdvertisementContractReportTargetTotalDto(
    val views: Long,
    val uniqueVisitors: Long,
    val memberViews: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    val avgScrollPercent: Double?,
    val avgDurationSeconds: Double?,
)

data class AdvertisementContractReportSourceDto(
    val source: ContentEventSource,
    val views: Long,
    val ctaClicks: Long,
)

/** CSV 한 줄이다. 날짜별·소재별이며 기간의 모든 날 × 이 상품의 모든 노출이 있다. */
data class AdvertisementContractReportCsvRowDto(
    val date: LocalDate,
    val exposureId: Long,
    val exposureName: String,
    val impressions: Long,
    val adClicks: Long,
    val views: Long,
    val uniqueVisitors: Long,
    val memberViews: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    val avgScrollPercent: Double?,
    val avgDurationSeconds: Double?,
)
