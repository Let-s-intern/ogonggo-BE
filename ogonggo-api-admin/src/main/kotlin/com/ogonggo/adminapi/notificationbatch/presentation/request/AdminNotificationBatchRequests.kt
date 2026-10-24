package com.ogonggo.adminapi.notificationbatch.presentation.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.ogonggo.adminapi.notificationbatch.business.CreateNotificationBatchCommand
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateNotificationBatchRequest(
    val targetType: NotificationBatchTargetType,
    @field:Size(max = 1000)
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val userIds: List<Long>?,
    @field:NotBlank @field:Size(max = 100)
    val title: String,
    @field:NotBlank @field:Size(max = 1000)
    val body: String,
    @field:Size(max = 20)
    @field:JsonDeserialize(contentUsing = NotificationDataStringDeserializer::class)
    val data: Map<String, String>,
) {
    fun toCommand() = CreateNotificationBatchCommand(targetType, userIds, title, body, data)
}

/** Jackson의 숫자·boolean→문자열 변환을 이 요청의 data에서만 막는다. */
internal class NotificationDataStringDeserializer : JsonDeserializer<String>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): String {
        if (parser.currentToken != JsonToken.VALUE_STRING) throw JsonMappingException.from(parser, "data 값은 문자열이어야 합니다.")
        return parser.text
    }

    override fun getNullValue(context: DeserializationContext): String =
        throw JsonMappingException.from(context.parser, "data 값은 null일 수 없습니다.")
}
