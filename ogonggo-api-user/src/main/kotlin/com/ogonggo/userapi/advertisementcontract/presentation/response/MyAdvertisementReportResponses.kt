package com.ogonggo.userapi.advertisementcontract.presentation.response

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractActiveReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportExposureDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportAudienceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportGradeDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportJobFieldDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetMatchDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductSumDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportScrollStepDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSourceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSummaryDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalSumDto
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.user.domain.UserGrade
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class MyAdvertisementReportResponse(
    val product: MyAdvertisementReportProductResponse,
    @field:Schema(description = "조회 시작일. 상품 기간 안으로 자른 값")
    val from: LocalDate,
    @field:Schema(description = "조회 종료일(포함). 상품 기간 안으로 자르고 진행 중이면 오늘까지")
    val to: LocalDate,
    @field:Schema(description = "이벤트 수집을 시작한 날. from보다 늦으면 그 앞은 집계 전")
    val collectedSince: LocalDate,
    @field:Schema(description = "기간이 원본 보관 기간(365일)보다 앞이라 순 방문자·스크롤 깔때기를 날마다 센 값의 합으로 대신했으면 true")
    val uniqueCountsApproximate: Boolean,
    @field:Schema(description = "상품 몫. 이 상품의 노출로 들어온 이벤트만 센다")
    val summary: MyAdvertisementReportSummaryResponse,
    @field:Schema(description = "from~to의 모든 날짜. 이벤트가 없는 날은 0")
    val daily: List<MyAdvertisementReportDailyResponse>,
    @field:Schema(description = "이 상품의 노출 소재별 숫자")
    val exposures: List<MyAdvertisementReportExposureResponse>,
    @field:Schema(description = "계약 대상 공고·부트캠프의 전체 숫자(유입 경로 무관)와 경로별 나눔. 대상이 없으면 빈 배열")
    val targets: List<MyAdvertisementReportTargetResponse>,
    @field:Schema(
        description = "상품 몫의 로그인 회원을 희망 직군으로 나눈 것. 여러 직군을 고른 회원은 각 직군에 한 번씩, 직무가 없으면 jobField null. 비회원은 넣지 않는다. 순 회원 수가 큰 순",
    )
    val byJobField: List<MyAdvertisementReportJobFieldResponse>,
    @field:Schema(description = "상품 몫을 본 사람의 구성. 도달·평균 빈도, 타깃 직무 일치율, 학년")
    val audience: MyAdvertisementReportAudienceResponse,
    @field:Schema(description = "운영자가 써서 보낸 인사이트. 보내지 않은 임시 저장은 나오지 않는다. 없으면 빈 배열")
    val insights: List<MyAdvertisementReportInsightResponse>,
) {
    companion object {
        fun from(report: AdvertisementContractReportDto) = MyAdvertisementReportResponse(
            product = MyAdvertisementReportProductResponse.from(report.product),
            from = report.from,
            to = report.to,
            collectedSince = report.collectedSince,
            uniqueCountsApproximate = report.uniqueCountsApproximate,
            summary = MyAdvertisementReportSummaryResponse.from(report.summary),
            daily = report.daily.map(MyAdvertisementReportDailyResponse::from),
            exposures = report.exposures.map(MyAdvertisementReportExposureResponse::from),
            targets = report.targets.map(MyAdvertisementReportTargetResponse::from),
            byJobField = report.byJobField.map(MyAdvertisementReportJobFieldResponse::from),
            audience = MyAdvertisementReportAudienceResponse.from(report.audience),
            insights = report.insights.map(MyAdvertisementReportInsightResponse::from),
        )
    }
}

