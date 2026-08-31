package com.ogonggo.userapi.bootcamp.presentation.response

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.business.CompanyBootcampResult
import com.ogonggo.userapi.bootcamp.business.CompanyBootcampSummary
import com.ogonggo.userapi.bootcamp.business.UserBootcampCurriculumResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampPartnerResult
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateCompanyBootcampResponse(val id: Long)

data class CompanyBootcampSummaryResponse(
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
) {
    companion object {
        internal fun from(result: CompanyBootcampSummary): CompanyBootcampSummaryResponse =
            CompanyBootcampSummaryResponse(
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
            )
    }
}

data class CompanyBootcampDetailResponse(
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
    val partners: List<CompanyBootcampPartnerResponse>,
    val curriculums: List<CompanyBootcampCurriculumResponse>,
) {
    companion object {
        internal fun from(result: CompanyBootcampResult): CompanyBootcampDetailResponse =
            CompanyBootcampDetailResponse(
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
                partners = result.partners.map(CompanyBootcampPartnerResponse::from),
                curriculums = result.curriculums.map(CompanyBootcampCurriculumResponse::from),
            )
    }
}

data class CompanyBootcampPartnerResponse(val partnerName: String, val displayOrder: Int) {
    companion object {
        internal fun from(result: UserBootcampPartnerResult): CompanyBootcampPartnerResponse =
            CompanyBootcampPartnerResponse(result.name, result.displayOrder)
    }
}

data class CompanyBootcampCurriculumResponse(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(result: UserBootcampCurriculumResult): CompanyBootcampCurriculumResponse =
            CompanyBootcampCurriculumResponse(
                result.startWeek,
                result.endWeek,
                result.subtitle,
                result.displayOrder,
            )
    }
}
