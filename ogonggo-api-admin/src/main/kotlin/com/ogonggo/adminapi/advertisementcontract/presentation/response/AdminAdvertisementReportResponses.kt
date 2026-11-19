package com.ogonggo.adminapi.advertisementcontract.presentation.response

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportExposureDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportScrollStepDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSourceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSummaryDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetTotalDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.contentevent.domain.ContentEventSource
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class AdminAdvertisementReportResponse(
    val product: AdminAdvertisementReportProductResponse,
    @field:Schema(description = "조회 시작일. 상품 기간 안으로 자른 값")
    val from: LocalDate,
    @field:Schema(description = "조회 종료일(포함). 상품 기간 안으로 자르고 진행 중이면 오늘까지")
    val to: LocalDate,
    @field:Schema(description = "이벤트 수집을 시작한 날. from보다 늦으면 그 앞은 집계 전")
    val collectedSince: LocalDate,
    @field:Schema(description = "기간이 원본 보관 기간(365일)보다 앞이라 순 방문자·스크롤 깔때기를 날마다 센 값의 합으로 대신했으면 true")
    val uniqueCountsApproximate: Boolean,
    @field:Schema(description = "상품 몫. 이 상품의 노출로 들어온 이벤트만 센다")
    val summary: AdminAdvertisementReportSummaryResponse,
    @field:Schema(description = "from~to의 모든 날짜. 이벤트가 없는 날은 0")
    val daily: List<AdminAdvertisementReportDailyResponse>,
    @field:Schema(description = "이 상품의 노출 소재별 숫자")
    val exposures: List<AdminAdvertisementReportExposureResponse>,
    @field:Schema(description = "계약 대상 공고·부트캠프의 전체 숫자(유입 경로 무관)와 경로별 나눔. 대상이 없으면 빈 배열")
    val targets: List<AdminAdvertisementReportTargetResponse>,
) {
    companion object {
        fun from(report: AdvertisementContractReportDto) = AdminAdvertisementReportResponse(
            product = AdminAdvertisementReportProductResponse.from(report.product),
            from = report.from,
            to = report.to,
            collectedSince = report.collectedSince,
            uniqueCountsApproximate = report.uniqueCountsApproximate,
            summary = AdminAdvertisementReportSummaryResponse.from(report.summary),
            daily = report.daily.map(AdminAdvertisementReportDailyResponse::from),
            exposures = report.exposures.map(AdminAdvertisementReportExposureResponse::from),
            targets = report.targets.map(AdminAdvertisementReportTargetResponse::from),
        )
    }
}

data class AdminAdvertisementReportProductResponse(
    val contractProductId: Long,
    val productId: Long,
    val category: AdvertisementProductCategory,
    @field:Schema(description = "노출 자리. 자리가 없는 상품은 null", nullable = true)
    val position: AdvertisementProductPosition?,
    @field:Schema(description = "조회 시점의 상품 이름")
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    companion object {
        fun from(product: AdvertisementContractReportProductDto) = AdminAdvertisementReportProductResponse(
            contractProductId = product.contractProductId,
            productId = product.productId,
            category = product.category,
            position = product.position,
            name = product.name,
            startDate = product.startDate,
            endDate = product.endDate,
        )
    }
}

data class AdminAdvertisementReportSummaryResponse(
    @field:Schema(description = "광고 노출수. 보일 때마다 센다")
    val impressions: Long,
    @field:Schema(description = "광고 소재 클릭수")
    val adClicks: Long,
    @field:Schema(description = "소재 클릭률(adClicks / impressions, 0~1). 노출이 없으면 null", nullable = true)
    val adClickRate: Double?,
    @field:Schema(description = "상세 조회수. 같은 방문자가 30분 안에 다시 연 것은 한 번")
    val views: Long,
    @field:Schema(description = "상세를 연 순 방문자 수. 회원은 회원 id, 비회원은 익명 방문자 id로 센다")
    val uniqueVisitors: Long,
    val memberViews: Long,
    val memberUniqueVisitors: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    val ctaUniqueMembers: Long,
    @field:Schema(description = "CTA 클릭률(ctaUniqueVisitors / uniqueVisitors, 0~1). 방문자가 없으면 null", nullable = true)
    val ctaClickRate: Double?,
    @field:Schema(description = "평균 스크롤 깊이(%). 배너 상품이거나 스크롤 기록이 없으면 null", nullable = true)
    val avgScrollPercent: Double?,
    @field:ArraySchema(
        arraySchema = Schema(
            description = "스크롤 깊이 깔때기(0·25·50·75·100%). 방문자마다 가장 깊이 내린 값으로 센다. 배너 상품은 null",
            nullable = true,
        ),
    )
    val scrollFunnel: List<AdminAdvertisementReportScrollStepResponse>?,
    @field:Schema(description = "평균 체류 시간(초). 배너 상품이거나 기록이 없으면 null", nullable = true)
    val avgDurationSeconds: Double?,
) {
    companion object {
        fun from(summary: AdvertisementContractReportSummaryDto) = AdminAdvertisementReportSummaryResponse(
            impressions = summary.impressions,
            adClicks = summary.adClicks,
            adClickRate = summary.adClickRate,
            views = summary.views,
            uniqueVisitors = summary.uniqueVisitors,
            memberViews = summary.memberViews,
            memberUniqueVisitors = summary.memberUniqueVisitors,
            ctaClicks = summary.ctaClicks,
            ctaUniqueVisitors = summary.ctaUniqueVisitors,
            ctaUniqueMembers = summary.ctaUniqueMembers,
            ctaClickRate = summary.ctaClickRate,
            avgScrollPercent = summary.avgScrollPercent,
            scrollFunnel = summary.scrollFunnel?.map(AdminAdvertisementReportScrollStepResponse::from),
            avgDurationSeconds = summary.avgDurationSeconds,
        )
    }
}

