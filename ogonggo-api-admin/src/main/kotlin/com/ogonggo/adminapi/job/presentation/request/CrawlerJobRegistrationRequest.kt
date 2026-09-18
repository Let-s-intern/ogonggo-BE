package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.job.business.CrawlerJobCommand
import com.ogonggo.adminapi.job.business.CrawlerJobRegistrationCommand
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobRecruitmentType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDateTime

/**
 * 등록과 교체가 같은 칸을 받으므로 입력 계약을 한 곳에 모은다.
 *
 * 크롤러가 원문을 읽고 판단한 값을 그대로 저장한다. 서버가 빈 값을 다른 값으로 채우면 크롤러의 판단과
 * 저장된 값이 어긋나므로, 고용 형태·경력 유형·요구 학력·모집 기간 유형은 크롤러가 반드시 골라 보낸다.
 */
interface CrawlerJobWriteRequest {
    val companyName: String
    val parentCompanyName: String?
    val title: String
    val jobField: String?
    val jobRole: String?
    val industry: String?
    val coverImageUrl: String?
    val employmentType: EmploymentType
    val experienceType: ExperienceType
    val experienceMinYears: Int?
    val educationLevel: EducationLevel
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
    val applicationEmail: String?
    val inquiryEmail: String?
    val sourceUrl: String
}

/** 상시 채용은 종료 일시가 없어야 한다. 도메인도 막지만 요청 단계에서 어느 필드가 틀렸는지 알린다. */
private fun CrawlerJobWriteRequest.toJobCommand(): CrawlerJobCommand {
    if (recruitmentType == JobRecruitmentType.ALWAYS_OPEN && recruitmentEndAt != null) {
        throw InvalidRequestFieldException("recruitmentEndAt", "상시 채용에는 모집 종료 일시를 둘 수 없습니다.")
    }
    return CrawlerJobCommand(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        title = title,
        jobField = jobField,
        jobRole = jobRole,
        industry = industry,
        coverImageUrl = coverImageUrl,
        employmentType = employmentType,
        experienceType = experienceType,
        experienceMinYears = experienceMinYears,
        educationLevel = educationLevel,
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
        applicationEmail = applicationEmail,
        inquiryEmail = inquiryEmail,
        sourceUrl = sourceUrl,
    )
}

@Schema(description = "크롤러가 수집한 채용공고 등록 요청")
data class CrawlerJobRegistrationRequest(
    @field:Schema(description = "실제 채용 주체인 자회사명", example = "오공고")
    @field:NotBlank(message = "자회사명은 필수입니다.")
    @field:Size(max = 150, message = "자회사명은 150자 이하여야 합니다.")
    override val companyName: String,

    @field:Schema(description = "모회사명. 모회사가 없으면 생략한다", example = "렛츠커리어")
    @field:Size(max = 150, message = "모회사명은 150자 이하여야 합니다.")
    override val parentCompanyName: String? = null,

    @field:NotBlank(message = "채용공고 제목은 필수입니다.")
    @field:Size(max = 255, message = "채용공고 제목은 255자 이하여야 합니다.")
    override val title: String,

    @field:Schema(description = "직군. 크롤러 직무 분류표의 대분류", example = "IT·개발")
    @field:Size(max = 100, message = "직군은 100자 이하여야 합니다.")
    override val jobField: String? = null,

    @field:Schema(
        description = "직무. 크롤러 직무 분류표의 소분류. 비슷한 공고 추천에서 사용자의 희망 직무와 정확히 같은지 비교한다",
        example = "서버·백엔드",
    )
    @field:Size(max = 100, message = "직무는 100자 이하여야 합니다.")
    override val jobRole: String? = null,

    @field:Schema(
        description = "산업. 크롤러 산업 분류표의 값. 비슷한 공고 추천에서 사용자의 희망 산업과 정확히 같은지 비교한다",
        example = "IT·정보통신업",
    )
    @field:Size(max = 100, message = "산업은 100자 이하여야 합니다.")
    override val industry: String? = null,

    @field:Schema(description = "공고 대표 이미지 주소. 회사 로고가 있으면 로고, 없으면 원문 페이지의 대표 이미지")
    @field:Size(max = 2048, message = "대표 이미지 주소는 2048자 이하여야 합니다.")
    @field:URL(message = "대표 이미지 주소가 URL 형식이 아닙니다.")
    override val coverImageUrl: String? = null,

    override val employmentType: EmploymentType,

    override val experienceType: ExperienceType,

    @field:Schema(description = "최소 요구 경력 연수. 원문에 근거가 있을 때만 보낸다")
    @field:PositiveOrZero(message = "최소 경력 연수는 0 이상이어야 합니다.")
    override val experienceMinYears: Int? = null,

    override val educationLevel: EducationLevel,

    @field:Schema(description = "근무 지역", example = "서울 강남구")
    @field:Size(max = 100, message = "근무 지역은 100자 이하여야 합니다.")
    override val region: String? = null,

    override val recruitmentType: JobRecruitmentType,

    @field:Schema(description = "모집 인원. 원문에 숫자로 적혀 있을 때만 보낸다")
    @field:Positive(message = "모집 인원은 1명 이상이어야 합니다.")
    override val recruitmentHeadcount: Int? = null,

    @field:Schema(description = "모집 시작 일시. 원문에 시각이 없으면 그날 00:00:00")
    override val recruitmentStartAt: LocalDateTime? = null,

    @field:Schema(description = "모집 종료 일시. 원문에 시각이 없으면 그날 23:59:59. 상시 채용이면 생략한다")
    override val recruitmentEndAt: LocalDateTime? = null,

    @field:Schema(description = "적합한 지원자를 뽑으면 마감일 전이라도 모집을 끝내는 공고인지")
    override val closesWhenFilled: Boolean? = null,

    @field:Schema(description = "모집 종료 일시가 지나면 별도 조작 없이 마감으로 넘길지")
    override val autoCloseEnabled: Boolean? = null,

    override val companyAndTeamIntroduction: String? = null,

    override val responsibilities: String? = null,

    override val qualifications: String? = null,

    override val preferredQualifications: String? = null,

    override val compensation: String? = null,

    override val benefits: String? = null,

    override val hiringProcess: String? = null,

    @field:Schema(description = "위 칸 어디에도 맞지 않는 이 공고만의 채용 안내사항")
    override val recruitmentNotice: String? = null,

    override val applicationMethod: JobApplicationMethod? = null,

    @field:Schema(
        description = "지원서를 받는 이메일. 문의 이메일과 같은 주소면 두 칸에 같은 값을 보낸다",
        example = "recruit@ogonggo.com",
    )
    @field:Size(max = 320, message = "지원 접수 이메일은 320자 이하여야 합니다.")
    @field:Email(message = "지원 접수 이메일이 이메일 형식이 아닙니다.")
    override val applicationEmail: String? = null,

    @field:Schema(description = "채용 문의 이메일", example = "hr@ogonggo.com")
    @field:Size(max = 320, message = "채용 문의 이메일은 320자 이하여야 합니다.")
    @field:Email(message = "채용 문의 이메일이 이메일 형식이 아닙니다.")
    override val inquiryEmail: String? = null,

    @field:Schema(description = "채용공고 원문 URL. 직무별로 나눈 공고는 #1, #2처럼 조각이 붙는다")
    @field:NotBlank(message = "원문 URL은 필수입니다.")
    @field:Size(max = 2048, message = "원문 URL은 2048자 이하여야 합니다.")
    @field:URL(message = "원문 URL이 URL 형식이 아닙니다.")
    override val sourceUrl: String,

    @field:Schema(description = "AI가 생성한 태그 목록")
    val tags: List<@NotBlank(message = "태그명은 비어 있을 수 없습니다.") String> = emptyList(),
) : CrawlerJobWriteRequest {

    fun toCommand(): CrawlerJobRegistrationCommand = CrawlerJobRegistrationCommand(job = toJobCommand(), tags = tags)
}

