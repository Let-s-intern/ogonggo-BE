package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.AdminJobResult
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ContentReviewStatus
import java.time.LocalDateTime

internal object AdminJobFixtures {

    fun summary(id: Long = 693L): AdminJobSummary = AdminJobSummary(
        id = id,
        title = "VMD 경력사원 채용",
        companyName = "한국후지필름",
        employmentType = JobEmploymentType.CONTRACT,
        experienceType = JobExperienceType.EXPERIENCED,
        jobField = JobField.DESIGN,
        jobRole = JobRole.DESIGN_SPACE_INTERIOR_VMD,
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
    )

    fun detail(id: Long = 693L): AdminJobResult = AdminJobResult(
        summary = summary(id),
        companyAndTeamIntroduction = null,
        responsibilities = "주요 업무",
        qualifications = null,
        preferredQualifications = null,
        compensation = null,
        benefits = null,
        hiringProcess = null,
        sourceUrl = "https://example.com/jobs/693",
    )
}
