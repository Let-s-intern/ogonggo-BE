package com.ogonggo.adminapi.announcement.presentation.request

import org.hibernate.validator.constraints.URL
import com.ogonggo.core.announcement.domain.AnnouncementCategory
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
    val category: AnnouncementCategory,
    @Schema(description = "추천 카드 썸네일 주소(선택). 이미지는 사용자 API `POST /api/v1/images`(`usage=NOTIFICATION`)로 올립니다.")
    @field:Size(max = 2048)
    @field:URL
    val thumbnailUrl: String? = null,
    @Schema(description = "목록·추천 카드에 보이는 한두 줄 요약(선택).")
    @field:Size(max = 300)
    val summary: String? = null,
) {
    fun toCommand(): AdminAnnouncementCreateCommand = AdminAnnouncementCreateCommand(
        title = title,
        content = content,
        pinned = pinned,
        visibility = visibility,
        category = category,
        thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
        summary = summary?.takeIf { it.isNotBlank() },
    )
}

/** 모든 값이 선택이며 넘어온 값만 바꾼다. 제목과 본문은 비울 수 없다. 썸네일과 요약은 빈 글자를 보내면 지운다. */
data class UpdateAdminAnnouncementRequest(
    @field:Size(max = 255)
    val title: String?,
    @Schema(description = "Lexical EditorState JSON 문자열입니다.")
    val content: String?,
    val pinned: Boolean?,
    val visibility: AdminContentVisibility?,
    val category: AnnouncementCategory? = null,
    @Schema(description = "추천 카드 썸네일 주소. 빈 글자를 보내면 지웁니다.")
    @field:Size(max = 2048)
    @field:URL
    val thumbnailUrl: String? = null,
    @Schema(description = "목록·추천 카드에 보이는 한두 줄 요약. 빈 글자를 보내면 지웁니다.")
    @field:Size(max = 300)
    val summary: String? = null,
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
            category = category,
            thumbnailUrl = thumbnailUrl,
            summary = summary,
        )
    }
}
