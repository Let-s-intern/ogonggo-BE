package com.ogonggo.userapi.job.presentation.request

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.userapi.error.InvalidRequestFieldException
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDateTime

/**
 * 등록과 수정이 같은 필드를 받으므로 입력 계약을 한 곳에 모은다.
 * 게시 상태는 요청으로 받지 않는다. 등록은 항상 임시저장으로 만들고 게시는 별도 요청으로 처리한다.
 */
interface CompanyJobWriteRequest {
    val companyName: String
    val parentCompanyName: String?
    val title: String
    /** 직군. 직무·산업과 함께 사용자 프로필의 희망 직군·직무·산업과 짝을 이룬다. */
    val jobField: String?
    /** 비슷한 공고 추천에서 사용자의 희망 직무와 정확히 같은지 비교한다. */
    val jobRole: String?
    /** 비슷한 공고 추천에서 사용자의 희망 산업과 정확히 같은지 비교한다. */
    val industry: String?
    val coverImageUrl: String?
    /** 대표 이미지와 따로 보이는 기업 로고다. */
    val logoUrl: String?
    val employmentType: EmploymentType
    val experienceType: ExperienceType
    val experienceMinYears: Int?
    val educationLevel: EducationLevel?
    val region: String?
    val recruitmentType: JobRecruitmentType
    val recruitmentHeadcount: Int?
    val recruitmentStartAt: LocalDateTime?
    val recruitmentEndAt: LocalDateTime?
    val closesWhenFilled: Boolean?
    val autoCloseEnabled: Boolean?
    val companyAndTeamIntroduction: String?
    val responsibilities: String?
    val qualifications: String?
    val preferredQualifications: String?
    val compensation: String?
    val benefits: String?
    val hiringProcess: String?
    val recruitmentNotice: String?
    val applicationMethod: JobApplicationMethod?
    /** 이메일 지원을 고른 공고가 지원서를 받을 주소다. */
    val applyEmail: String?
    /** 지원 링크로도 함께 사용한다. */
    val sourceUrl: String?
}

/** 학력을 보내지 않으면 조건을 두지 않는다는 뜻이므로 학력 무관으로 본다. */
private fun CompanyJobWriteRequest.educationLevelOrAny(): EducationLevel =
    educationLevel ?: EducationLevel.ANY

/** 상시 채용은 종료 일시가 없어야 한다. 도메인도 막지만 요청 단계에서 어느 필드가 틀렸는지 알린다. */
private fun CompanyJobWriteRequest.validRecruitmentEndAt(): LocalDateTime? {
    if (recruitmentType == JobRecruitmentType.ALWAYS_OPEN && recruitmentEndAt != null) {
        throw InvalidRequestFieldException("recruitmentEndAt", "상시 채용에는 모집 종료 일시를 둘 수 없습니다.")
    }
    return recruitmentEndAt
}

/** 이메일 지원을 고르면 지원서를 받을 주소가 있어야 지원자가 지원할 수 있다. */
private fun CompanyJobWriteRequest.validApplyEmail(): String? {
    if (applicationMethod == JobApplicationMethod.EMAIL && applyEmail == null) {
        throw InvalidRequestFieldException("applyEmail", "이메일 지원에는 지원 이메일이 필요합니다.")
    }
    return validOptionalText("applyEmail", applyEmail)
}

/** 빈 문자열은 `@Email`·`@URL`을 통과하지만 도메인은 공백 값을 막으므로, 요청 단계에서 어느 필드인지 알린다. */
private fun validOptionalText(field: String, value: String?): String? {
    if (value != null && value.isBlank()) {
        throw InvalidRequestFieldException(field, "공백일 수 없습니다.")
    }
    return value
}

