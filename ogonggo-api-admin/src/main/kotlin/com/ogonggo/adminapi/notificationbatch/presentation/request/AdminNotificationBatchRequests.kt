package com.ogonggo.adminapi.notificationbatch.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
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
    @field:Schema(description = "JOB_ROLES에서만 보낸다. job-role-options의 직무 이름 또는 '<직군> 직무 전체'.")
    @field:Size(max = 100)
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val jobRoles: List<String>? = null,
    @field:Schema(description = "광고성이면 true. 렛츠커리어 마케팅 수신 동의자에게만 기기마다 보낸다. 기업 회원 대상에는 쓸 수 없다.")
    val advertisement: Boolean = false,
    @field:Schema(description = "관리자 이미지 업로드 API에서 받은 이미지 ID. 생략하면 이미지 없이 발송", format = "uuid")
    @field:Size(max = 36)
    val imageId: String? = null,
) {
    fun toCommand() = CreateNotificationBatchCommand(targetType, userIds, title, body, data, jobRoles, advertisement, imageId)
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