/** 태그는 등록할 때만 받는다. 교체는 공고 칸만 바꾼다. */
@Schema(description = "크롤러가 다시 수집·분류한 채용공고 교체 요청")
data class CrawlerJobReplaceRequest(
    @field:NotBlank(message = "자회사명은 필수입니다.")
    @field:Size(max = 150, message = "자회사명은 150자 이하여야 합니다.")
    override val companyName: String,

    @field:Size(max = 150, message = "모회사명은 150자 이하여야 합니다.")
    override val parentCompanyName: String? = null,

    @field:NotBlank(message = "채용공고 제목은 필수입니다.")
    @field:Size(max = 255, message = "채용공고 제목은 255자 이하여야 합니다.")
    override val title: String,

    @field:Size(max = 100, message = "직군은 100자 이하여야 합니다.")
    override val jobField: String? = null,

    @field:Size(max = 100, message = "직무는 100자 이하여야 합니다.")
    override val jobRole: String? = null,

    @field:Size(max = 100, message = "산업은 100자 이하여야 합니다.")
    override val industry: String? = null,

    @field:Size(max = 2048, message = "대표 이미지 주소는 2048자 이하여야 합니다.")
    @field:URL(message = "대표 이미지 주소가 URL 형식이 아닙니다.")
    override val coverImageUrl: String? = null,

    override val employmentType: EmploymentType,

    override val experienceType: ExperienceType,

    @field:PositiveOrZero(message = "최소 경력 연수는 0 이상이어야 합니다.")
    override val experienceMinYears: Int? = null,

    override val educationLevel: EducationLevel,

    @field:Size(max = 100, message = "근무 지역은 100자 이하여야 합니다.")
    override val region: String? = null,

    override val recruitmentType: JobRecruitmentType,

    @field:Positive(message = "모집 인원은 1명 이상이어야 합니다.")
    override val recruitmentHeadcount: Int? = null,

    override val recruitmentStartAt: LocalDateTime? = null,

    override val recruitmentEndAt: LocalDateTime? = null,

    override val closesWhenFilled: Boolean? = null,

    override val autoCloseEnabled: Boolean? = null,

    override val companyAndTeamIntroduction: String? = null,

    override val responsibilities: String? = null,

    override val qualifications: String? = null,

    override val preferredQualifications: String? = null,

    override val compensation: String? = null,

    override val benefits: String? = null,

    override val hiringProcess: String? = null,

    override val recruitmentNotice: String? = null,

    override val applicationMethod: JobApplicationMethod? = null,

    @field:Size(max = 320, message = "지원 접수 이메일은 320자 이하여야 합니다.")
    @field:Email(message = "지원 접수 이메일이 이메일 형식이 아닙니다.")
    override val applicationEmail: String? = null,

    @field:Size(max = 320, message = "채용 문의 이메일은 320자 이하여야 합니다.")
    @field:Email(message = "채용 문의 이메일이 이메일 형식이 아닙니다.")
    override val inquiryEmail: String? = null,

    @field:NotBlank(message = "원문 URL은 필수입니다.")
    @field:Size(max = 2048, message = "원문 URL은 2048자 이하여야 합니다.")
    @field:URL(message = "원문 URL이 URL 형식이 아닙니다.")
    override val sourceUrl: String,
) : CrawlerJobWriteRequest {

    fun toCommand(): CrawlerJobCommand = toJobCommand()
}
