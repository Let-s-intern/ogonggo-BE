package com.ogonggo.adminapi.bootcamp.presentation.response

import com.ogonggo.adminapi.bootcamp.business.AdminBootcampResult
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampSummary
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampImageDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ContentReviewStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminBootcampSummaryResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: BootcampOperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int?,
    val tuitionType: BootcampTuitionType,
    val tuitionAmount: Long?,
    @Schema(description = "고용24에서 수집한 과정은 이미지가 없어 null입니다. 클라이언트가 기본 이미지를 보여 줍니다.")
    val representativeImageUrl: String?,
    val shortDescription: String,
    @Schema(description = "모집 상태입니다. 콘솔에서는 RECRUITING·CLOSED만 다룹니다.")
    val status: BootcampRecruitmentStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링·고용24 수집분은 null입니다.")
    val reviewStatus: ContentReviewStatus?,
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
    val operationType: BootcampOperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int?,
    val tuitionType: BootcampTuitionType,
    val tuitionAmount: Long?,
    @Schema(description = "고용24에서 수집한 과정은 이미지가 없어 null입니다. 클라이언트가 기본 이미지를 보여 줍니다.")
    val representativeImageUrl: String?,
    val shortDescription: String,
    @Schema(description = "모집 상태입니다. 콘솔에서는 RECRUITING·CLOSED만 다룹니다.")
    val status: BootcampRecruitmentStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링·고용24 수집분은 null입니다.")
    val reviewStatus: ContentReviewStatus?,
    val registeredAt: LocalDateTime,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val logoUrl: String?,
    val instructorInfo: String?,
    val programFeatures: String?,
    val completionRequirements: String?,
    val applicationMethod: BootcampApplicationMethod,
    val applicationUrl: String?,
    val managerEmail: String?,
    val inquiryUrl: String?,
    val publicationStartAt: LocalDateTime?,
    val publicationEndAt: LocalDateTime?,
    val sourceUrl: String?,
    val partners: List<AdminBootcampPartnerResponse>,
    val curriculums: List<AdminBootcampCurriculumResponse>,
    @Schema(description = "상세에서만 보여 주는 사진입니다. 고용24에서 수집한 과정은 훈련기관 시설 사진이 들어 있고, 그 밖에는 빈 배열입니다.")
    val images: List<AdminBootcampImageResponse>,
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
                logoUrl = result.logoUrl,
                instructorInfo = result.instructorInfo,
                programFeatures = result.programFeatures,
                completionRequirements = result.completionRequirements,
                applicationMethod = result.applicationMethod,
                applicationUrl = result.applicationUrl,
                managerEmail = result.managerEmail,
                inquiryUrl = result.inquiryUrl,
                publicationStartAt = result.publicationStartAt,
                publicationEndAt = result.publicationEndAt,
                sourceUrl = result.sourceUrl,
                partners = result.partners.map(AdminBootcampPartnerResponse::from),
                curriculums = result.curriculums.map(AdminBootcampCurriculumResponse::from),
                images = result.images.map(AdminBootcampImageResponse::from),
            )
        }
    }
}

data class AdminBootcampImageResponse(
    val url: String,
    @Schema(description = "사진 설명입니다. 예: 강의실, 안내데스크", nullable = true)
    val caption: String?,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(image: BootcampImageDto.Response): AdminBootcampImageResponse =
            AdminBootcampImageResponse(url = image.url, caption = image.caption, displayOrder = image.displayOrder)
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
