package com.ogonggo.adminapi.bootcamp.presentation.request

import com.ogonggo.adminapi.bootcamp.business.AdminProxyBootcampCreateCommand
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampWriteValidator
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 기업회원 부트캠프 등록(사용자 API `CreateCompanyBootcampRequest`)과 같은 칸과 검증에 노출 여부를 더한다.
 * 칸 사이 규칙은 core의 [BootcampWriteValidator]를 함께 쓴다. 칸을 바꾸면 기업회원 등록 요청도 함께 바꾼다.
 */
data class CreateProxyBootcampRequest(
    @field:NotBlank @field:Size(max = 150) val companyName: String,
    @field:NotBlank @field:Size(max = 255) val title: String,
    @field:NotBlank @field:Size(max = 50) val programType: String,
    val operationType: BootcampOperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    @field:PositiveOrZero val capacity: Int?,
    val tuitionType: BootcampTuitionType,
    @field:PositiveOrZero val tuitionAmount: Long?,
    @field:NotBlank @field:Size(max = 2048) @field:URL val representativeImageUrl: String,
    @field:NotBlank @field:Size(max = 500) val shortDescription: String,
    @field:NotBlank val content: String,
    val eligibilityAndSelectionProcess: String?,
    @field:Size(max = 2048) @field:URL val logoUrl: String?,
    val instructorInfo: String?,
    val programFeatures: String?,
    val completionRequirements: String?,
    val applicationMethod: BootcampApplicationMethod,
    @field:Size(max = 2048) @field:URL val applicationUrl: String?,
    @field:Size(max = 320) @field:Email val managerEmail: String?,
    @field:Size(max = 2048) @field:URL val inquiryUrl: String?,
    val publicationStartAt: LocalDateTime?,
    val publicationEndAt: LocalDateTime?,
    @field:Size(max = 2048) @field:URL val sourceUrl: String?,
    @field:Valid @field:Size(max = 100) val partners: List<CreateProxyBootcampPartnerRequest>,
    @field:Valid @field:Size(max = 100) val curriculums: List<CreateProxyBootcampCurriculumRequest>,
    val status: BootcampRecruitmentStatus,
    @field:Schema(description = "등록하면서 사용자 화면에 노출할지입니다. 보내지 않으면 VISIBLE입니다. 검수 없이 승인으로 등록되므로 VISIBLE이면 모집 상태와 공개 기간이 맞을 때 바로 보입니다.")
    val visibility: AdminContentVisibility?,
) {
    fun toCommand(): AdminProxyBootcampCreateCommand = AdminProxyBootcampCreateCommand(
        bootcamp = BootcampAppendDto(
            companyName = companyName,
            title = title,
            programType = programType,
            operationType = operationType,
            recruitmentType = recruitmentType,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = capacity,
            tuitionType = tuitionType,
            tuitionAmount = tuitionAmount,
            representativeImageUrl = representativeImageUrl,
            shortDescription = shortDescription,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            logoUrl = logoUrl,
            instructorInfo = instructorInfo,
            programFeatures = programFeatures,
            completionRequirements = completionRequirements,
            applicationMethod = applicationMethod,
            applicationUrl = applicationUrl,
            managerEmail = managerEmail,
            inquiryUrl = inquiryUrl,
            publicationStartAt = publicationStartAt,
            publicationEndAt = publicationEndAt,
            sourceUrl = sourceUrl,
            partners = partners.map(CreateProxyBootcampPartnerRequest::toCommand),
            curriculums = curriculums.map(CreateProxyBootcampCurriculumRequest::toCommand),
            status = status,
        ).also { BootcampWriteValidator.validate(it) { field, reason -> throw InvalidRequestFieldException(field, reason) } },
        visibility = visibility ?: AdminContentVisibility.VISIBLE,
    )
}

data class CreateProxyBootcampPartnerRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val partnerName: String,
    @field:PositiveOrZero
    val displayOrder: Int,
) {
    fun toCommand(): BootcampPartnerDto.Request = BootcampPartnerDto.Request(partnerName, displayOrder)
}

data class CreateProxyBootcampCurriculumRequest(
    @field:Positive
    val startWeek: Int,
    @field:Positive
    val endWeek: Int,
    @field:NotBlank
    @field:Size(max = 255)
    val subtitle: String,
    @field:PositiveOrZero
    val displayOrder: Int,
) {
    fun toCommand(): BootcampCurriculumDto.Request =
        BootcampCurriculumDto.Request(startWeek, endWeek, subtitle, displayOrder)
}
