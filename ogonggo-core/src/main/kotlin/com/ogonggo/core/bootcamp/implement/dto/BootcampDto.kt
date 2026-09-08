package com.ogonggo.core.bootcamp.implement.dto

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import java.time.LocalDate
import java.time.LocalDateTime

data class BootcampAppendDto(
    val ownerUserId: Long? = null,
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int? = null,
    val tuitionType: TuitionType,
    val tuitionAmount: Long? = null,
    val representativeImageUrl: String,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String? = null,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String? = null,
    val managerEmail: String? = null,
    val inquiryUrl: String? = null,
    val publicationStartAt: LocalDateTime? = null,
    val publicationEndAt: LocalDateTime? = null,
    val sourceUrl: String? = null,
    val partners: List<BootcampPartnerDto.Request> = emptyList(),
    val curriculums: List<BootcampCurriculumDto.Request> = emptyList(),
    val status: BootcampStatus = BootcampStatus.DRAFT,
    val closedAt: LocalDateTime? = null,
)

data class BootcampUpdateDto(
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int? = null,
    val tuitionType: TuitionType,
    val tuitionAmount: Long? = null,
    val representativeImageUrl: String,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String? = null,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String? = null,
    val managerEmail: String? = null,
    val inquiryUrl: String? = null,
    val publicationStartAt: LocalDateTime? = null,
    val publicationEndAt: LocalDateTime? = null,
    val sourceUrl: String? = null,
    val partners: List<BootcampPartnerDto.Request> = emptyList(),
    val curriculums: List<BootcampCurriculumDto.Request> = emptyList(),
)

data class BootcampPageDto(
    val bootcamps: List<Bootcamp>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class BootcampMetricDto(
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        val EMPTY = BootcampMetricDto(viewCount = 0, bookmarkCount = 0, commentCount = 0)

        internal fun from(metric: BootcampMetric): BootcampMetricDto = BootcampMetricDto(
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
        )
    }
}

/** 부트캠프와 함께 다루는 파트너사. 등록·수정에 넣는 값과 조회로 받는 값의 모양이 다르다. */
object BootcampPartnerDto {
    data class Request(
        val partnerName: String,
        val displayOrder: Int = 0,
    )

    data class Response(
        val name: String,
        val displayOrder: Int,
    )
}

/** 부트캠프와 함께 다루는 커리큘럼. 등록·수정에 넣는 값과 조회로 받는 값의 모양이 다르다. */
object BootcampCurriculumDto {
    data class Request(
        val startWeek: Int,
        val endWeek: Int,
        val subtitle: String,
        val displayOrder: Int = 0,
    )

    data class Response(
        val startWeek: Int,
        val endWeek: Int,
        val subtitle: String,
        val displayOrder: Int,
    )
}
