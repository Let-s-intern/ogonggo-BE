package com.ogonggo.userapi.recruitmentpost.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostAuthorResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostContactResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostDetailResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostSummary
import java.time.LocalDate

data class CreateRecruitmentPostResponse(
    val id: Long,
)

data class RecruitmentPostDetailResponse(
    val id: Long,
    val author: RecruitmentPostAuthorResponse,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val progressMethod: RecruitmentPostProgressMethod,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val positions: List<RecruitmentPostPosition>,
    val contact: RecruitmentPostContactResponse,
    val summary: String,
    val content: JsonNode,
    val eligibilityAndSelectionProcess: String?,
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val bookmarkCount: Long = 0,
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
                bookmarkCount = result.bookmarkCount,
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
    val method: RecruitmentPostContactMethod,
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
    val recruitmentType: RecruitmentPostType,
    val progressMethod: RecruitmentPostProgressMethod,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val positions: List<RecruitmentPostPosition>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val applicationCount: Long = 0,
    val bookmarkCount: Long = 0,
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
            positions = summary.positions,
            recruitmentStartDate = summary.recruitmentStartDate,
            recruitmentEndDate = summary.recruitmentEndDate,
            viewCount = summary.viewCount,
            commentCount = summary.commentCount,
            applicationCount = summary.applicationCount,
            bookmarkCount = summary.bookmarkCount,
            bookmarked = summary.bookmarked,
        )
    }
}
