package com.ogonggo.userapi.job.presentation.request

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
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
    val companyLogoUrl: String?
    val title: String
    val jobField: String?
    val coverImageUrl: String?
    val employmentType: EmploymentType
    val experienceType: ExperienceType
    val experienceMinYears: Int?
    val experienceMaxYears: Int?
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
    /** 지원 링크로도 함께 사용한다. */
    val sourceUrl: String?
}

/** 학력을 보내지 않으면 조건을 두지 않는다는 뜻이므로 학력 무관으로 본다. */
private fun CompanyJobWriteRequest.educationLevelOrAny(): EducationLevel =
    educationLevel ?: EducationLevel.ANY

data class CreateCompanyJobRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:Size(max = 150) override val parentCompanyName: String?,
    @field:Size(max = 2048) @field:URL override val companyLogoUrl: String?,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:Size(max = 100) override val jobField: String?,
    @field:Size(max = 2048) @field:URL override val coverImageUrl: String?,
    override val employmentType: EmploymentType,
    override val experienceType: ExperienceType,
    @field:PositiveOrZero override val experienceMinYears: Int?,
    @field:PositiveOrZero override val experienceMaxYears: Int?,
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
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
) : CompanyJobWriteRequest {

    fun toCommand(): JobAppendDto = JobAppendDto(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        companyLogoUrl = companyLogoUrl,
        title = title,
        jobField = jobField,
        coverImageUrl = coverImageUrl,
        employmentType = employmentType,
        experienceType = experienceType,
        experienceMinYears = experienceMinYears,
        experienceMaxYears = experienceMaxYears,
        educationLevel = educationLevelOrAny(),
        region = region,
        recruitmentType = recruitmentType,
        recruitmentHeadcount = recruitmentHeadcount,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
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
        sourceUrl = sourceUrl,
    )
}

data class UpdateCompanyJobRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:Size(max = 150) override val parentCompanyName: String?,
    @field:Size(max = 2048) @field:URL override val companyLogoUrl: String?,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    @field:Size(max = 100) override val jobField: String?,
    @field:Size(max = 2048) @field:URL override val coverImageUrl: String?,
    override val employmentType: EmploymentType,
    override val experienceType: ExperienceType,
    @field:PositiveOrZero override val experienceMinYears: Int?,
    @field:PositiveOrZero override val experienceMaxYears: Int?,
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
    @field:Size(max = 2048) @field:URL override val sourceUrl: String?,
) : CompanyJobWriteRequest {

    fun toCommand(): JobUpdateDto = JobUpdateDto(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        companyLogoUrl = companyLogoUrl,
        title = title,
        jobField = jobField,
        coverImageUrl = coverImageUrl,
        employmentType = employmentType,
        experienceType = experienceType,
        experienceMinYears = experienceMinYears,
        experienceMaxYears = experienceMaxYears,
        educationLevel = educationLevelOrAny(),
        region = region,
        recruitmentType = recruitmentType,
        recruitmentHeadcount = recruitmentHeadcount,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
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
        sourceUrl = sourceUrl,
    )
}
