package com.ogonggo.adminapi.advertisementexposure.presentation.request

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.ogonggo.adminapi.advertisementexposure.business.UpdateAdminAdvertisementExposureCommand
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UpdateAdminAdvertisementExposureRequest(
    @field:Positive
    @field:JsonDeserialize(using = StrictNullableExposureLongDeserializer::class)
    @field:Schema(description = "광고주 계약. null이면 자체 홍보입니다.", nullable = true, minimum = "1")
    val contractId: Long? = null,
    @field:Size(max = 100)
    @field:Schema(nullable = true, maxLength = 100)
    val name: String? = null,
    @field:Size(max = 2048)
    @field:Schema(description = "배너 이미지는 HTTP(S) 절대 URL이어야 합니다.", nullable = true, maxLength = 2048)
    val imageUrl: String? = null,
    @field:Size(max = 2048)
    @field:Schema(description = "HTTP(S) URL 또는 단일 슬래시로 시작하는 내부 경로입니다.", nullable = true, maxLength = 2048)
    val linkUrl: String? = null,
    @field:JsonDeserialize(using = StrictExposureTargetTypeDeserializer::class)
    @field:Schema(nullable = true)
    val targetType: AdvertisementExposureTargetType? = null,
    @field:Positive
    @field:JsonDeserialize(using = StrictNullableExposureLongDeserializer::class)
    @field:Schema(nullable = true, minimum = "1")
    val targetId: Long? = null,
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
        linkUrl = linkUrl,
        targetType = targetType,
        targetId = targetId,
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
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ["UP", "DOWN"])
    val direction: AdvertisementExposureMoveDirection,
) {
    @JsonAnySetter
    fun rejectUnknownProperty(propertyName: String, @Suppress("UNUSED_PARAMETER") value: JsonNode) {
        throw IllegalArgumentException("지원하지 않는 요청 필드입니다: $propertyName")
    }
}
