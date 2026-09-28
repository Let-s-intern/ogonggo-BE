package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampMetricDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPageDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminBootcampPageResult(
    val items: List<AdminBootcampSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: BootcampPageDto, metrics: Map<Long, BootcampMetricDto>): AdminBootcampPageResult =
            AdminBootcampPageResult(
                items = result.bootcamps.map { bootcamp ->
                    AdminBootcampSummary.from(bootcamp, metrics[bootcamp.requiredId()] ?: BootcampMetricDto.EMPTY)
                },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
    }
}

/** 채용공고 목록과 같은 운영 칸(노출·등록 경로·검수 상태·등록일)을 싣는다. 모집 상태만 저장된 값이다. */
data class AdminBootcampSummary(
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
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    val reviewStatus: ReviewStatus?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(bootcamp: Bootcamp, metric: BootcampMetricDto): AdminBootcampSummary = AdminBootcampSummary(
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
            visibility = AdminContentVisibility.of(bootcamp.publicationStatus == BootcampPublicationStatus.PUBLISHED),
            source = ContentSource.of(bootcamp.ownerUserId),
            reviewStatus = bootcamp.reviewStatus,
            registeredAt = bootcamp.createdAt,
        )
    }
}

data class AdminBootcampResult(
    val summary: AdminBootcampSummary,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val logoUrl: String?,
    val instructorInfo: String?,
    val programFeatures: String?,
    val completionRequirements: String?,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String?,
    val managerEmail: String?,
    val inquiryUrl: String?,
    val publicationStartAt: LocalDateTime?,
    val publicationEndAt: LocalDateTime?,
    val sourceUrl: String?,
    val partners: List<BootcampPartnerDto.Response>,
    val curriculums: List<BootcampCurriculumDto.Response>,
) {
    companion object {
        internal fun from(
            bootcamp: Bootcamp,
            metric: BootcampMetricDto,
            partners: List<BootcampPartnerDto.Response>,
            curriculums: List<BootcampCurriculumDto.Response>,
        ): AdminBootcampResult = AdminBootcampResult(
            summary = AdminBootcampSummary.from(bootcamp, metric),
            content = bootcamp.content,
            eligibilityAndSelectionProcess = bootcamp.eligibilityAndSelectionProcess,
            logoUrl = bootcamp.logoUrl,
            instructorInfo = bootcamp.instructorInfo,
            programFeatures = bootcamp.programFeatures,
            completionRequirements = bootcamp.completionRequirements,
            applicationMethod = bootcamp.applicationMethod,
            applicationUrl = bootcamp.applicationUrl,
            managerEmail = bootcamp.managerEmail,
            inquiryUrl = bootcamp.inquiryUrl,
            publicationStartAt = bootcamp.publicationStartAt,
            publicationEndAt = bootcamp.publicationEndAt,
            sourceUrl = bootcamp.sourceUrl,
            partners = partners,
            curriculums = curriculums,
        )
    }
}

internal fun Bootcamp.requiredId(): Long = checkNotNull(id) { "부트캠프 식별자가 없습니다." }
