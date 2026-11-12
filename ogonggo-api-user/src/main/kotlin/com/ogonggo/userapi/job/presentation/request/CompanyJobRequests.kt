package com.ogonggo.userapi.job.presentation.request

import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.implement.JobWriteValidator
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.userapi.error.InvalidRequestFieldException
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.net.URI
import java.time.LocalDateTime
import org.hibernate.validator.constraints.URL

/**
 * 등록과 수정이 같은 필드를 받으므로 입력 계약을 한 곳에 모은다.
 * 게시 상태는 요청으로 받지 않는다. 등록은 항상 임시저장으로 만들고 게시는 별도 요청으로 처리한다.
 */
interface CompanyJobWriteRequest {
    val companyName: String
    val parentCompanyName: String?
    val title: String
    /** 직군. 직무·산업과 함께 사용자 프로필의 희망 직군·직무·산업과 짝을 이룬다. */
    val jobField: JobField?
    /** 직무. 보내면 [jobField]에 속한 값이어야 한다. 비슷한 공고 추천에서 사용자의 희망 직무와 라벨로 비교한다. */
    val jobRole: JobRole?
    /** 비슷한 공고 추천에서 사용자의 희망 산업과 정확히 같은지 비교한다. */
    val industry: String?
    val coverImageUrl: String?
    /** 대표 이미지와 따로 보이는 기업 로고다. */
    val logoUrl: String?
    val employmentType: JobEmploymentType
    val experienceType: JobExperienceType
    val experienceMinYears: Int?
    val educationLevel: JobEducationLevel?
    val region: Region?
    val subRegion: SubRegion?
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
    /** 선택으로 올린 원문 이미지. 공고 상세 `공고 원문` 탭에 글 아래로 보인다. 공고 칸이 아니라 원문으로 저장한다. */
    val sourceImageUrls: List<String>?
}

const val MAX_SOURCE_IMAGES = 10
private const val MAX_SOURCE_IMAGE_URL_LENGTH = 2048

/**
 * 원문 이미지 주소를 하나씩 검사해 돌려준다. 목록 원소에 붙인 Bean Validation 제약은 Kotlin이 바이트코드에 남기지 않아
 * 검증되지 않으므로 여기서 본다. 원문 HTML의 `src` 로 들어가므로 http(s) 절대 주소만 받는다.
 */
fun CompanyJobWriteRequest.validatedSourceImageUrls(): List<String>? = sourceImageUrls?.onEachIndexed { index, url ->
    val uri = runCatching { URI(url) }.getOrNull()
    val valid = url.isNotBlank() &&
        url.length <= MAX_SOURCE_IMAGE_URL_LENGTH &&
        uri != null &&
        uri.scheme?.lowercase() in setOf("http", "https") &&
        !uri.host.isNullOrBlank()
    if (!valid) invalid("sourceImageUrls[$index]", "http(s) 이미지 주소여야 합니다.")
}

/** 학력을 보내지 않으면 조건을 두지 않는다는 뜻이므로 학력 무관으로 본다. */
private fun CompanyJobWriteRequest.educationLevelOrAny(): JobEducationLevel =
    educationLevel ?: JobEducationLevel.ANY

/** 칸 사이 규칙은 관리자 대리 등록과 같이 core의 [JobWriteValidator]가 정한다. */
private fun invalid(field: String, reason: String): Nothing = throw InvalidRequestFieldException(field, reason)

data class CreateCompanyJobRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:Size(max = 150) override val parentCompanyName: String?,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    override val jobField: JobField?,
    override val jobRole: JobRole?,
    @field:Size(max = 100) override val industry: String?,
    @field:Size(max = 2048) @field:URL override val coverImageUrl: String?,
    @field:Size(max = 2048) @field:URL override val logoUrl: String?,
    override val employmentType: JobEmploymentType,
    override val experienceType: JobExperienceType,
    @field:PositiveOrZero override val experienceMinYears: Int?,
    @field:Schema(description = "학력 조건입니다. 보내지 않으면 학력 무관(ANY)으로 저장합니다.")
    override val educationLevel: JobEducationLevel?,
    override val region: Region?,
    override val subRegion: SubRegion?,
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
    @field:Schema(description = "원문 이미지 주소(선택, 최대 10장). 올린 순서대로 공고 원문에 보입니다. 비우거나 보내지 않으면 원문 이미지가 없습니다.")
    @field:Size(max = MAX_SOURCE_IMAGES)
    override val sourceImageUrls: List<String>? = null,
) : CompanyJobWriteRequest {

    fun toCommand(): JobAppendDto = JobAppendDto(
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
        educationLevel = educationLevelOrAny(),
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
    ).also { JobWriteValidator.validate(it, ::invalid) }
}

data class UpdateCompanyJobRequest(
    @field:NotBlank @field:Size(max = 150) override val companyName: String,
    @field:Size(max = 150) override val parentCompanyName: String?,
    @field:NotBlank @field:Size(max = 255) override val title: String,
    override val jobField: JobField?,
    override val jobRole: JobRole?,
    @field:Size(max = 100) override val industry: String?,
    @field:Size(max = 2048) @field:URL override val coverImageUrl: String?,
    @field:Size(max = 2048) @field:URL override val logoUrl: String?,
    override val employmentType: JobEmploymentType,
    override val experienceType: JobExperienceType,
    @field:PositiveOrZero override val experienceMinYears: Int?,
    @field:Schema(description = "학력 조건입니다. 보내지 않으면 학력 무관(ANY)으로 저장합니다.")
    override val educationLevel: JobEducationLevel?,
    override val region: Region?,
    override val subRegion: SubRegion?,
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
    @field:Schema(description = "원문 이미지 주소(선택, 최대 10장). 보내지 않으면(null) 지금 원문 이미지를 그대로 두고, 빈 목록이면 모두 뺍니다. 값이 있으면 그 이미지들로 바꿉니다.")
    @field:Size(max = MAX_SOURCE_IMAGES)
    override val sourceImageUrls: List<String>? = null,
) : CompanyJobWriteRequest {

    fun toCommand(): JobUpdateDto = JobUpdateDto(
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
        educationLevel = educationLevelOrAny(),
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
    ).also { JobWriteValidator.validate(it, ::invalid) }
}
