package com.ogonggo.userapi.job.presentation.response

import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.userapi.job.business.UserJobCalendarItem
import com.ogonggo.userapi.job.business.UserJobResult
import com.ogonggo.userapi.job.business.UserJobSummary
import com.ogonggo.userapi.job.business.UserTodayJobSummary
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserJobCalendarItemResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    @field:Schema(description = "공고 대표 이미지 주소. 없으면 null")
    val coverImageUrl: String?,
    @field:Schema(description = "기업 로고 이미지 주소. 없으면 null")
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val recruitmentStartAt: LocalDateTime,
    val recruitmentEndAt: LocalDateTime,
    @field:Schema(description = "로그인한 사용자의 북마크 여부. 토큰이 없으면 항상 false")
    val bookmarked: Boolean,
) {
    companion object {
        internal fun from(result: UserJobCalendarItem): UserJobCalendarItemResponse = UserJobCalendarItemResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            coverImageUrl = result.coverImageUrl,
            logoUrl = result.logoUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            bookmarked = result.bookmarked,
        )
    }
}

data class UserJobSummaryResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val experienceMinYears: Int?,
    val educationLevel: JobEducationLevel,
    val region: Region?,
    val subRegion: SubRegion?,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        internal fun from(result: UserJobSummary): UserJobSummaryResponse = UserJobSummaryResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            coverImageUrl = result.coverImageUrl,
            logoUrl = result.logoUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            experienceMinYears = result.experienceMinYears,
            educationLevel = result.educationLevel,
            region = result.region,
            subRegion = result.subRegion,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            closedAt = result.closedAt,
            bookmarked = result.bookmarked,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
        )
    }
}

/** 목록 항목과 같은 필드를 같은 높이에 펼치고 추천 문구를 더한다. 클라이언트가 목록 카드와 같은 코드로 그리기 위해서다. */
data class UserTodayJobSummaryResponse(
    @get:JsonUnwrapped
    val job: UserJobSummaryResponse,
    @field:Schema(description = "카드에 굵게 보여 주는 추천 문구 제목. 예: 경력 없이 시작하고 싶다면")
    val recommendationTitle: String,
    @field:Schema(description = "추천 문구 제목 아래 설명. 예: 실무 중심 프로젝트로 빠른 성장")
    val recommendationDescription: String,
) {
    companion object {
        internal fun from(result: UserTodayJobSummary): UserTodayJobSummaryResponse = UserTodayJobSummaryResponse(
            job = UserJobSummaryResponse.from(result.job),
            recommendationTitle = result.recommendationTitle,
            recommendationDescription = result.recommendationDescription,
        )
    }
}

data class UserJobDetailResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val experienceMinYears: Int?,
    val educationLevel: JobEducationLevel,
    val region: Region?,
    val subRegion: SubRegion?,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    @field:Schema(description = "채용 안내사항. 제출 서류·근무 조건·유의사항 등")
    val recruitmentNotice: String?,
    val sourceUrl: String?,
    val applyEmail: String?,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    @field:Schema(description = "공고 분석. 아직 없거나 본문이 바뀐 뒤 다시 분석하기 전이면 null이며, 그때는 원문만 보여 줍니다")
    val analysis: UserJobAnalysisResponse?,
) {
    companion object {
        internal fun from(result: UserJobResult): UserJobDetailResponse = UserJobDetailResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            coverImageUrl = result.coverImageUrl,
            logoUrl = result.logoUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            experienceMinYears = result.experienceMinYears,
            educationLevel = result.educationLevel,
            region = result.region,
            subRegion = result.subRegion,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            companyAndTeamIntroduction = result.companyAndTeamIntroduction,
            responsibilities = result.responsibilities,
            qualifications = result.qualifications,
            preferredQualifications = result.preferredQualifications,
            compensation = result.compensation,
            benefits = result.benefits,
            hiringProcess = result.hiringProcess,
            recruitmentNotice = result.recruitmentNotice,
            sourceUrl = result.sourceUrl,
            applyEmail = result.applyEmail,
            closedAt = result.closedAt,
            bookmarked = result.bookmarked,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
            analysis = result.analysis?.let(UserJobAnalysisResponse::from),
        )
    }
}

/**
 * 공고 상세의 '공고 분석' 탭 내용이다. 크롤러가 AI로 만든다.
 * 값이 null인 칸은 공고에서 확인할 수 없다는 뜻이며 '공고에 명시 없음'으로 그린다.
 */
data class UserJobAnalysisResponse(
    @field:Schema(description = "실제 하는 일. 3개까지")
    val tasks: List<Task>,
    @field:Schema(description = "필수 지원 조건. 8개까지. 사용자가 체크해 보는 목록이며 서버에 저장하지 않습니다")
    val required: List<String>,
    @field:Schema(description = "우대 조건. 8개까지")
    val preferred: List<String>,
    val employment: Employment,
    val submission: Submission,
    @field:Schema(description = "연결하기 좋은 경험. 역량 3개까지")
    val competencies: List<Competency>,
) {
    data class Task(
        @field:Schema(description = "일의 성격", example = "기획")
        val tag: String,
        val text: String,
    )

    data class Fact(
        @field:Schema(description = "값. 공고에 없으면 null")
        val value: String?,
        @field:Schema(description = "보충 설명", example = "PDF 권장")
        val note: String?,
    )

    data class Employment(val type: Fact, val conversion: Fact, val salary: Fact, val affiliation: Fact)

    data class Submission(val documents: Fact, val essay: Fact, val process: Fact, val deadline: Fact)

    data class Competency(
        val name: String,
        @field:Schema(description = "그 역량을 요구하는 공고 문장 그대로")
        val quote: String,
        val description: String,
        @field:Schema(description = "연결할 수 있는 경험. 3개까지")
        val experiences: List<String>,
    )

    companion object {
        internal fun from(content: JobAnalysisContent): UserJobAnalysisResponse = UserJobAnalysisResponse(
            tasks = content.tasks.map { Task(it.tag, it.text) },
            required = content.required,
            preferred = content.preferred,
            employment = content.employment.let {
                Employment(fact(it.type), fact(it.conversion), fact(it.salary), fact(it.affiliation))
            },
            submission = content.submission.let {
                Submission(fact(it.documents), fact(it.essay), fact(it.process), fact(it.deadline))
            },
            competencies = content.competencies.map { Competency(it.name, it.quote, it.description, it.experiences) },
        )

        private fun fact(fact: JobAnalysisContent.Fact) = Fact(fact.value, fact.note)
    }
}
