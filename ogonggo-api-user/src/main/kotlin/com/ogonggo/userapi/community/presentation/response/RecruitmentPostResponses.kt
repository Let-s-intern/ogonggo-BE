package com.ogonggo.userapi.community.presentation.response

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
    val content: String,
    val eligibilityAndSelectionProcess: String?,
) {
    companion object {
        fun from(result: RecruitmentPostDetailResult): RecruitmentPostDetailResponse =
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
                content = result.content,
                eligibilityAndSelectionProcess = result.eligibilityAndSelectionProcess,
            )
    }
}

data class RecruitmentPostAuthorResponse(
    val userId: Long,
) {
    companion object {
        fun from(result: RecruitmentPostAuthorResult): RecruitmentPostAuthorResponse =
            RecruitmentPostAuthorResponse(userId = result.userId)
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
    val title: String,
    val recruitmentType: RecruitmentType,
    val progressMethod: ProgressMethod,
    val recruitmentStatus: RecruitmentStatus,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
) {
    companion object {
        fun from(summary: RecruitmentPostSummary): RecruitmentPostSummaryResponse = RecruitmentPostSummaryResponse(
            id = summary.id,
            title = summary.title,
            recruitmentType = summary.recruitmentType,
            progressMethod = summary.progressMethod,
            recruitmentStatus = summary.recruitmentStatus,
            capacity = summary.capacity,
            activityDurationMonths = summary.activityDurationMonths,
            technologyStacks = summary.technologyStacks,
            recruitmentStartDate = summary.recruitmentStartDate,
            recruitmentEndDate = summary.recruitmentEndDate,
        )
    }
}
