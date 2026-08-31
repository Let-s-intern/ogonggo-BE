package com.ogonggo.userapi.bootcamp.presentation.response

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.business.UserBootcampCurriculumResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampPartnerResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import java.time.LocalDate
import java.time.LocalDateTime

data class UserBootcampSummaryResponse(
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
    val status: BootcampStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        internal fun from(result: UserBootcampSummary): UserBootcampSummaryResponse = UserBootcampSummaryResponse(
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
        )
    }
}

data class UserBootcampDetailResponse(
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
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String?,
    val managerEmail: String?,
    val inquiryUrl: String?,
    val publicationStartAt: LocalDateTime?,
    val publicationEndAt: LocalDateTime?,
    val sourceUrl: String?,
    val status: BootcampStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val partners: List<UserBootcampPartnerResponse>,
    val curriculums: List<UserBootcampCurriculumResponse>,
) {
    companion object {
        internal fun from(result: UserBootcampResult): UserBootcampDetailResponse = UserBootcampDetailResponse(
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
            content = result.content,
            eligibilityAndSelectionProcess = result.eligibilityAndSelectionProcess,
            applicationMethod = result.applicationMethod,
            applicationUrl = result.applicationUrl,
            managerEmail = result.managerEmail,
            inquiryUrl = result.inquiryUrl,
            publicationStartAt = result.publicationStartAt,
            publicationEndAt = result.publicationEndAt,
            sourceUrl = result.sourceUrl,
            status = result.status,
            closedAt = result.closedAt,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
            partners = result.partners.map(UserBootcampPartnerResponse::from),
            curriculums = result.curriculums.map(UserBootcampCurriculumResponse::from),
        )
    }
}

data class UserBootcampPartnerResponse(
    val name: String,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(result: UserBootcampPartnerResult): UserBootcampPartnerResponse =
            UserBootcampPartnerResponse(name = result.name, displayOrder = result.displayOrder)
    }
}

data class UserBootcampCurriculumResponse(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(result: UserBootcampCurriculumResult): UserBootcampCurriculumResponse =
            UserBootcampCurriculumResponse(
                startWeek = result.startWeek,
                endWeek = result.endWeek,
                subtitle = result.subtitle,
                displayOrder = result.displayOrder,
            )
    }
}