data class CreateCompanyJobRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:Size(max = 150) override val parentCompanyName: String?,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:Size(max = 100) override val jobField: String?,
    @field:Size(max = 100) override val jobRole: String?,
    @field:Size(max = 100) override val industry: String?,
    @field:Size(max = 2048) @field:URL override val coverImageUrl: String?,
    @field:Size(max = 2048) @field:URL override val logoUrl: String?,
    override val employmentType: EmploymentType,
    override val experienceType: ExperienceType,
    @field:PositiveOrZero override val experienceMinYears: Int?,
    override val educationLevel: EducationLevel?,
    @field:Size(max = 100) override val region: String?,
    override val recruitmentType: JobRecruitmentType,
    @field:Positive override val recruitmentHeadcount: Int?,
    override val recruitmentStartAt: LocalDateTime?,
    override val recruitmentEndAt: LocalDateTime?,
    override val closesWhenFilled: Boolean?,
    override val autoCloseEnabled: Boolean?,
    override val companyAndTeamIntroduction: String?,
    override val responsibilities: String?,
    override val qualifications: String?,
    override val preferredQualifications: String?,
    override val compensation: String?,
    override val benefits: String?,
    override val hiringProcess: String?,
    override val recruitmentNotice: String?,
    override val applicationMethod: JobApplicationMethod?,
    @field:Size(max = 320) @field:Email override val applyEmail: String?,
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
) : CompanyJobWriteRequest {

    fun toCommand(): JobAppendDto = JobAppendDto(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        title = title,
        jobField = jobField,
        jobRole = jobRole,
        industry = industry,
        coverImageUrl = coverImageUrl,
        logoUrl = validOptionalText("logoUrl", logoUrl),
        employmentType = employmentType,
        experienceType = experienceType,
        experienceMinYears = experienceMinYears,
        educationLevel = educationLevelOrAny(),
        region = region,
        recruitmentType = recruitmentType,
        recruitmentHeadcount = recruitmentHeadcount,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = validRecruitmentEndAt(),
        closesWhenFilled = closesWhenFilled,
        autoCloseEnabled = autoCloseEnabled,
        companyAndTeamIntroduction = companyAndTeamIntroduction,
        responsibilities = responsibilities,
        qualifications = qualifications,
        preferredQualifications = preferredQualifications,
        compensation = compensation,
        benefits = benefits,
        hiringProcess = hiringProcess,
        recruitmentNotice = recruitmentNotice,
        applicationMethod = applicationMethod,
        applyEmail = validApplyEmail(),
        sourceUrl = sourceUrl,
    )
}

data class UpdateCompanyJobRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:Size(max = 150) override val parentCompanyName: String?,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:Size(max = 100) override val jobField: String?,
    @field:Size(max = 100) override val jobRole: String?,
    @field:Size(max = 100) override val industry: String?,
    @field:Size(max = 2048) @field:URL override val coverImageUrl: String?,
    @field:Size(max = 2048) @field:URL override val logoUrl: String?,
    override val employmentType: EmploymentType,
    override val experienceType: ExperienceType,
    @field:PositiveOrZero override val experienceMinYears: Int?,
    override val educationLevel: EducationLevel?,
    @field:Size(max = 100) override val region: String?,
    override val recruitmentType: JobRecruitmentType,
    @field:Positive override val recruitmentHeadcount: Int?,
    override val recruitmentStartAt: LocalDateTime?,
    override val recruitmentEndAt: LocalDateTime?,
    override val closesWhenFilled: Boolean?,
    override val autoCloseEnabled: Boolean?,
    override val companyAndTeamIntroduction: String?,
    override val responsibilities: String?,
    override val qualifications: String?,
    override val preferredQualifications: String?,
    override val compensation: String?,
    override val benefits: String?,
    override val hiringProcess: String?,
    override val recruitmentNotice: String?,
    override val applicationMethod: JobApplicationMethod?,
    @field:Size(max = 320) @field:Email override val applyEmail: String?,
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
) : CompanyJobWriteRequest {

    fun toCommand(): JobUpdateDto = JobUpdateDto(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        title = title,
        jobField = jobField,
        jobRole = jobRole,
        industry = industry,
        coverImageUrl = coverImageUrl,
        logoUrl = validOptionalText("logoUrl", logoUrl),
        employmentType = employmentType,
        experienceType = experienceType,
        experienceMinYears = experienceMinYears,
        educationLevel = educationLevelOrAny(),
        region = region,
        recruitmentType = recruitmentType,
        recruitmentHeadcount = recruitmentHeadcount,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = validRecruitmentEndAt(),
        closesWhenFilled = closesWhenFilled,
        autoCloseEnabled = autoCloseEnabled,
        companyAndTeamIntroduction = companyAndTeamIntroduction,
        responsibilities = responsibilities,
        qualifications = qualifications,
        preferredQualifications = preferredQualifications,
        compensation = compensation,
        benefits = benefits,
        hiringProcess = hiringProcess,
        recruitmentNotice = recruitmentNotice,
        applicationMethod = applicationMethod,
        applyEmail = validApplyEmail(),
        sourceUrl = sourceUrl,
    )
}
