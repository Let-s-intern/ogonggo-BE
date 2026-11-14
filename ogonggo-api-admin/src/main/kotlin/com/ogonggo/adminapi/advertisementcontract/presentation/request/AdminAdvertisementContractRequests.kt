package com.ogonggo.adminapi.advertisementcontract.presentation.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class ChangeAdminAdvertisementContractStatusRequest(
    @field:JsonDeserialize(using = StrictAdvertisementContractStatusDeserializer::class)
    val status: AdvertisementContractStatus,
)

data class ChangeAdminAdvertisementContractPaymentAmountRequest(
    @param:JsonProperty(value = "paymentAmount", required = true)
    @field:Min(0)
    @field:JsonDeserialize(using = StrictNullableIntDeserializer::class)
    @field:Schema(description = "협의한 결제 금액(원). null을 보내면 입력한 금액을 해제합니다.", nullable = true)
    val paymentAmount: Int?,
)

data class AddAdminAdvertisementContractProductRequest(
    @field:Positive
    @field:JsonDeserialize(using = StrictLongDeserializer::class)
    val productId: Long,
    @field:Schema(description = "게시 시작일(yyyy-MM-dd).", example = "2026-11-02", requiredMode = Schema.RequiredMode.REQUIRED)
    val startDate: LocalDate,
    @field:Schema(description = "게시 종료일(yyyy-MM-dd). 이 날까지 게시하며 시작일과 같아도 됩니다.", example = "2026-11-05", requiredMode = Schema.RequiredMode.REQUIRED)
    val endDate: LocalDate,
)

internal class StrictAdvertisementContractStatusDeserializer : JsonDeserializer<AdvertisementContractStatus>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): AdvertisementContractStatus {
        if (parser.currentToken != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "status는 enum 이름 문자열이어야 합니다.")
        }
        return try {
            AdvertisementContractStatus.valueOf(parser.text)
        } catch (_: IllegalArgumentException) {
            throw JsonMappingException.from(parser, "status는 유효한 enum 이름이어야 합니다.")
        }
    }
}

internal class StrictLongDeserializer : JsonDeserializer<Long>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Long {
        if (parser.currentToken != JsonToken.VALUE_NUMBER_INT) {
            throw JsonMappingException.from(parser, "productId는 정수여야 합니다.")
        }
        return parser.longValue
    }
}

internal class StrictNullableIntDeserializer : JsonDeserializer<Int?>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Int {
        if (parser.currentToken != JsonToken.VALUE_NUMBER_INT) {
            throw JsonMappingException.from(parser, "paymentAmount는 정수 또는 null이어야 합니다.")
        }
        return parser.intValue
    }
}
