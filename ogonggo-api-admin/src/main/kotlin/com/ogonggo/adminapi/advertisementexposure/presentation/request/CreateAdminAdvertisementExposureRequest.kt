package com.ogonggo.adminapi.advertisementexposure.presentation.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.ogonggo.adminapi.advertisementexposure.business.CreateAdminAdvertisementExposureCommand
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateAdminAdvertisementExposureRequest(
    @field:Positive
    @field:JsonDeserialize(using = StrictExposureLongDeserializer::class)
    @field:Schema(minimum = "1")
    val productId: Long,
    @field:Positive
    @field:JsonDeserialize(using = StrictNullableExposureLongDeserializer::class)
    @field:Schema(description = "자체 홍보면 null입니다.", nullable = true, minimum = "1")
    val contractId: Long? = null,
    @field:Size(max = 100)
    @field:Schema(nullable = true)
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
    @field:JsonDeserialize(using = StrictExposureBooleanDeserializer::class)
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val exposed: Boolean,
) {
    @JsonAnySetter
    fun rejectUnknownProperty(propertyName: String, @Suppress("UNUSED_PARAMETER") value: JsonNode) {
        throw IllegalArgumentException("지원하지 않는 요청 필드입니다: $propertyName")
    }

    fun toCommand(): CreateAdminAdvertisementExposureCommand = CreateAdminAdvertisementExposureCommand(
        productId = productId,
        contractId = contractId,
        name = name,
        imageUrl = imageUrl,
        linkUrl = linkUrl,
        targetType = targetType,
        targetId = targetId,
        adLabel = adLabel,
        startAt = startAt,
        endAt = endAt,
        exposed = exposed,
    )
}

internal class StrictExposureLongDeserializer : JsonDeserializer<Long>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Long {
        if (parser.currentToken != JsonToken.VALUE_NUMBER_INT) {
            throw JsonMappingException.from(parser, "식별자는 정수여야 합니다.")
        }
        return parser.longValue
    }
}

internal class StrictNullableExposureLongDeserializer : JsonDeserializer<Long?>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Long? {
        if (parser.currentToken != JsonToken.VALUE_NUMBER_INT) {
            throw JsonMappingException.from(parser, "식별자는 정수 또는 null이어야 합니다.")
        }
        return parser.longValue
    }
}

internal class StrictExposureBooleanDeserializer : JsonDeserializer<Boolean>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Boolean = when (parser.currentToken) {
        JsonToken.VALUE_TRUE -> true
        JsonToken.VALUE_FALSE -> false
        else -> throw JsonMappingException.from(parser, "true 또는 false를 보내야 합니다.")
    }

    override fun getNullValue(context: DeserializationContext): Boolean? = null
}

internal class StrictExposureTargetTypeDeserializer : JsonDeserializer<AdvertisementExposureTargetType?>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): AdvertisementExposureTargetType? {
        if (parser.currentToken != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "targetType은 enum 이름 문자열이어야 합니다.")
        }
        return try {
            AdvertisementExposureTargetType.valueOf(parser.text)
        } catch (_: IllegalArgumentException) {
            throw JsonMappingException.from(parser, "targetType은 JOB 또는 BOOTCAMP여야 합니다.")
        }
    }
}
