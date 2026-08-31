package com.ogonggo.userapi.bootcamp.presentation.request

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampAppendCommand
import com.ogonggo.core.bootcamp.implement.BootcampCurriculumCommand
import com.ogonggo.core.bootcamp.implement.BootcampPartnerCommand
import com.ogonggo.core.bootcamp.implement.BootcampUpdateCommand
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
    override val operationType: OperationType,
    override val recruitmentType: BootcampRecruitmentType,
    override val recruitmentStartAt: LocalDateTime?,
    override val recruitmentEndAt: LocalDateTime?,
    override val programStartDate: LocalDate,
    override val programEndDate: LocalDate,
    @field:PositiveOrZero override val capacity: Int?,
    override val tuitionType: TuitionType,
    @field:PositiveOrZero override val tuitionAmount: Long?,
    @field:NotBlank @field:Size(max = 2048) @field:URL override val representativeImageUrl: String,
    @field:NotBlank @field:Size(max = 500) override val shortDescription: String,
    @field:NotBlank override val content: String,
    override val eligibilityAndSelectionProcess: String?,
    override val applicationMethod: ApplicationMethod,
    @field:Size(max = 2048) @field:URL override val applicationUrl: String?,
    @field:Size(max = 320) @field:Email override val managerEmail: String?,
    @field:Size(max = 2048) @field:URL override val inquiryUrl: String?,
    override val publicationStartAt: LocalDateTime?,
    override val publicationEndAt: LocalDateTime?,
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
    @field:Valid @field:Size(max = 100) override val partners: List<CompanyBootcampPartnerRequest>,
    @field:Valid @field:Size(max = 100) override val curriculums: List<CompanyBootcampCurriculumRequest>,
    val status: BootcampStatus,
) : CompanyBootcampWriteRequest {
    fun toCommand(): BootcampAppendCommand {
        validateRelations()
        return BootcampAppendCommand(
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
        )
    }
}

data class UpdateCompanyBootcampRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:NotBlank @field:Size(max = 50) override val programType: String,
    override val operationType: OperationType,
    override val recruitmentType: BootcampRecruitmentType,
    override val recruitmentStartAt: LocalDateTime?,
    override val recruitmentEndAt: LocalDateTime?,
    override val programStartDate: LocalDate,
    override val programEndDate: LocalDate,
    @field:PositiveOrZero override val capacity: Int?,
    override val tuitionType: TuitionType,
    @field:PositiveOrZero override val tuitionAmount: Long?,
    @field:NotBlank @field:Size(max = 2048) @field:URL override val representativeImageUrl: String,
    @field:NotBlank @field:Size(max = 500) override val shortDescription: String,
    @field:NotBlank override val content: String,
    override val eligibilityAndSelectionProcess: String?,
    override val applicationMethod: ApplicationMethod,
    @field:Size(max = 2048) @field:URL override val applicationUrl: String?,
    @field:Size(max = 320) @field:Email override val managerEmail: String?,
    @field:Size(max = 2048) @field:URL override val inquiryUrl: String?,
    override val publicationStartAt: LocalDateTime?,
    override val publicationEndAt: LocalDateTime?,
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
    @field:Valid @field:Size(max = 100) override val partners: List<CompanyBootcampPartnerRequest>,
    @field:Valid @field:Size(max = 100) override val curriculums: List<CompanyBootcampCurriculumRequest>,
) : CompanyBootcampWriteRequest {
    fun toCommand(): BootcampUpdateCommand {
        validateRelations()
        return BootcampUpdateCommand(
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
            applicationMethod = applicationMethod,
            applicationUrl = applicationUrl,
            managerEmail = managerEmail,
            inquiryUrl = inquiryUrl,
            publicationStartAt = publicationStartAt,
            publicationEndAt = publicationEndAt,
            sourceUrl = sourceUrl,
            partners = partners.map(CompanyBootcampPartnerRequest::toCommand),
            curriculums = curriculums.map(CompanyBootcampCurriculumRequest::toCommand),
        )
    }
}

data class CompanyBootcampPartnerRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val partnerName: String,
    @field:PositiveOrZero
    val displayOrder: Int,
) {
    fun toCommand(): BootcampPartnerCommand = BootcampPartnerCommand(partnerName, displayOrder)
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
    fun toCommand(): BootcampCurriculumCommand =
        BootcampCurriculumCommand(startWeek, endWeek, subtitle, displayOrder)
}

private interface CompanyBootcampWriteRequest {
    val companyName: String
    val title: String
    val programType: String
    val operationType: OperationType
    val recruitmentType: BootcampRecruitmentType
    val recruitmentStartAt: LocalDateTime?
    val recruitmentEndAt: LocalDateTime?
    val programStartDate: LocalDate
    val programEndDate: LocalDate
    val capacity: Int?
    val tuitionType: TuitionType
    val tuitionAmount: Long?
    val representativeImageUrl: String
    val shortDescription: String
    val content: String
    val eligibilityAndSelectionProcess: String?
    val applicationMethod: ApplicationMethod
    val applicationUrl: String?
    val managerEmail: String?
    val inquiryUrl: String?
    val publicationStartAt: LocalDateTime?
    val publicationEndAt: LocalDateTime?
    val sourceUrl: String?
    val partners: List<CompanyBootcampPartnerRequest>
    val curriculums: List<CompanyBootcampCurriculumRequest>
}

private fun CompanyBootcampWriteRequest.validateRelations() {
    val recruitmentStart = recruitmentStartAt
    val recruitmentEnd = recruitmentEndAt
    val publicationStart = publicationStartAt
    val publicationEnd = publicationEndAt
    if (recruitmentType == BootcampRecruitmentType.PERIOD && recruitmentStart == null) {
        invalid("recruitmentStartAt", "기간 모집의 시작 일시는 필수입니다.")
    }
    if (recruitmentType == BootcampRecruitmentType.PERIOD && recruitmentEnd == null) {
        invalid("recruitmentEndAt", "기간 모집의 종료 일시는 필수입니다.")
    }
    if (recruitmentStart != null && recruitmentEnd != null && recruitmentStart.isAfter(recruitmentEnd)) {
        invalid("recruitmentStartAt", "모집 종료 일시보다 늦을 수 없습니다.")
    }
    if (programStartDate.isAfter(programEndDate)) {
        invalid("programStartDate", "교육 종료일보다 늦을 수 없습니다.")
    }
    if (publicationStart != null && publicationEnd != null && publicationStart.isAfter(publicationEnd)) {
        invalid("publicationStartAt", "공개 종료 일시보다 늦을 수 없습니다.")
    }
    if (applicationMethod == ApplicationMethod.EXTERNAL_PAGE && applicationUrl.isNullOrBlank()) {
        invalid("applicationUrl", "외부 페이지 지원 링크는 필수입니다.")
    }
    if (applicationMethod == ApplicationMethod.EMAIL && applicationUrl != null) {
        invalid("applicationUrl", "이메일 지원에는 외부 지원 링크를 설정할 수 없습니다.")
    }
    optionalTextValues().firstOrNull { (_, value) -> value != null && value.isBlank() }?.let { (field, _) ->
        invalid(field, "공백일 수 없습니다.")
    }
    if (partners.map { it.partnerName }.distinct().size != partners.size) {
        invalid("partners", "중복된 파트너사명은 등록할 수 없습니다.")
    }
    curriculums.forEachIndexed { index, curriculum ->
        if (curriculum.startWeek > curriculum.endWeek) {
            invalid("curriculums[$index].startWeek", "종료 주차보다 클 수 없습니다.")
        }
    }
}

private fun CompanyBootcampWriteRequest.optionalTextValues(): List<Pair<String, String?>> = listOf(
    "eligibilityAndSelectionProcess" to eligibilityAndSelectionProcess,
    "managerEmail" to managerEmail,
    "inquiryUrl" to inquiryUrl,
    "sourceUrl" to sourceUrl,
)

private fun invalid(field: String, reason: String): Nothing = throw InvalidRequestFieldException(field, reason)
