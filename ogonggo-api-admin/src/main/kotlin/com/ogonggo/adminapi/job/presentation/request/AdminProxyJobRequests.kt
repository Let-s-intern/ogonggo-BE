package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.job.business.AdminProxyJobCreateCommand
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.implement.JobWriteValidator
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import org.hibernate.validator.constraints.URL

/**
 * 기업회원 채용공고 등록(사용자 API `CreateCompanyJobRequest`)과 같은 칸과 검증에 노출 여부를 더한다.
 * 칸 사이 규칙은 core의 [JobWriteValidator]를 함께 쓴다. 칸을 바꾸면 기업회원 등록 요청도 함께 바꾼다.
 */
data class CreateProxyJobRequest(
    @field:NotBlank @field:Size(max = 150) val companyName: String,
    @field:Size(max = 150) val parentCompanyName: String?,
    @field:NotBlank @field:Size(max = 255) val title: String,
    val jobField: JobField?,
    val jobRole: JobRole?,
    @field:Size(max = 100) val industry: String?,
    @field:Size(max = 2048) @field:URL val coverImageUrl: String?,
    @field:Size(max = 2048) @field:URL val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    @field:PositiveOrZero val experienceMinYears: Int?,
    @field:Schema(description = "학력 조건입니다. 보내지 않으면 학력 무관(ANY)으로 저장합니다.")
    val educationLevel: JobEducationLevel?,
    val region: Region?,
    val subRegion: SubRegion?,
    val recruitmentType: JobRecruitmentType,
    @field:Positive val recruitmentHeadcount: Int?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val closesWhenFilled: Boolean?,
    val autoCloseEnabled: Boolean?,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val recruitmentNotice: String?,
    val applicationMethod: JobApplicationMethod?,
    @field:Size(max = 320) @field:Email val applyEmail: String?,
    @field:Size(max = 2048) @field:URL val sourceUrl: String?,
    @field:Schema(description = "등록하면서 사용자 화면에 노출할지입니다. 보내지 않으면 VISIBLE입니다. 검수 없이 승인으로 등록되므로 VISIBLE이면 바로 보입니다.")
    val visibility: AdminContentVisibility?,
) {

    fun toCommand(): AdminProxyJobCreateCommand = AdminProxyJobCreateCommand(
        job = JobAppendDto(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            title = title,
            jobField = jobField,
            jobRole = jobRole,
            industry = industry,
            coverImageUrl = coverImageUrl,
            logoUrl = logoUrl,
            employmentType = employmentType,
            experienceType = experienceType,
            experienceMinYears = experienceMinYears,
            educationLevel = educationLevel ?: JobEducationLevel.ANY,
            region = region,
            subRegion = subRegion,
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
            applyEmail = applyEmail,
            sourceUrl = sourceUrl,
        ).also { JobWriteValidator.validate(it) { field, reason -> throw InvalidRequestFieldException(field, reason) } },
        visibility = visibility ?: AdminContentVisibility.VISIBLE,
    )
}
