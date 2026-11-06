package com.ogonggo.adminapi.calendartodo.presentation.request

import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoCreateCommand
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoUpdateCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminCalendarTodoRequest(
    @Schema(description = "제목. 앞뒤 공백을 빼고 1~100자입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val title: String? = null,
    @Schema(description = "내용. 2000자까지이고 빈 문자열은 null로 저장합니다.", nullable = true)
    val content: String? = null,
    @Schema(description = "시작일(KST, 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
    val startDate: LocalDate? = null,
    @Schema(description = "끝나는 날(KST, 포함). startDate와 같거나 뒤입니다. 하루짜리는 둘이 같습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val endDate: LocalDate? = null,
    @Schema(description = "완료 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    val done: Boolean? = null,
) {
    fun toCommand(): AdminCalendarTodoCreateCommand = AdminCalendarTodoCreateCommand(
        title = title,
        content = content,
        startDate = startDate,
        endDate = endDate,
        done = done,
    )
}

/**
 * 보낸 칸만 바꿉니다. `content`는 null을 보내면 지웁니다. 그 밖의 칸은 null이면 보내지 않은 것과 같습니다.
 * 검증은 저장된 값과 합친 결과로 합니다.
 */
data class UpdateAdminCalendarTodoRequest(
    val title: String? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 내용을 지웁니다.")
    val content: Optional<String>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val done: Boolean? = null,
) {
    fun toCommand(): AdminCalendarTodoUpdateCommand = AdminCalendarTodoUpdateCommand(
        title = title,
        content = content,
        startDate = startDate,
        endDate = endDate,
        done = done,
    )
}
