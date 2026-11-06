package com.ogonggo.userapi.bootcamp.presentation.request

import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampWriteValidator
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import com.ogonggo.userapi.error.InvalidRequestFieldException
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateCompanyBootcampRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:NotBlank @field:Size(max = 50) override val programType: String,
    override val operationType: BootcampOperationType,
    override val recruitmentType: BootcampRecruitmentType,
    override val recruitmentStartAt: LocalDateTime?,
    override val recruitmentEndAt: LocalDateTime?,
    override val programStartDate: LocalDate,
    override val programEndDate: LocalDate,
    @field:PositiveOrZero override val capacity: Int?,
    override val tuitionType: BootcampTuitionType,
    @field:PositiveOrZero override val tuitionAmount: Long?,
    @field:NotBlank @field:Size(max = 2048) @field:URL override val representativeImageUrl: String,
    @field:NotBlank @field:Size(max = 500) override val shortDescription: String,
    @field:NotBlank override val content: String,
    override val eligibilityAndSelectionProcess: String?,
    @field:Size(max = 2048) @field:URL override val logoUrl: String?,
    override val instructorInfo: String?,
    override val programFeatures: String?,
    override val completionRequirements: String?,
    override val applicationMethod: BootcampApplicationMethod,
    @field:Size(max = 2048) @field:URL override val applicationUrl: String?,
    @field:Size(max = 320) @field:Email override val managerEmail: String?,
    @field:Size(max = 2048) @field:URL override val inquiryUrl: String?,
    override val publicationStartAt: LocalDateTime?,
    override val publicationEndAt: LocalDateTime?,
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
    @field:Valid @field:Size(max = 100) override val partners: List<CompanyBootcampPartnerRequest>,
    @field:Valid @field:Size(max = 100) override val curriculums: List<CompanyBootcampCurriculumRequest>,
    val status: BootcampRecruitmentStatus,
) : CompanyBootcampWriteRequest {
    fun toCommand(): BootcampAppendDto {
        return BootcampAppendDto(
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
            partners = partners.map(CompanyBootcampPartnerRequest::toCommand),
            curriculums = curriculums.map(CompanyBootcampCurriculumRequest::toCommand),
            status = status,
        ).also { BootcampWriteValidator.validate(it, ::invalid) }
    }
}

data class UpdateCompanyBootcampRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:NotBlank @field:Size(max = 50) override val programType: String,
    override val operationType: BootcampOperationType,
    override val recruitmentType: BootcampRecruitmentType,
    override val recruitmentStartAt: LocalDateTime?,
    override val recruitmentEndAt: LocalDateTime?,
    override val programStartDate: LocalDate,
    override val programEndDate: LocalDate,
    @field:PositiveOrZero override val capacity: Int?,
    override val tuitionType: BootcampTuitionType,
    @field:PositiveOrZero override val tuitionAmount: Long?,
    @field:NotBlank @field:Size(max = 2048) @field:URL override val representativeImageUrl: String,
    @field:NotBlank @field:Size(max = 500) override val shortDescription: String,
    @field:NotBlank override val content: String,
    override val eligibilityAndSelectionProcess: String?,
    @field:Size(max = 2048) @field:URL override val logoUrl: String?,
    override val instructorInfo: String?,
    override val programFeatures: String?,
    override val completionRequirements: String?,
    override val applicationMethod: BootcampApplicationMethod,
    @field:Size(max = 2048) @field:URL override val applicationUrl: String?,
    @field:Size(max = 320) @field:Email override val managerEmail: String?,
    @field:Size(max = 2048) @field:URL override val inquiryUrl: String?,
    override val publicationStartAt: LocalDateTime?,
    override val publicationEndAt: LocalDateTime?,
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
    @field:Valid @field:Size(max = 100) override val partners: List<CompanyBootcampPartnerRequest>,
    @field:Valid @field:Size(max = 100) override val curriculums: List<CompanyBootcampCurriculumRequest>,
) : CompanyBootcampWriteRequest {
    fun toCommand(): BootcampUpdateDto {
        return BootcampUpdateDto(
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
            partners = partners.map(CompanyBootcampPartnerRequest::toCommand),
            curriculums = curriculums.map(CompanyBootcampCurriculumRequest::toCommand),
        ).also { BootcampWriteValidator.validate(it, ::invalid) }
    }
}

data class CompanyBootcampPartnerRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val partnerName: String,
    @field:PositiveOrZero
    val displayOrder: Int,
) {
    fun toCommand(): BootcampPartnerDto.Request = BootcampPartnerDto.Request(partnerName, displayOrder)
}

data class CompanyBootcampCurriculumRequest(
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

private interface CompanyBootcampWriteRequest {
    val companyName: String
    val title: String
    val programType: String
    val operationType: BootcampOperationType
    val recruitmentType: BootcampRecruitmentType
    val recruitmentStartAt: LocalDateTime?
    val recruitmentEndAt: LocalDateTime?
    val programStartDate: LocalDate
    val programEndDate: LocalDate
    val capacity: Int?
    val tuitionType: BootcampTuitionType
    val tuitionAmount: Long?
    val representativeImageUrl: String
    val shortDescription: String
    val content: String
    val eligibilityAndSelectionProcess: String?
    val logoUrl: String?
    val instructorInfo: String?
    val programFeatures: String?
    val completionRequirements: String?
    val applicationMethod: BootcampApplicationMethod
    val applicationUrl: String?
    val managerEmail: String?
    val inquiryUrl: String?
    val publicationStartAt: LocalDateTime?
    val publicationEndAt: LocalDateTime?
    val sourceUrl: String?
    val partners: List<CompanyBootcampPartnerRequest>
    val curriculums: List<CompanyBootcampCurriculumRequest>
}

/** 칸 사이 규칙은 관리자 대리 등록과 같이 core의 [BootcampWriteValidator]가 정한다. */
private fun invalid(field: String, reason: String): Nothing = throw InvalidRequestFieldException(field, reason)
