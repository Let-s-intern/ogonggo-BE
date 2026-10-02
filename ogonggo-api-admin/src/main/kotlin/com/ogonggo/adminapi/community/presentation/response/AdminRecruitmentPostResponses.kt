package com.ogonggo.adminapi.community.presentation.response

import com.ogonggo.adminapi.community.business.AdminRecruitmentPostSummary
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminRecruitmentPostSummaryResponse(
    val id: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val progressMethod: ProgressMethod,
    val capacity: Int,
    @Schema(description = "활동 기간(개월)")
    val activityDurationMonths: Int,
    val positions: List<RecruitmentPosition>,
    val technologyStacks: List<String>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val recruitmentStatus: RecruitmentStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val authorUserId: Long,
    @Schema(description = "작성자 프로필이 없으면 null입니다.")
    val authorNickname: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminRecruitmentPostSummary): AdminRecruitmentPostSummaryResponse =
            AdminRecruitmentPostSummaryResponse(
                id = result.id,
                title = result.title,
                recruitmentType = result.recruitmentType,
                progressMethod = result.progressMethod,
                capacity = result.capacity,
                activityDurationMonths = result.activityDurationMonths,
                positions = result.positions,
                technologyStacks = result.technologyStacks,
                recruitmentStartDate = result.recruitmentStartDate,
                recruitmentEndDate = result.recruitmentEndDate,
                recruitmentStatus = result.recruitmentStatus,
                closedAt = result.closedAt,
                viewCount = result.viewCount,
                bookmarkCount = result.bookmarkCount,
                commentCount = result.commentCount,
                visibility = result.visibility,
                authorUserId = result.authorUserId,
                authorNickname = result.authorNickname,
                registeredAt = result.registeredAt,
            )
    }
}