data class MyAdvertisementReportProductResponse(
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
        fun from(product: AdvertisementContractReportProductDto) = MyAdvertisementReportProductResponse(
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

data class MyAdvertisementReportSummaryResponse(
    @field:Schema(description = "광고 노출수. 보일 때마다 센다")
    val impressions: Long,
    @field:Schema(description = "순 노출수. 같은 방문자가 같은 소재를 하루(KST)에 여러 번 봐도 한 번으로 센다")
    val uniqueImpressions: Long,
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
    val scrollFunnel: List<MyAdvertisementReportScrollStepResponse>?,
    @field:Schema(description = "평균 체류 시간(초). 배너 상품이거나 기록이 없으면 null", nullable = true)
    val avgDurationSeconds: Double?,
) {
    companion object {
        fun from(summary: AdvertisementContractReportSummaryDto) = MyAdvertisementReportSummaryResponse(
            impressions = summary.impressions,
            uniqueImpressions = summary.uniqueImpressions,
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
            scrollFunnel = summary.scrollFunnel?.map(MyAdvertisementReportScrollStepResponse::from),
            avgDurationSeconds = summary.avgDurationSeconds,
        )
    }
}

data class MyAdvertisementReportScrollStepResponse(
    @field:Schema(description = "깊이(%): 0, 25, 50, 75, 100. 0은 순 방문자 전체")
    val threshold: Int,
    val visitors: Long,
    @field:Schema(description = "순 방문자 대비 비율(0~1). 방문자가 없으면 null", nullable = true)
    val rate: Double?,
) {
    companion object {
        fun from(step: AdvertisementContractReportScrollStepDto) = MyAdvertisementReportScrollStepResponse(step.threshold, step.visitors, step.rate)
    }
}

data class MyAdvertisementReportDailyResponse(
    val date: LocalDate,
    val impressions: Long,
    @field:Schema(description = "그날 순 노출수(같은 방문자·소재는 한 번)")
    val uniqueImpressions: Long,
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
        fun from(daily: AdvertisementContractReportDailyDto) = MyAdvertisementReportDailyResponse(
            date = daily.date,
            impressions = daily.impressions,
            uniqueImpressions = daily.uniqueImpressions,
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

data class MyAdvertisementReportExposureResponse(
    val exposureId: Long,
    @field:Schema(description = "배너는 소재 이름, 광고 공고는 `<대상 제목> · <회사명>`")
    val name: String,
    val impressions: Long,
    val adClicks: Long,
    @field:Schema(description = "소재 클릭률(0~1). 노출이 없으면 null", nullable = true)
    val adClickRate: Double?,
) {
    companion object {
        fun from(exposure: AdvertisementContractReportExposureDto) = MyAdvertisementReportExposureResponse(
            exposureId = exposure.exposureId,
            name = exposure.name,
            impressions = exposure.impressions,
            adClicks = exposure.adClicks,
            adClickRate = exposure.adClickRate,
        )
    }
}

data class MyAdvertisementReportTargetResponse(
    val targetType: AdvertisementContractPromotionTarget,
    val targetId: Long,
    @field:Schema(description = "조회 시점의 공고·부트캠프 제목")
    val title: String,
    val total: MyAdvertisementReportTargetTotalResponse,
    @field:Schema(description = "유입 경로별 조회·CTA 클릭. 경로 enum 순서로 모두 있고 없으면 0")
    val bySource: List<MyAdvertisementReportSourceResponse>,
) {
    companion object {
        fun from(target: AdvertisementContractReportTargetDto) = MyAdvertisementReportTargetResponse(
            targetType = target.targetType,
            targetId = target.targetId,
            title = target.title,
            total = MyAdvertisementReportTargetTotalResponse.from(target.total),
            bySource = target.bySource.map(MyAdvertisementReportSourceResponse::from),
        )
    }
}

data class MyAdvertisementReportTargetTotalResponse(
    @field:Schema(description = "기간 안에 저장했고 지금도 저장 중인 사람 수. 저장을 풀면 빠진다")
    val saves: Long,
    @field:Schema(description = "공유 수. 원본 보관 기간(365일)보다 앞인 날은 빠진다")
    val shares: Long,
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
        fun from(total: AdvertisementContractReportTargetTotalDto) = MyAdvertisementReportTargetTotalResponse(
            saves = total.saves,
            shares = total.shares,
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

data class MyAdvertisementReportSourceResponse(
    val source: ContentEventSource,
    val views: Long,
    val ctaClicks: Long,
) {
    companion object {
        fun from(source: AdvertisementContractReportSourceDto) = MyAdvertisementReportSourceResponse(source.source, source.views, source.ctaClicks)
    }
}

data class MyAdvertisementReportJobFieldResponse(
    @field:Schema(description = "희망 직군. 회원의 렛츠커리어 희망 직무를 오공고 직무로 옮긴 직군이며 옮길 수 없으면 null", nullable = true)
    val jobField: JobField?,
    @field:Schema(description = "직군 이름. null 칸은 \"직무 미설정\"")
    val label: String,
    @field:Schema(description = "상세를 연 순 회원 수")
    val memberUniqueVisitors: Long,
    @field:Schema(description = "회원 상세 조회수(30분 안 재조회는 한 번)")
    val memberViews: Long,
    @field:Schema(description = "CTA를 누른 순 회원 수")
    val ctaUniqueMembers: Long,
    @field:Schema(description = "광고 소재를 누른 순 회원 수")
    val adClickMembers: Long,
) {
    companion object {
        fun from(row: AdvertisementContractReportJobFieldDto) = MyAdvertisementReportJobFieldResponse(
            jobField = row.jobField,
            label = row.label,
            memberUniqueVisitors = row.memberUniqueVisitors,
            memberViews = row.memberViews,
            ctaUniqueMembers = row.ctaUniqueMembers,
            adClickMembers = row.adClickMembers,
        )
    }
}

data class MyAdvertisementReportInsightResponse(
    val title: String,
    val body: String,
) {
    companion object {
        fun from(insight: AdvertisementReportInsightDto) = MyAdvertisementReportInsightResponse(insight.title, insight.body)
    }
}

data class MyAdvertisementReportAudienceResponse(
    @field:Schema(description = "도달. 기간 동안 광고를 본 서로 다른 사람(회원·비회원 합). 기간이 원본 보관 기간보다 앞이면 null", nullable = true)
    val reach: Long?,
    @field:Schema(description = "평균 빈도. 노출수 ÷ 도달(소수 첫째 자리). 도달이 없거나 0이면 null", nullable = true)
    val avgFrequency: Double?,
    @field:Schema(description = "광고를 눌러 공고를 연 서로 다른 사람. 기간이 원본 보관 기간보다 앞이면 null", nullable = true)
    val adClickVisitors: Long?,
    @field:Schema(description = "타깃 직군. 광고가 띄우는 공고의 직군과 노출을 띄우는 직군 탭. 없으면 빈 배열")
    val targetJobFields: List<JobField>,
    val targetMatch: MyAdvertisementReportTargetMatchResponse,
    @field:Schema(description = "공고를 연 로그인 회원을 학년으로 나눈 것. 학년 순이고 학년 미설정이 맨 뒤")
    val byGrade: List<MyAdvertisementReportGradeResponse>,
) {
    companion object {
        fun from(audience: AdvertisementContractReportAudienceDto) = MyAdvertisementReportAudienceResponse(
            reach = audience.reach,
            avgFrequency = audience.avgFrequency,
            adClickVisitors = audience.adClickVisitors,
            targetJobFields = audience.targetJobFields,
            targetMatch = MyAdvertisementReportTargetMatchResponse.from(audience.targetMatch),
            byGrade = audience.byGrade.map(MyAdvertisementReportGradeResponse::from),
        )
    }
}

@Schema(description = "공고를 연 회원 중 희망 직군이 타깃 직군과 겹치는 회원")
data class MyAdvertisementReportTargetMatchResponse(
    val matchedMembers: Long,
    val members: Long,
    @field:Schema(description = "matchedMembers ÷ members(0~1). 타깃 직군이 없거나 회원이 없으면 null", nullable = true)
    val rate: Double?,
) {
    companion object {
        fun from(match: AdvertisementContractReportTargetMatchDto) = MyAdvertisementReportTargetMatchResponse(match.matchedMembers, match.members, match.rate)
    }
}

data class MyAdvertisementReportGradeResponse(
    @field:Schema(description = "학년. 입력하지 않은 회원은 null", nullable = true)
    val grade: UserGrade?,
    @field:Schema(description = "학년 이름. null 칸은 \"학년 미설정\"")
    val label: String,
    @field:Schema(description = "상세를 연 순 회원 수")
    val memberUniqueVisitors: Long,
) {
    companion object {
        fun from(row: AdvertisementContractReportGradeDto) = MyAdvertisementReportGradeResponse(row.grade, row.label, row.memberUniqueVisitors)
    }
}

data class MyAdvertisementReportTotalResponse(
    val total: MyAdvertisementReportTotalSumResponse,
    @field:Schema(description = "리포트를 열 수 있는 상품마다의 상품 몫. 계약에 붙인 순서")
    val products: List<MyAdvertisementReportProductSumResponse>,
) {
    companion object {
        fun from(total: AdvertisementContractReportTotalDto) = MyAdvertisementReportTotalResponse(
            total = MyAdvertisementReportTotalSumResponse.from(total.total),
            products = total.products.map(MyAdvertisementReportProductSumResponse::from),
        )
    }
}

data class MyAdvertisementReportTotalSumResponse(
    val impressions: Long,
    val uniqueImpressions: Long,
    val adClicks: Long,
    @field:Schema(description = "소재 클릭률(0~1). 노출이 없으면 null", nullable = true)
    val adClickRate: Double?,
    val views: Long,
    val ctaClicks: Long,
) {
    companion object {
        fun from(sum: AdvertisementContractReportTotalSumDto) = MyAdvertisementReportTotalSumResponse(
            impressions = sum.impressions,
            uniqueImpressions = sum.uniqueImpressions,
            adClicks = sum.adClicks,
            adClickRate = sum.adClickRate,
            views = sum.views,
            ctaClicks = sum.ctaClicks,
        )
    }
}

data class MyAdvertisementReportProductSumResponse(
    val contractProductId: Long,
    val category: AdvertisementProductCategory,
    @field:Schema(description = "노출 자리. 자리가 없는 상품은 null", nullable = true)
    val position: AdvertisementProductPosition?,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val impressions: Long,
    val uniqueImpressions: Long,
    val adClicks: Long,
    val views: Long,
    val ctaClicks: Long,
) {
    companion object {
        fun from(product: AdvertisementContractReportProductSumDto) = MyAdvertisementReportProductSumResponse(
            contractProductId = product.contractProductId,
            category = product.category,
            position = product.position,
            name = product.name,
            startDate = product.startDate,
            endDate = product.endDate,
            impressions = product.impressions,
            uniqueImpressions = product.uniqueImpressions,
            adClicks = product.adClicks,
            views = product.views,
            ctaClicks = product.ctaClicks,
        )
    }
}

data class MyActiveAdvertisementReportResponse(
    val contractId: Long,
    val contractProductId: Long,
    val category: AdvertisementProductCategory,
    @field:Schema(description = "노출 자리. 자리가 없는 상품은 null", nullable = true)
    val position: AdvertisementProductPosition?,
    @field:Schema(description = "조회 시점의 상품 이름")
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @field:Schema(description = "오늘부터 끝나는 날까지 남은 날 수. 끝나는 날이 오늘이면 0")
    val daysLeft: Long,
    val impressions: Long,
    val adClicks: Long,
    @field:Schema(description = "소재 클릭률(0~1). 노출이 없으면 null", nullable = true)
    val adClickRate: Double?,
    val views: Long,
    val ctaClicks: Long,
    @field:Schema(description = "스크롤 50%까지 내린 순 방문자 비율(0~1). 배너 상품이거나 방문자가 없으면 null", nullable = true)
    val scroll50Rate: Double?,
    @field:Schema(description = "광고 공고 상품이면 걸린 공고·부트캠프. 아니면 null", nullable = true)
    val target: MyActiveAdvertisementReportTargetResponse?,
    @field:Schema(description = "상품 시작일~오늘의 날짜별 숫자. 없는 날은 0")
    val daily: List<MyActiveAdvertisementReportDailyResponse>,
) {
    companion object {
        fun from(report: AdvertisementContractActiveReportDto) = MyActiveAdvertisementReportResponse(
            contractId = report.contractId,
            contractProductId = report.contractProductId,
            category = report.category,
            position = report.position,
            name = report.name,
            startDate = report.startDate,
            endDate = report.endDate,
            daysLeft = report.daysLeft,
            impressions = report.impressions,
            adClicks = report.adClicks,
            adClickRate = report.adClickRate,
            views = report.views,
            ctaClicks = report.ctaClicks,
            scroll50Rate = report.scroll50Rate,
            target = report.target?.let { MyActiveAdvertisementReportTargetResponse(it.targetType, it.targetId, it.title) },
            daily = report.daily.map { MyActiveAdvertisementReportDailyResponse(it.date, it.impressions, it.adClicks, it.views) },
        )
    }
}

data class MyActiveAdvertisementReportTargetResponse(
    val targetType: AdvertisementExposureTargetType,
    val targetId: Long,
    @field:Schema(description = "조회 시점의 공고·부트캠프 제목")
    val title: String,
)

data class MyActiveAdvertisementReportDailyResponse(
    val date: LocalDate,
    val impressions: Long,
    val adClicks: Long,
    val views: Long,
)
