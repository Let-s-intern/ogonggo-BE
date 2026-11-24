package com.ogonggo.core.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.user.domain.UserGrade
import java.time.LocalDate

/**
 * 계약 상품 하나의 성과 리포트다. 사용자·관리자 API가 같은 숫자를 보이도록 계산은 core가 한다.
 *
 * - [summary]·[daily]·[exposures]·[csvRows]는 상품 몫이다. 이 상품의 노출 식별자로 들어온 이벤트만 센다.
 * - [targets]는 계약 대상 공고의 전체 숫자다. 유입 경로와 상관없이 세고 경로별로 나눈다.
 * - [rawCountsApproximate]는 원본에서만 세는 값(공유 수, 회원 나눔)이 원본 보관 기간 밖 날을 뺀 값인지다.
 * - 배너 상품은 스크롤 깊이·체류 시간을 보이지 않아 그 값이 null이다.
 */
data class AdvertisementContractReportDto(
    val product: AdvertisementContractReportProductDto,
    val from: LocalDate,
    val to: LocalDate,
    val collectedSince: LocalDate,
    val uniqueCountsApproximate: Boolean,
    val rawCountsApproximate: Boolean,
    val summary: AdvertisementContractReportSummaryDto,
    val daily: List<AdvertisementContractReportDailyDto>,
    val exposures: List<AdvertisementContractReportExposureDto>,
    val targets: List<AdvertisementContractReportTargetDto>,
    val byJobField: List<AdvertisementContractReportJobFieldDto>,
    val audience: AdvertisementContractReportAudienceDto,
    val insights: List<AdvertisementReportInsightDto>,
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
    /** 상품의 소개용 가격(정가, 원). 리포트의 비용 효율은 이 값을 광고비로 쓴다. 견적 문의 상품은 null이다. */
    val price: Int?,
)

data class AdvertisementContractReportSummaryDto(
    val impressions: Long,
    val uniqueImpressions: Long,
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
    val uniqueImpressions: Long,
    /** 그날 광고를 본 서로 다른 사람 수다. 원본 보관 기간보다 앞날은 null이다. */
    val reach: Long?,
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
    val saves: Long,
    val shares: Long,
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

/**
 * 상품 몫의 로그인 회원을 희망 직군으로 나눈 한 칸이다. 희망 직무가 여러 직군에 걸친 회원은 각 직군에 한 번씩 센다.
 * 희망 직무가 없거나 오공고 직군으로 옮길 수 없으면 [jobField]가 null이다.
 */
data class AdvertisementContractReportJobFieldDto(
    val jobField: JobField?,
    val label: String,
    val memberUniqueVisitors: Long,
    val memberViews: Long,
    val ctaUniqueMembers: Long,
    val adClickMembers: Long,
)

/**
 * 상품 몫을 본 사람의 구성이다.
 *
 * - [reach]는 기간 동안 광고를 본 서로 다른 사람(회원·비회원 합), [avgFrequency]는 노출수 ÷ [reach]다. 하루 집계로는 기간 합을 낼 수 없어 원본으로 세므로,
 *   원본 보관 기간보다 앞날이 걸리면 [reach]·[adClickVisitors]·[avgFrequency]가 null이다.
 * - [adClickVisitors]는 광고를 눌러 공고를 연 서로 다른 사람이다.
 * - [targetJobFields]는 광고가 띄우는 공고의 직군과 노출을 띄우는 직군 탭이다. 없으면 [targetMatch]도 비율이 null이다.
 * - [byGrade]는 로그인 회원을 학년으로 나눈 것이다. 공고를 연 회원만 센다.
 */
data class AdvertisementContractReportAudienceDto(
    val reach: Long?,
    val avgFrequency: Double?,
    val adClickVisitors: Long?,
    val targetJobFields: List<JobField>,
    val targetMatch: AdvertisementContractReportTargetMatchDto,
    val byGrade: List<AdvertisementContractReportGradeDto>,
)

/** 공고를 연 회원 [members]명 중 희망 직무의 직군이 타깃 직군과 하나라도 겹치는 회원이 [matchedMembers]명이다. */
data class AdvertisementContractReportTargetMatchDto(
    val matchedMembers: Long,
    val members: Long,
    val rate: Double?,
)

/** 학년 한 칸이다. 학년을 입력하지 않은 회원은 [grade]가 null이다. */
data class AdvertisementContractReportGradeDto(
    val grade: UserGrade?,
    val label: String,
    val memberUniqueVisitors: Long,
)

/** 대시보드에 보이는 진행 중 광고 상품 하나다. 숫자는 상품 시작일부터 오늘까지의 상품 몫이다. */
data class AdvertisementContractActiveReportDto(
    val contractId: Long,
    val contractProductId: Long,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysLeft: Long,
    val impressions: Long,
    val adClicks: Long,
    val adClickRate: Double?,
    val views: Long,
    val ctaClicks: Long,
    val scroll50Rate: Double?,
    val target: AdvertisementContractReportPostingDto?,
    val daily: List<AdvertisementContractActiveDailyDto>,
)

/** 광고 공고 상품의 노출이 띄우는 공고·부트캠프다. */
data class AdvertisementContractReportPostingDto(
    val targetType: AdvertisementExposureTargetType,
    val targetId: Long,
    val title: String,
    val companyName: String,
    val thumbnailUrl: String?,
    val logoUrl: String?,
)

data class AdvertisementContractActiveDailyDto(
    val date: LocalDate,
    val impressions: Long,
    val adClicks: Long,
    val views: Long,
)

/** 계약 전체 합계다. 리포트를 열 수 있는 상품만 각 상품 기간 전체(진행 중이면 오늘까지)로 더한다. */
data class AdvertisementContractReportTotalDto(
    val total: AdvertisementContractReportTotalSumDto,
    val products: List<AdvertisementContractReportProductSumDto>,
)

data class AdvertisementContractReportTotalSumDto(
    val impressions: Long,
    val uniqueImpressions: Long,
    val adClicks: Long,
    val adClickRate: Double?,
    val views: Long,
    val ctaClicks: Long,
)

data class AdvertisementContractReportProductSumDto(
    val contractProductId: Long,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val impressions: Long,
    val uniqueImpressions: Long,
    val adClicks: Long,
    val views: Long,
    val ctaClicks: Long,
)
