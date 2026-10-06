package com.ogonggo.core.recruitmentpost.implement.dto

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import java.time.LocalDate

data class RecruitmentPostAppendDto(
    val authorUserId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val capacity: Int,
    val progressMethod: RecruitmentPostProgressMethod,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val summary: String,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod,
    val contactValue: String,
) {
    fun toEntity(): RecruitmentPost = RecruitmentPost(
        authorUserId = authorUserId,
        title = title,
        recruitmentType = recruitmentType,
        capacity = capacity,
        progressMethod = progressMethod,
        activityDurationMonths = activityDurationMonths,
        technologyStacks = technologyStacks,
        summary = summary,
        content = content,
        eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = contactMethod,
        contactValue = contactValue,
    )
}

data class RecruitmentPostDraftAppendDto(
    val authorUserId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType?,
    val capacity: Int?,
    val progressMethod: RecruitmentPostProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod?,
    val contactValue: String?,
) {
    fun toEntity(): RecruitmentPost = RecruitmentPost(
        authorUserId = authorUserId,
        title = title,
        recruitmentType = recruitmentType,
        capacity = capacity,
        progressMethod = progressMethod,
        activityDurationMonths = activityDurationMonths,
        technologyStacks = technologyStacks,
        summary = summary,
        content = content,
        eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = contactMethod,
        contactValue = contactValue,
        publicationStatus = RecruitmentPostPublicationStatus.DRAFT,
    )
}

data class RecruitmentPostUpdateDto(
    val title: String,
    val recruitmentType: RecruitmentPostType?,
    val capacity: Int?,
    val progressMethod: RecruitmentPostProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod?,
    val contactValue: String?,
)