data class AdminAdvertisementReportScrollStepResponse(
    @field:Schema(description = "깊이(%): 0, 25, 50, 75, 100. 0은 순 방문자 전체")
    val threshold: Int,
    val visitors: Long,
    @field:Schema(description = "순 방문자 대비 비율(0~1). 방문자가 없으면 null", nullable = true)
    val rate: Double?,
) {
    companion object {
        fun from(step: AdvertisementContractReportScrollStepDto) = AdminAdvertisementReportScrollStepResponse(step.threshold, step.visitors, step.rate)
    }
}

data class AdminAdvertisementReportDailyResponse(
    val date: LocalDate,
    val impressions: Long,
    val adClicks: Long,
    val views: Long,
    val uniqueVisitors: Long,
    val memberViews: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    @field:Schema(description = "그날 평균 스크롤 깊이(%). 기록이 없는 날은 0, 배너 상품은 null", nullable = true)
    val avgScrollPercent: Double?,
    @field:Schema(description = "그날 평균 체류 시간(초). 기록이 없는 날은 0, 배너 상품은 null", nullable = true)
    val avgDurationSeconds: Double?,
) {
    companion object {
        fun from(daily: AdvertisementContractReportDailyDto) = AdminAdvertisementReportDailyResponse(
            date = daily.date,
            impressions = daily.impressions,
            adClicks = daily.adClicks,
            views = daily.views,
            uniqueVisitors = daily.uniqueVisitors,
            memberViews = daily.memberViews,
            ctaClicks = daily.ctaClicks,
            ctaUniqueVisitors = daily.ctaUniqueVisitors,
            avgScrollPercent = daily.avgScrollPercent,
            avgDurationSeconds = daily.avgDurationSeconds,
        )
    }
}

data class AdminAdvertisementReportExposureResponse(
    val exposureId: Long,
    @field:Schema(description = "배너는 소재 이름, 광고 공고는 `<대상 제목> · <회사명>`")
    val name: String,
    val impressions: Long,
    val adClicks: Long,
    @field:Schema(description = "소재 클릭률(0~1). 노출이 없으면 null", nullable = true)
    val adClickRate: Double?,
) {
    companion object {
        fun from(exposure: AdvertisementContractReportExposureDto) = AdminAdvertisementReportExposureResponse(
            exposureId = exposure.exposureId,
            name = exposure.name,
            impressions = exposure.impressions,
            adClicks = exposure.adClicks,
            adClickRate = exposure.adClickRate,
        )
    }
}

data class AdminAdvertisementReportTargetResponse(
    val targetType: AdvertisementContractPromotionTarget,
    val targetId: Long,
    @field:Schema(description = "조회 시점의 공고·부트캠프 제목")
    val title: String,
    val total: AdminAdvertisementReportTargetTotalResponse,
    @field:Schema(description = "유입 경로별 조회·CTA 클릭. 경로 enum 순서로 모두 있고 없으면 0")
    val bySource: List<AdminAdvertisementReportSourceResponse>,
) {
    companion object {
        fun from(target: AdvertisementContractReportTargetDto) = AdminAdvertisementReportTargetResponse(
            targetType = target.targetType,
            targetId = target.targetId,
            title = target.title,
            total = AdminAdvertisementReportTargetTotalResponse.from(target.total),
            bySource = target.bySource.map(AdminAdvertisementReportSourceResponse::from),
        )
    }
}

data class AdminAdvertisementReportTargetTotalResponse(
    val views: Long,
    val uniqueVisitors: Long,
    val memberViews: Long,
    val ctaClicks: Long,
    val ctaUniqueVisitors: Long,
    @field:Schema(description = "평균 스크롤 깊이(%). 기록이 없으면 null", nullable = true)
    val avgScrollPercent: Double?,
    @field:Schema(description = "평균 체류 시간(초). 기록이 없으면 null", nullable = true)
    val avgDurationSeconds: Double?,
) {
    companion object {
        fun from(total: AdvertisementContractReportTargetTotalDto) = AdminAdvertisementReportTargetTotalResponse(
            views = total.views,
            uniqueVisitors = total.uniqueVisitors,
            memberViews = total.memberViews,
            ctaClicks = total.ctaClicks,
            ctaUniqueVisitors = total.ctaUniqueVisitors,
            avgScrollPercent = total.avgScrollPercent,
            avgDurationSeconds = total.avgDurationSeconds,
        )
    }
}

data class AdminAdvertisementReportSourceResponse(
    val source: ContentEventSource,
    val views: Long,
    val ctaClicks: Long,
) {
    companion object {
        fun from(source: AdvertisementContractReportSourceDto) = AdminAdvertisementReportSourceResponse(source.source, source.views, source.ctaClicks)
    }
}
