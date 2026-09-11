package com.ogonggo.userapi.community.presentation.response

import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.business.RecruitmentPostSummary
import java.time.LocalDate

data class CreateRecruitmentPostResponse(
    val id: Long,
)

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
