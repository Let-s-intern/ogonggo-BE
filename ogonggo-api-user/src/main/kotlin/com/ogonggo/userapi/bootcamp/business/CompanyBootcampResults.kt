package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampCurriculumData
import com.ogonggo.core.bootcamp.implement.BootcampPage
import com.ogonggo.core.bootcamp.implement.BootcampPartnerData
import java.time.LocalDate
import java.time.LocalDateTime

data class CompanyBootcampPageResult(
    val items: List<CompanyBootcampSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: BootcampPage): CompanyBootcampPageResult = CompanyBootcampPageResult(
            items = result.bootcamps.map(CompanyBootcampSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class CompanyBootcampSummary(
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
        internal fun from(bootcamp: Bootcamp): CompanyBootcampSummary = CompanyBootcampSummary(
            id = bootcamp.requiredId(),
            companyName = bootcamp.companyName,
            title = bootcamp.title,
            programType = bootcamp.programType,
            operationType = bootcamp.operationType,
            recruitmentType = bootcamp.recruitmentType,
            recruitmentStartAt = bootcamp.recruitmentStartAt,
            recruitmentEndAt = bootcamp.recruitmentEndAt,
            programStartDate = bootcamp.programStartDate,
            programEndDate = bootcamp.programEndDate,
            capacity = bootcamp.capacity,
            tuitionType = bootcamp.tuitionType,
            tuitionAmount = bootcamp.tuitionAmount,
            representativeImageUrl = bootcamp.representativeImageUrl,
            shortDescription = bootcamp.shortDescription,
            status = bootcamp.status,
            closedAt = bootcamp.closedAt,
        )
    }
}

data class CompanyBootcampResult(
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
    val partners: List<UserBootcampPartnerResult>,
    val curriculums: List<UserBootcampCurriculumResult>,
) {
    companion object {
        internal fun from(
            bootcamp: Bootcamp,
            partners: List<BootcampPartnerData>,
            curriculums: List<BootcampCurriculumData>,
        ): CompanyBootcampResult = CompanyBootcampResult(
            id = bootcamp.requiredId(),
            companyName = bootcamp.companyName,
            title = bootcamp.title,
            programType = bootcamp.programType,
            operationType = bootcamp.operationType,
            recruitmentType = bootcamp.recruitmentType,
            recruitmentStartAt = bootcamp.recruitmentStartAt,
            recruitmentEndAt = bootcamp.recruitmentEndAt,
            programStartDate = bootcamp.programStartDate,
            programEndDate = bootcamp.programEndDate,
            capacity = bootcamp.capacity,
            tuitionType = bootcamp.tuitionType,
            tuitionAmount = bootcamp.tuitionAmount,
            representativeImageUrl = bootcamp.representativeImageUrl,
            shortDescription = bootcamp.shortDescription,
            content = bootcamp.content,
            eligibilityAndSelectionProcess = bootcamp.eligibilityAndSelectionProcess,
            applicationMethod = bootcamp.applicationMethod,
            applicationUrl = bootcamp.applicationUrl,
            managerEmail = bootcamp.managerEmail,
            inquiryUrl = bootcamp.inquiryUrl,
            publicationStartAt = bootcamp.publicationStartAt,
            publicationEndAt = bootcamp.publicationEndAt,
            sourceUrl = bootcamp.sourceUrl,
            status = bootcamp.status,
            closedAt = bootcamp.closedAt,
            partners = partners.map { UserBootcampPartnerResult(it.name, it.displayOrder) },
            curriculums = curriculums.map {
                UserBootcampCurriculumResult(it.startWeek, it.endWeek, it.subtitle, it.displayOrder)
            },
        )
    }
}

internal fun Bootcamp.requiredId(): Long = checkNotNull(id) { "부트캠프 식별자가 없습니다." }
