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
