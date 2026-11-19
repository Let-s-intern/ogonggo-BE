package com.ogonggo.adminapi.advertisementexposure.presentation.request

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.ogonggo.adminapi.advertisementexposure.business.UpdateAdminAdvertisementExposureCommand
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.job.domain.JobField
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UpdateAdminAdvertisementExposureRequest(
    @field:Positive
    @field:JsonDeserialize(using = StrictNullableExposureLongDeserializer::class)
    @field:Schema(description = "광고주 계약. null이면 자체 홍보입니다.", nullable = true)
    val contractId: Long? = null,
    @field:Size(max = 100)
    @field:Schema(nullable = true)
    val name: String? = null,
    @field:Size(max = 2048)
    @field:ValidAdvertisementImageUrl
    @field:Schema(description = "배너 이미지는 HTTP(S) 절대 URL이어야 합니다.", nullable = true)
    val imageUrl: String? = null,
    @field:Size(max = 2048)
    @field:ValidAdvertisementImageUrl
    @field:Schema(
        description = "배너 모바일 이미지. 선택이며 HTTP(S) 절대 URL이어야 합니다. null이면 모바일에서도 imageUrl을 씁니다. 광고 공고는 보내지 않습니다.",
        nullable = true,
    )
    val mobileImageUrl: String? = null,
    @field:Size(max = 2048)
    @field:ValidAdvertisementLinkUrl
    @field:Schema(description = "HTTP(S) URL 또는 단일 슬래시로 시작하는 내부 경로입니다.", nullable = true)
    val linkUrl: String? = null,
    @field:JsonDeserialize(using = StrictExposureTargetTypeDeserializer::class)
    @field:Schema(nullable = true)
    val targetType: AdvertisementExposureTargetType? = null,
    @field:Positive
    @field:JsonDeserialize(using = StrictNullableExposureLongDeserializer::class)
    @field:Schema(nullable = true)
    val targetId: Long? = null,
    @field:Schema(
        description = "광고 공고가 채용공고 목록(JOB_LIST) 자리면 띄울 직군 탭. GET /api/v1/enums의 JobField 값이며 JOB_LIST는 필수, 다른 자리와 배너는 null입니다.",
        nullable = true,
    )
    val jobField: JobField? = null,
    @field:Size(max = AdvertisementExposure.MAX_RECOMMENDATION_TITLE_LENGTH)
    @field:Schema(
        description = "오공고 PICK(JOB_PICK) 광고 카드의 추천 문구 제목(운영자 PICK과 같은 30자 이하). JOB_PICK은 필수이고 공백일 수 없으며, 다른 자리와 배너는 null입니다.",
        nullable = true,
    )
    val recommendationTitle: String? = null,
    @field:Size(max = AdvertisementExposure.MAX_RECOMMENDATION_DESCRIPTION_LENGTH)
    @field:Schema(
        description = "오공고 PICK(JOB_PICK) 광고 카드의 추천 문구 설명(운영자 PICK과 같은 200자 이하). JOB_PICK은 필수이고 공백일 수 없으며, 다른 자리와 배너는 null입니다.",
        nullable = true,
    )
    val recommendationDescription: String? = null,
    @field:JsonDeserialize(using = StrictExposureBooleanDeserializer::class)
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val adLabel: Boolean,
    @field:Schema(description = "Asia/Seoul 기준 일시. 시작·종료를 함께 생략하면 상시입니다.", nullable = true)
    val startAt: LocalDateTime? = null,
    @field:Schema(description = "Asia/Seoul 기준 일시. 시작·종료를 함께 생략하면 상시입니다.", nullable = true)
    val endAt: LocalDateTime? = null,
) {
    @JsonAnySetter
    fun rejectUnknownProperty(propertyName: String, @Suppress("UNUSED_PARAMETER") value: JsonNode) {
        throw IllegalArgumentException("지원하지 않는 요청 필드입니다: $propertyName")
    }

    fun toCommand(exposureId: Long): UpdateAdminAdvertisementExposureCommand = UpdateAdminAdvertisementExposureCommand(
        exposureId = exposureId,
        contractId = contractId,
        name = name,
        imageUrl = imageUrl,
        mobileImageUrl = mobileImageUrl,
        linkUrl = linkUrl,
        targetType = targetType,
        targetId = targetId,
        jobField = jobField,
        recommendationTitle = recommendationTitle,
        recommendationDescription = recommendationDescription,
        adLabel = adLabel,
        startAt = startAt,
        endAt = endAt,
    )
}

data class ChangeAdminAdvertisementExposureExposedRequest(
    @field:JsonDeserialize(using = StrictExposureBooleanDeserializer::class)
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val exposed: Boolean,
) {
    @JsonAnySetter
    fun rejectUnknownProperty(propertyName: String, @Suppress("UNUSED_PARAMETER") value: JsonNode) {
        throw IllegalArgumentException("지원하지 않는 요청 필드입니다: $propertyName")
    }
}

data class MoveAdminAdvertisementExposureRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val direction: AdvertisementExposureMoveDirection,
) {
    @JsonAnySetter
    fun rejectUnknownProperty(propertyName: String, @Suppress("UNUSED_PARAMETER") value: JsonNode) {
        throw IllegalArgumentException("지원하지 않는 요청 필드입니다: $propertyName")
    }
}
