package com.ogonggo.adminapi.advertisementexposure.presentation.response

import com.ogonggo.adminapi.advertisementexposure.business.AdminAdvertisementExposureResult
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.job.domain.JobField
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdvertisementExposureResponse(
    val id: Long,
    val productId: Long,
    @field:Schema(nullable = true)
    val contractId: Long?,
    @field:Schema(nullable = true)
    val name: String?,
    @field:Schema(nullable = true)
    val imageUrl: String?,
    @field:Schema(description = "모바일 배너 이미지. null이면 모바일에서도 imageUrl을 씁니다.", nullable = true)
    val mobileImageUrl: String?,
    @field:Schema(nullable = true)
    val linkUrl: String?,
    @field:Schema(nullable = true)
    val targetType: AdvertisementExposureTargetType?,
    @field:Schema(nullable = true)
    val targetId: Long?,
    @field:Schema(description = "채용공고 목록(JOB_LIST) 노출의 직군 탭. JOB_LIST의 null은 전체 탭이고, 다른 자리와 배너는 늘 null입니다.", nullable = true)
    val jobField: JobField?,
    @field:Schema(description = "오공고 PICK(JOB_PICK) 노출의 추천 문구 제목. 다른 자리와 배너는 null입니다.", nullable = true)
    val recommendationTitle: String?,
    @field:Schema(description = "오공고 PICK(JOB_PICK) 노출의 추천 문구 설명. 다른 자리와 배너는 null입니다.", nullable = true)
    val recommendationDescription: String?,
    val adLabel: Boolean,
    @field:Schema(description = "노출 시작 일시. 노출 기간은 실제로 광고가 화면에 노출되는 기간입니다. 없으면 상시", nullable = true)
    val startAt: LocalDateTime?,
    @field:Schema(description = "노출 종료 일시. 노출 기간은 실제로 광고가 화면에 노출되는 기간입니다. 없으면 상시", nullable = true)
    val endAt: LocalDateTime?,
    val exposed: Boolean,
    @field:Schema(nullable = true)
    val advertiserCompanyName: String?,
    @field:Schema(description = "광고 공고 대상의 제목. 배너이거나 대상 행이 없으면 null입니다.", nullable = true)
    val targetTitle: String?,
    @field:Schema(description = "광고 공고 대상의 회사명. 배너이거나 대상 행이 없으면 null입니다.", nullable = true)
    val targetCompanyName: String?,
    @field:Schema(
        description = "광고 공고 대상이 지금 게시 상태(PUBLISHED)이고 지워지지 않았는지. 모집 마감 여부는 보지 않습니다. 대상 행이 없으면 false, 배너는 null입니다.",
        nullable = true,
    )
    val targetPublished: Boolean?,
    val displayOrder: Int,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminAdvertisementExposureResult) = AdminAdvertisementExposureResponse(
            id = result.id,
            productId = result.productId,
            contractId = result.contractId,
            name = result.name,
            imageUrl = result.imageUrl,
            mobileImageUrl = result.mobileImageUrl,
            linkUrl = result.linkUrl,
            targetType = result.targetType,
            targetId = result.targetId,
            jobField = result.jobField,
            recommendationTitle = result.recommendationTitle,
            recommendationDescription = result.recommendationDescription,
            adLabel = result.adLabel,
            startAt = result.startAt,
            endAt = result.endAt,
            exposed = result.exposed,
            advertiserCompanyName = result.advertiserCompanyName,
            targetTitle = result.targetTitle,
            targetCompanyName = result.targetCompanyName,
            targetPublished = result.targetPublished,
            displayOrder = result.displayOrder,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class CreateAdminAdvertisementExposureResponse(val id: Long) {
    companion object {
        fun from(id: Long) = CreateAdminAdvertisementExposureResponse(id)
    }
}
