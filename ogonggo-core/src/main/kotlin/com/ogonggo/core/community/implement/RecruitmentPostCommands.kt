package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import java.time.LocalDate

data class RecruitmentPostAppendCommand(
    val authorUserId: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val capacity: Int,
    val progressMethod: ProgressMethod,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val summary: String,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val positions: List<RecruitmentPosition>,
    val contactMethod: ContactMethod,
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

data class RecruitmentPostDraftAppendCommand(
    val authorUserId: Long,
    val title: String,
    val recruitmentType: RecruitmentType?,
    val capacity: Int?,
    val progressMethod: ProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPosition>,
    val contactMethod: ContactMethod?,
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
        publicationStatus = PublicationStatus.DRAFT,
    )
}

sealed interface RecruitmentPostSaveCommand {
    data class Draft(
        val command: RecruitmentPostDraftAppendCommand,
    ) : RecruitmentPostSaveCommand

    data class Published(
        val command: RecruitmentPostAppendCommand,
    ) : RecruitmentPostSaveCommand
}

data class RecruitmentPostUpdateCommand(
    val title: String,
    val recruitmentType: RecruitmentType?,
    val capacity: Int?,
    val progressMethod: ProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPosition>,
    val contactMethod: ContactMethod?,
    val contactValue: String?,
)
