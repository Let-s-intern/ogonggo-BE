package com.ogonggo.userapi.community.presentation.response

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.business.RecruitmentPostFormResult
import com.ogonggo.userapi.community.business.RecruitmentPostManagementItemResult
import java.time.LocalDate
import java.time.LocalDateTime

data class RecruitmentPostFormResponse(
    val postId: Long,
    val status: RecruitmentPostManagementStatus,
    val recruitmentStatus: RecruitmentStatus,
    val title: String,
    val recruitmentType: RecruitmentType?,
    val capacity: Int?,
    val progressMethod: ProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: JsonNode?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPosition>,
    val contactMethod: ContactMethod?,
    val contactValue: String?,
    val agreedToPolicy: Boolean,
) {
    companion object {
        fun from(result: RecruitmentPostFormResult, objectMapper: ObjectMapper): RecruitmentPostFormResponse =
            RecruitmentPostFormResponse(
                postId = result.postId,
                status = result.status,
                recruitmentStatus = result.recruitmentStatus,
                title = result.title,
                recruitmentType = result.recruitmentType,
                capacity = result.capacity,
                progressMethod = result.progressMethod,
                activityDurationMonths = result.activityDurationMonths,
                technologyStacks = result.technologyStacks,
                summary = result.summary,
                content = result.content?.let(objectMapper::readTree),
                eligibilityAndSelectionProcess = result.eligibilityAndSelectionProcess,
                recruitmentStartDate = result.recruitmentStartDate,
                recruitmentEndDate = result.recruitmentEndDate,
                positions = result.positions,
                contactMethod = result.contactMethod,
                contactValue = result.contactValue,
                agreedToPolicy = result.agreedToPolicy,
            )
    }
}

data class RecruitmentPostManagementItemResponse(
    val postId: Long,
    val status: RecruitmentPostManagementStatus,
    val title: String,
    val recruitmentType: RecruitmentType?,
    val progressMethod: ProgressMethod?,
    val activityDurationMonths: Int?,
    val recruitmentStatus: RecruitmentStatus?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val applicationCount: Long,
    val capacity: Int?,
    val viewCount: Long,
    val commentCount: Long,
    val lastSavedAt: LocalDateTime,
    val continueWriting: Boolean,
) {
    companion object {
        fun from(result: RecruitmentPostManagementItemResult): RecruitmentPostManagementItemResponse =
            RecruitmentPostManagementItemResponse(
                postId = result.postId,
                status = result.status,
                title = result.title,
                recruitmentType = result.recruitmentType,
                progressMethod = result.progressMethod,
                activityDurationMonths = result.activityDurationMonths,
                recruitmentStatus = result.recruitmentStatus,
                recruitmentStartDate = result.recruitmentStartDate,
                recruitmentEndDate = result.recruitmentEndDate,
                applicationCount = result.applicationCount,
                capacity = result.capacity,
                viewCount = result.viewCount,
                commentCount = result.commentCount,
                lastSavedAt = result.lastSavedAt,
                continueWriting = result.continueWriting,
            )
    }
}
