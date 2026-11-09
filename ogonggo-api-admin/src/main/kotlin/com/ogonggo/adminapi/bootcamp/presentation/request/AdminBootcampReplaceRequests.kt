package com.ogonggo.adminapi.bootcamp.presentation.request

import com.ogonggo.adminapi.bootcamp.business.AdminBootcampReplaceCommand
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampWriteValidator
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 부트캠프 대리 등록([CreateProxyBootcampRequest])과 같은 칸으로 부트캠프 전체를 바꾼다. 노출은 받지 않는다.
 * 대표 이미지는 선택이다. 수집 부트캠프에는 대표 이미지가 없는 과정이 있어 필수로 두면 고칠 수 없다.
 */
data class ReplaceAdminBootcampRequest(
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
    @field:Size(max = 2048) @field:URL val representativeImageUrl: String?,
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
    @field:Schema(description = "모집 상태입니다. RECRUITING 또는 CLOSED만 받습니다. 마감에서 모집 중으로 되돌리면 마감 일시를 지웁니다.")
    val status: BootcampRecruitmentStatus,
) {
    fun toCommand(): AdminBootcampReplaceCommand {
        if (status == BootcampRecruitmentStatus.DRAFT) {
            throw InvalidRequestFieldException("status", "임시저장으로는 바꿀 수 없습니다.")
        }
        if (representativeImageUrl != null && representativeImageUrl.isBlank()) {
            throw InvalidRequestFieldException("representativeImageUrl", "공백일 수 없습니다.")
        }
        val bootcamp = BootcampUpdateDto(
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
        ).also { BootcampWriteValidator.validate(it) { field, reason -> throw InvalidRequestFieldException(field, reason) } }
        return AdminBootcampReplaceCommand(bootcamp = bootcamp, status = status)
    }
}
