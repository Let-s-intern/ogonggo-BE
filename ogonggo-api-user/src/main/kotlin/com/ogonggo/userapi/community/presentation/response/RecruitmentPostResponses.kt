package com.ogonggo.userapi.community.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.business.RecruitmentPostAuthorResult
import com.ogonggo.userapi.community.business.RecruitmentPostContactResult
import com.ogonggo.userapi.community.business.RecruitmentPostDetailResult
import com.ogonggo.userapi.community.business.RecruitmentPostSummary
import java.time.LocalDate

data class CreateRecruitmentPostResponse(
    val id: Long,
)

data class RecruitmentPostDetailResponse(
    val id: Long,
    val author: RecruitmentPostAuthorResponse,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val progressMethod: ProgressMethod,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val positions: List<RecruitmentPosition>,
    val contact: RecruitmentPostContactResponse,
    val summary: String,
    val content: JsonNode,
    val eligibilityAndSelectionProcess: String?,
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val bookmarked: Boolean = false,
) {
    companion object {
        fun from(
            result: RecruitmentPostDetailResult,
            objectMapper: ObjectMapper,
        ): RecruitmentPostDetailResponse =
            RecruitmentPostDetailResponse(
                id = result.id,
                author = RecruitmentPostAuthorResponse.from(result.author),
                title = result.title,
                recruitmentType = result.recruitmentType,
                recruitmentStatus = result.recruitmentStatus,
                recruitmentStartDate = result.recruitmentStartDate,
                recruitmentEndDate = result.recruitmentEndDate,
                progressMethod = result.progressMethod,
                capacity = result.capacity,
                activityDurationMonths = result.activityDurationMonths,
                technologyStacks = result.technologyStacks,
                positions = result.positions,
                contact = RecruitmentPostContactResponse.from(result.contact),
                summary = result.summary,
                content = objectMapper.readTree(result.content),
                eligibilityAndSelectionProcess = result.eligibilityAndSelectionProcess,
                viewCount = result.viewCount,
                commentCount = result.commentCount,
                bookmarked = result.bookmarked,
            )
    }
}

@JsonInclude(JsonInclude.Include.ALWAYS)
data class RecruitmentPostAuthorResponse(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(result: RecruitmentPostAuthorResult): RecruitmentPostAuthorResponse =
            RecruitmentPostAuthorResponse(
                userId = result.userId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
            )
    }
}

data class RecruitmentPostContactResponse(
    val method: ContactMethod,
    val value: String,
) {
    companion object {
        fun from(result: RecruitmentPostContactResult): RecruitmentPostContactResponse =
            RecruitmentPostContactResponse(method = result.method, value = result.value)
    }
}

data class RecruitmentPostSummaryResponse(
    val id: Long,
    val author: RecruitmentPostAuthorResponse,
    val title: String,
    val recruitmentType: RecruitmentType,
    val progressMethod: ProgressMethod,
    val recruitmentStatus: RecruitmentStatus,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val bookmarked: Boolean = false,
) {
    companion object {
        fun from(summary: RecruitmentPostSummary): RecruitmentPostSummaryResponse = RecruitmentPostSummaryResponse(
            id = summary.id,
            author = RecruitmentPostAuthorResponse.from(summary.author),
            title = summary.title,
            recruitmentType = summary.recruitmentType,
            progressMethod = summary.progressMethod,
            recruitmentStatus = summary.recruitmentStatus,
            capacity = summary.capacity,
            activityDurationMonths = summary.activityDurationMonths,
            technologyStacks = summary.technologyStacks,
            recruitmentStartDate = summary.recruitmentStartDate,
            recruitmentEndDate = summary.recruitmentEndDate,
            viewCount = summary.viewCount,
            commentCount = summary.commentCount,
            bookmarked = summary.bookmarked,
        )
    }
}
