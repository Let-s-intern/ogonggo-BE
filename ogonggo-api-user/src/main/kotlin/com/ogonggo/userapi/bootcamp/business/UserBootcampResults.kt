package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampCurriculumData
import com.ogonggo.core.bootcamp.implement.BootcampMetricData
import com.ogonggo.core.bootcamp.implement.BootcampPage
import com.ogonggo.core.bootcamp.implement.BootcampPartnerData
import java.time.LocalDate
import java.time.LocalDateTime

data class UserBootcampPageResult(
    val items: List<UserBootcampSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        internal fun from(
            result: BootcampPage,
            metrics: Map<Long, BootcampMetricData>,
        ): UserBootcampPageResult = UserBootcampPageResult(
            items = result.bootcamps.map { bootcamp ->
                UserBootcampSummary.from(
                    bootcamp = bootcamp,
                    metric = metrics[bootcamp.requiredId()] ?: BootcampMetricData.EMPTY,
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext,
        )
    }
}

data class UserBootcampSummary(
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
        internal fun from(bootcamp: Bootcamp, metric: BootcampMetricData): UserBootcampSummary = UserBootcampSummary(
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
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
        )
    }
}

data class UserBootcampResult(
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
    val partners: List<UserBootcampPartnerResult>,
    val curriculums: List<UserBootcampCurriculumResult>,
) {
    companion object {
        internal fun from(
            bootcamp: Bootcamp,
            partners: List<BootcampPartnerData>,
            curriculums: List<BootcampCurriculumData>,
            metric: BootcampMetricData,
        ): UserBootcampResult = UserBootcampResult(
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
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
            partners = partners.map { UserBootcampPartnerResult(it.name, it.displayOrder) },
            curriculums = curriculums.map {
                UserBootcampCurriculumResult(it.startWeek, it.endWeek, it.subtitle, it.displayOrder)
            },
        )
    }
}

data class UserBootcampPartnerResult(
    val name: String,
    val displayOrder: Int,
)

data class UserBootcampCurriculumResult(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
    val displayOrder: Int,
)
