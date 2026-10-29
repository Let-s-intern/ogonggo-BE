package com.ogonggo.adminapi.member.business

import com.ogonggo.adminapi.bootcamp.business.requiredId
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.requiredId
import com.ogonggo.adminapi.recruitmentpost.business.requiredId
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostMetricDto
import com.ogonggo.core.user.implement.dto.CompanyMemberDto
import com.ogonggo.core.user.implement.dto.GeneralMemberDto
import java.time.LocalDateTime

/** 기업 회원 상세다. 목록 항목에 이 회원이 등록한 공고를 더한다. */
data class AdminCompanyMemberDetailResult(
    val member: CompanyMemberDto,
    val jobs: List<AdminCompanyMemberJob>,
)

data class AdminCompanyMemberJob(
    val id: Long,
    val title: String,
    val visibility: AdminContentVisibility,
    val reviewStatus: ContentReviewStatus?,
    val registeredAt: LocalDateTime,
    val viewCount: Long,
) {
    companion object {
        internal fun from(job: Job, metric: JobMetricDto): AdminCompanyMemberJob = AdminCompanyMemberJob(
            id = job.requiredId(),
            title = job.title,
            visibility = AdminContentVisibility.of(job.publicationStatus == JobPublicationStatus.PUBLISHED),
            reviewStatus = job.reviewStatus,
            registeredAt = job.createdAt,
            viewCount = metric.viewCount,
        )
    }
}

/** 일반 회원 상세다. 목록 항목에 북마크와 작성한 모집글을 더한다. */
data class AdminGeneralMemberDetailResult(
    val member: GeneralMemberDto,
    val bookmarkedJobs: List<AdminMemberBookmarkedJob>,
    val bookmarkedBootcamps: List<AdminMemberBookmarkedBootcamp>,
    val authoredSideStudies: List<AdminMemberAuthoredSideStudy>,
)

data class AdminMemberBookmarkedJob(
    val id: Long,
    val title: String,
    val companyName: String,
    val viewCount: Long,
) {
    companion object {
        internal fun from(job: Job, metric: JobMetricDto): AdminMemberBookmarkedJob = AdminMemberBookmarkedJob(
            id = job.requiredId(),
            title = job.title,
            companyName = job.companyName,
            viewCount = metric.viewCount,
        )
    }
}

data class AdminMemberBookmarkedBootcamp(
    val id: Long,
    val title: String,
    val companyName: String,
    val status: BootcampRecruitmentStatus,
) {
    companion object {
        internal fun from(bootcamp: Bootcamp): AdminMemberBookmarkedBootcamp = AdminMemberBookmarkedBootcamp(
            id = bootcamp.requiredId(),
            title = bootcamp.title,
            companyName = bootcamp.companyName,
            status = bootcamp.status,
        )
    }
}

/** 게시된 적 있는 모집글이라 모집 구분과 인원이 늘 있다. 관리자 모집글 목록과 같다. */
data class AdminMemberAuthoredSideStudy(
    val id: Long,
    val title: String,
    val kind: RecruitmentPostType,
    val appliedCount: Long,
    val capacity: Int,
    val closed: Boolean,
    val viewCount: Long,
) {
    companion object {
        internal fun from(
            post: RecruitmentPost,
            appliedCount: Long,
            metric: RecruitmentPostMetricDto,
        ): AdminMemberAuthoredSideStudy = AdminMemberAuthoredSideStudy(
            id = post.requiredId(),
            title = post.title,
            kind = checkNotNull(post.recruitmentType),
            appliedCount = appliedCount,
            capacity = checkNotNull(post.capacity),
            closed = post.recruitmentStatus == RecruitmentPostRecruitmentStatus.CLOSED,
            viewCount = metric.viewCount,
        )
    }
}
