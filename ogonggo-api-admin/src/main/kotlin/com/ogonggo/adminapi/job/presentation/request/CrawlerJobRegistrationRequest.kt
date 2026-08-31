package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.job.business.CrawlerJobRegistrationCommand
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "크롤러가 수집한 채용공고 등록 요청")
data class CrawlerJobRegistrationRequest(
    @field:Schema(description = "실제 채용 주체인 자회사명", example = "오공고")
    @field:NotBlank(message = "자회사명은 필수입니다.")
    @field:Size(max = 150, message = "자회사명은 150자 이하여야 합니다.")
    val companyName: String,

    @field:Schema(description = "모회사명. 모회사가 없으면 생략한다", example = "렛츠커리어")
    @field:Size(max = 150, message = "모회사명은 150자 이하여야 합니다.")
    val parentCompanyName: String? = null,

    @field:Schema(description = "채용공고 제목", example = "백엔드 개발자")
    @field:NotBlank(message = "채용공고 제목은 필수입니다.")
    @field:Size(max = 255, message = "채용공고 제목은 255자 이하여야 합니다.")
    val title: String,

    @field:Schema(description = "고용 형태")
    val employmentType: EmploymentType,

    @field:Schema(description = "채용공고 원문 URL")
    @field:NotBlank(message = "원문 URL은 필수입니다.")
    @field:Size(max = 2048, message = "원문 URL은 2048자 이하여야 합니다.")
    val sourceUrl: String,

    @field:Schema(description = "최소 요구 경력 연수. 생략하면 경력 무관으로 등록한다")
    @field:PositiveOrZero(message = "최소 경력 연수는 0 이상이어야 합니다.")
    val experienceMinYears: Int? = null,

    @field:Schema(description = "최대 요구 경력 연수. 생략하면 경력 무관으로 등록한다")
    @field:PositiveOrZero(message = "최대 경력 연수는 0 이상이어야 합니다.")
    val experienceMaxYears: Int? = null,

    @field:Schema(description = "모집 시작 일시. 생략 가능")
    val recruitmentStartAt: LocalDateTime? = null,

    @field:Schema(description = "모집 종료 일시. 생략 가능")
    val recruitmentEndAt: LocalDateTime? = null,

    @field:Schema(description = "요구 학력. 생략하면 학력 무관으로 등록한다")
    val educationLevel: EducationLevel? = null,

    @field:Schema(description = "근무 지역. 생략 가능", example = "서울")
    @field:Size(max = 100, message = "근무 지역은 100자 이하여야 합니다.")
    val region: String? = null,

    @field:Schema(description = "AI가 생성한 태그 목록")
    val tags: List<@NotBlank(message = "태그명은 비어 있을 수 없습니다.") String> = emptyList(),

    @field:Schema(description = "회사 및 팀 소개")
    val companyAndTeamIntroduction: String? = null,

    @field:Schema(description = "주요 업무")
    val responsibilities: String? = null,

    @field:Schema(description = "자격 요건")
    val qualifications: String? = null,

    @field:Schema(description = "우대 사항")
    val preferredQualifications: String? = null,

    @field:Schema(description = "급여 및 처우")
    val compensation: String? = null,

    @field:Schema(description = "복지 및 혜택")
    val benefits: String? = null,

    @field:Schema(description = "채용 절차")
    val hiringProcess: String? = null,
) {

    /**
     * 크롤러는 모집 기간 유형을 따로 판단하지 않으므로 모집 일시 유무로 결정하고,
     * 경력과 학력은 값이 없으면 무관으로 등록한다.
     */
    fun toCommand(): CrawlerJobRegistrationCommand = CrawlerJobRegistrationCommand(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        title = title,
        employmentType = employmentType,
        experienceType = resolveExperienceType(),
        experienceMinYears = experienceMinYears,
        experienceMaxYears = experienceMaxYears,
        educationLevel = educationLevel ?: EducationLevel.ANY,
        region = region,
        recruitmentType = resolveRecruitmentType(),
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        companyAndTeamIntroduction = companyAndTeamIntroduction,
        responsibilities = responsibilities,
        qualifications = qualifications,
        preferredQualifications = preferredQualifications,
        compensation = compensation,
        benefits = benefits,
        hiringProcess = hiringProcess,
        sourceUrl = sourceUrl,
        tags = tags,
        publicationStatus = JobPublicationStatus.PUBLISHED,
    )

    private fun resolveExperienceType(): ExperienceType =
        if (experienceMinYears == null && experienceMaxYears == null) {
            ExperienceType.IRRELEVANT
        } else {
            ExperienceType.EXPERIENCED
        }

    private fun resolveRecruitmentType(): JobRecruitmentType =
        if (recruitmentStartAt == null && recruitmentEndAt == null) {
            JobRecruitmentType.ALWAYS_OPEN
        } else {
            JobRecruitmentType.PERIOD
        }
}
