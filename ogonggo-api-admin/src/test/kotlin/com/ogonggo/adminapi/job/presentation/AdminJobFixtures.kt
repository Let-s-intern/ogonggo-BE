package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.AdminJobResult
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import java.time.LocalDateTime

internal object AdminJobFixtures {

    fun summary(id: Long = 693L, inspectedAt: LocalDateTime? = null): AdminJobSummary = AdminJobSummary(
        id = id,
        title = "VMD 경력사원 채용",
        companyName = "한국후지필름",
        employmentType = JobEmploymentType.CONTRACT,
        experienceType = JobExperienceType.EXPERIENCED,
        jobField = JobField.PLANNING_STRATEGY,
        jobRole = JobRole.DESIGN_UX_UI_PRODUCT,
        educationLevel = JobEducationLevel.ANY,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 9, 8, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 9, 14, 23, 59),
        region = Region.SEOUL,
        subRegion = null,
        closedAt = null,
        viewCount = 3254,
        bookmarkCount = 196,
        commentCount = 0,
        visibility = AdminContentVisibility.HIDDEN,
        source = ContentSource.COMPANY,
        reviewStatus = ContentReviewStatus.PENDING,
        recruitmentStatus = JobRecruitmentStatus.RECRUITING,
        registeredAt = LocalDateTime.of(2026, 9, 10, 10, 48),
        inspectedAt = inspectedAt,
    )

    fun detail(
        id: Long = 693L,
        analysis: JobAnalysisContent? = null,
        analysisCurrent: Boolean = false,
        contentEditedAt: LocalDateTime? = null,
    ): AdminJobResult = AdminJobResult(
        summary = summary(id),
        parentCompanyName = null,
        industry = null,
        coverImageUrl = null,
        logoUrl = null,
        experienceMinYears = 3,
        recruitmentHeadcount = null,
        closesWhenFilled = null,
        autoCloseEnabled = null,
        applicationMethod = null,
        applyEmail = null,
        inquiryEmail = null,
        companyAndTeamIntroduction = null,
        responsibilities = "주요 업무",
        qualifications = null,
        preferredQualifications = null,
        compensation = null,
        benefits = null,
        hiringProcess = null,
        recruitmentNotice = null,
        sourceUrl = "https://example.com/jobs/693",
        sourceHtml = "<h2>주요 업무</h2><p>백엔드 개발</p>",
        analysis = analysis,
        analysisCurrent = analysisCurrent,
        contentEditedAt = contentEditedAt,
    )

    private val NOT_STATED = JobAnalysisContent.Fact(value = null, note = null)

    val ANALYSIS = JobAnalysisContent(
        tasks = listOf(JobAnalysisContent.Task(tag = "개발", text = "서버를 개발해요.")),
        required = listOf("Kotlin 경험이 있는 분"),
        preferred = emptyList(),
        employment = JobAnalysisContent.Employment(
            type = JobAnalysisContent.Fact(value = "정규직", note = null),
            conversion = NOT_STATED,
            salary = NOT_STATED,
            affiliation = NOT_STATED,
        ),
        submission = JobAnalysisContent.Submission(
            documents = JobAnalysisContent.Fact(value = "이력서", note = "PDF 권장"),
            essay = NOT_STATED,
            process = NOT_STATED,
            deadline = NOT_STATED,
        ),
        competencies = listOf(
            JobAnalysisContent.Competency(
                name = "문제 해결",
                quote = "주요 업무",
                description = "막힌 문제를 끝까지 푸는 역량이에요.",
                experiences = listOf("버그를 끝까지 추적한 경험"),
            ),
        ),
    )
}
