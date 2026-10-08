package com.ogonggo.core.bootcamp.implement.dto

import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.contentreview.domain.ContentSource
import java.time.LocalDate
import java.time.LocalDateTime

data class BootcampAppendDto(
    val ownerUserId: Long? = null,
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: BootcampOperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int? = null,
    val tuitionType: BootcampTuitionType,
    val tuitionAmount: Long? = null,
    val representativeImageUrl: String?,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String? = null,
    val logoUrl: String? = null,
    val instructorInfo: String? = null,
    val programFeatures: String? = null,
    val completionRequirements: String? = null,
    val applicationMethod: BootcampApplicationMethod,
    val applicationUrl: String? = null,
    val managerEmail: String? = null,
    val inquiryUrl: String? = null,
    val publicationStartAt: LocalDateTime? = null,
    val publicationEndAt: LocalDateTime? = null,
    val sourceUrl: String? = null,
    val partners: List<BootcampPartnerDto.Request> = emptyList(),
    val curriculums: List<BootcampCurriculumDto.Request> = emptyList(),
    /** 상세에서만 보여 주는 사진이다. 고용24 수집만 넣으며, 수정([BootcampUpdateDto])으로는 바뀌지 않는다. */
    val images: List<BootcampImageDto.Request> = emptyList(),
    val status: BootcampRecruitmentStatus = BootcampRecruitmentStatus.DRAFT,
    val closedAt: LocalDateTime? = null,
    val publicationStatus: BootcampPublicationStatus = BootcampPublicationStatus.DRAFT,
    /** 비우면 저장할 때 소유자 유무로 정한다. `copy(ownerUserId = ...)`로 만든 값도 소유자와 어긋나지 않게 하기 위해서다. */
    val source: ContentSource? = null,
    val externalId: String? = null,
    /** 기업과 함께 운영하는 과정이면 공개 목록에서 앞에 둔다. */
    val enterpriseLinked: Boolean = false,
)

/** 운영자가 고치는 제목과 본문 칸이다. 넘어온 칸만 바꾸며 본문 값이 null이면 그 칸을 비운다. */
data class BootcampContentEditDto(
    val title: String? = null,
    val contents: Map<BootcampContentField, String?> = emptyMap(),
)

data class BootcampUpdateDto(
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: BootcampOperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int? = null,
    val tuitionType: BootcampTuitionType,
    val tuitionAmount: Long? = null,
    val representativeImageUrl: String?,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String? = null,
    val logoUrl: String? = null,
    val instructorInfo: String? = null,
    val programFeatures: String? = null,
    val completionRequirements: String? = null,
    val applicationMethod: BootcampApplicationMethod,
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

/** 부트캠프 상세에서만 보여 주는 사진. 등록에 넣는 값과 조회로 받는 값이 같아도 다른 하위 항목과 모양을 맞춘다. */
object BootcampImageDto {
    data class Request(
        val url: String,
        val caption: String? = null,
        val displayOrder: Int = 0,
    )

    data class Response(
        val url: String,
        val caption: String?,
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
