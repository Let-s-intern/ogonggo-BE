package com.ogonggo.userapi.bootcamp.presentation.response

import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.userapi.bootcamp.business.UserBootcampCurriculumResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampImageResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampPartnerResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class UserBootcampSummaryResponse(
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
    @Schema(description = "운영 회사 로고 이미지 주소. 없으면 null")
    val logoUrl: String?,
    val shortDescription: String,
    val status: BootcampRecruitmentStatus,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
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
            logoUrl = result.logoUrl,
            shortDescription = result.shortDescription,
            status = result.status,
            closedAt = result.closedAt,
            bookmarked = result.bookmarked,
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
    val status: BootcampRecruitmentStatus,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val partners: List<UserBootcampPartnerResponse>,
    val curriculums: List<UserBootcampCurriculumResponse>,
    @Schema(description = "상세에서만 보여 주는 사진입니다. 고용24에서 수집한 과정은 훈련기관 시설 사진이 들어 있고, 그 밖에는 빈 배열입니다.")
    val images: List<UserBootcampImageResponse>,
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
            status = result.status,
            closedAt = result.closedAt,
            bookmarked = result.bookmarked,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
            partners = result.partners.map(UserBootcampPartnerResponse::from),
            curriculums = result.curriculums.map(UserBootcampCurriculumResponse::from),
            images = result.images.map(UserBootcampImageResponse::from),
        )
    }
}

data class UserBootcampImageResponse(
    val url: String,
    @Schema(description = "사진 설명입니다. 예: 강의실, 안내데스크", nullable = true)
    val caption: String?,
    val displayOrder: Int,
) {
    companion object {
        internal fun from(result: UserBootcampImageResult): UserBootcampImageResponse =
            UserBootcampImageResponse(url = result.url, caption = result.caption, displayOrder = result.displayOrder)
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
