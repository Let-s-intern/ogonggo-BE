package com.ogonggo.adminapi.announcement.presentation.request

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementCreateCommand
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementUpdateCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAdminAnnouncementRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    @Schema(description = "Lexical EditorState JSON 문자열입니다.")
    @field:NotBlank
    val content: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
) {
    fun toCommand(): AdminAnnouncementCreateCommand = AdminAnnouncementCreateCommand(
        title = title,
        content = content,
        pinned = pinned,
        visibility = visibility,
    )
}

/** 모든 값이 선택이며 넘어온 값만 바꾼다. 제목과 본문은 비울 수 없다. */
data class UpdateAdminAnnouncementRequest(
    @field:Size(max = 255)
    val title: String?,
    @Schema(description = "Lexical EditorState JSON 문자열입니다.")
    val content: String?,
    val pinned: Boolean?,
    val visibility: AdminContentVisibility?,
) {
    fun toCommand(): AdminAnnouncementUpdateCommand {
        if (title != null && title.isBlank()) {
            throw InvalidRequestFieldException("title", "제목을 입력해 주세요.")
        }
        return AdminAnnouncementUpdateCommand(
            title = title,
            content = content,
            pinned = pinned,
            visibility = visibility,
        )
    }
}
