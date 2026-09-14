package com.ogonggo.adminapi.bootcamp.presentation.response

import com.ogonggo.adminapi.bootcamp.business.AdminBootcampResult
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampSummary
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminBootcampSummaryResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int?,
    val tuitionType: TuitionType,
    val tuitionAmount: Long?,
    val representativeImageUrl: String,
    val shortDescription: String,
    @Schema(description = "모집 상태입니다. 콘솔에서는 RECRUITING·CLOSED만 다룹니다.")
    val status: BootcampStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링 수집분은 null입니다.")
    val reviewStatus: ReviewStatus?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminBootcampSummary): AdminBootcampSummaryResponse = AdminBootcampSummaryResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            programType = result.programType,
            operationType = result.operationType,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            programStartDate = result.programStartDate,
            programEndDate = result.programEndDate,
            capacity = result.capacity,
            tuitionType = result.tuitionType,
            tuitionAmount = result.tuitionAmount,
            representativeImageUrl = result.representativeImageUrl,
            shortDescription = result.shortDescription,
            status = result.status,
            closedAt = result.closedAt,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
            visibility = result.visibility,
            source = result.source,
            reviewStatus = result.reviewStatus,
            registeredAt = result.registeredAt,
        )
    }
}

data class AdminBootcampDetailResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int?,
    val tuitionType: TuitionType,
    val tuitionAmount: Long?,
    val representativeImageUrl: String,
    val shortDescription: String,
    @Schema(description = "모집 상태입니다. 콘솔에서는 RECRUITING·CLOSED만 다룹니다.")
    val status: BootcampStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링 수집분은 null입니다.")
    val reviewStatus: ReviewStatus?,
    val registeredAt: LocalDateTime,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String?,
    val managerEmail: String?,
    val inquiryUrl: String?,
    val publicationStartAt: LocalDateTime?,
    val publicationEndAt: LocalDateTime?,
    val sourceUrl: String?,
    val partners: List<AdminBootcampPartnerResponse>,
    val curriculums: List<AdminBootcampCurriculumResponse>,
) {
    companion object {
        internal fun from(result: AdminBootcampResult): AdminBootcampDetailResponse {
            val summary = result.summary
            return AdminBootcampDetailResponse(
                id = summary.id,
                companyName = summary.companyName,
                title = summary.title,
                programType = summary.programType,
                operationType = summary.operationType,
                recruitmentType = summary.recruitmentType,
                recruitmentStartAt = summary.recruitmentStartAt,
                recruitmentEndAt = summary.recruitmentEndAt,
                programStartDate = summary.programStartDate,
                programEndDate = summary.programEndDate,
                capacity = summary.capacity,
                tuitionType = summary.tuitionType,
                tuitionAmount = summary.tuitionAmount,
                representativeImageUrl = summary.representativeImageUrl,
                shortDescription = summary.shortDescription,
                status = summary.status,
                closedAt = summary.closedAt,
                viewCount = summary.viewCount,
                bookmarkCount = summary.bookmarkCount,
                commentCount = summary.commentCount,
                visibility = summary.visibility,
                source = summary.source,
                reviewStatus = summary.reviewStatus,
                registeredAt = summary.registeredAt,
                content = result.content,
                eligibilityAndSelectionProcess = result.eligibilityAndSelectionProcess,
                applicationMethod = result.applicationMethod,
                applicationUrl = result.applicationUrl,
                managerEmail = result.managerEmail,
                inquiryUrl = result.inquiryUrl,
                publicationStartAt = result.publicationStartAt,
                publicationEndAt = result.publicationEndAt,
                sourceUrl = result.sourceUrl,
                partners = result.partners.map(AdminBootcampPartnerResponse::from),
                curriculums = result.curriculums.map(AdminBootcampCurriculumResponse::from),
            )
        }
    }
}

data class AdminBootcampPartnerResponse(
    val name: String,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(partner: BootcampPartnerDto.Response): AdminBootcampPartnerResponse =
            AdminBootcampPartnerResponse(name = partner.name, displayOrder = partner.displayOrder)
    }
}

data class AdminBootcampCurriculumResponse(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(curriculum: BootcampCurriculumDto.Response): AdminBootcampCurriculumResponse =
            AdminBootcampCurriculumResponse(
                startWeek = curriculum.startWeek,
                endWeek = curriculum.endWeek,
                subtitle = curriculum.subtitle,
                displayOrder = curriculum.displayOrder,
            )
    }
}
