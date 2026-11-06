package com.ogonggo.adminapi.advertisernote.presentation.request

import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteCreateCommand
import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteUpdateCommand
import io.swagger.v3.oas.annotations.media.Schema

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. 쓴 사람은 본문으로 받지 않는다. */
data class CreateAdminAdvertiserNoteRequest(
    @Schema(description = "제목. 앞뒤 공백을 빼고 1~100자입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val title: String? = null,
    @Schema(description = "내용. 여러 줄이며 앞뒤 공백을 빼고 1~2000자입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String? = null,
) {
    fun toCommand(): AdminAdvertiserNoteCreateCommand = AdminAdvertiserNoteCreateCommand(title = title, content = content)
}

/** 보낸 칸만 바꿉니다. */
data class UpdateAdminAdvertiserNoteRequest(
    val title: String? = null,
    val content: String? = null,
) {
    fun toCommand(): AdminAdvertiserNoteUpdateCommand = AdminAdvertiserNoteUpdateCommand(title = title, content = content)
}
