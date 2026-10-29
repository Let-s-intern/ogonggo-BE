package com.ogonggo.adminapi.member.business

import com.ogonggo.adminapi.bootcamp.business.requiredId
import com.ogonggo.adminapi.job.business.requiredId
import com.ogonggo.adminapi.recruitmentpost.business.requiredId
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostMetricDto
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserManagementReader
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.dto.CompanyMemberPageDto
import com.ogonggo.core.user.implement.dto.GeneralMemberPageDto
import org.springframework.stereotype.Service

@Service
class AdminMemberService(
    private val userManagementReader: UserManagementReader,
    private val userManager: UserManager,
    private val jobReader: JobReader,
    private val jobMetricReader: JobMetricReader,
    private val jobBookmarkReader: JobBookmarkReader,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
    private val recruitmentPostReader: RecruitmentPostReader,
    private val recruitmentPostApplicationReader: RecruitmentPostApplicationReader,
    private val recruitmentPostMetricReader: RecruitmentPostMetricReader,
) {

    fun getGeneralMembers(condition: UserManagementSearchCondition, page: Int, size: Int): GeneralMemberPageDto =
        userManagementReader.readGeneralMemberPage(condition, page, size)

    fun getCompanyMembers(condition: UserManagementSearchCondition, page: Int, size: Int): CompanyMemberPageDto =
        userManagementReader.readCompanyMemberPage(condition, page, size)

    /**
     * 일반 회원 상세. 북마크는 사용자가 마이페이지에서 보는 것과 같이 지금 공개된 공고·부트캠프만,
     * 모집글은 콘솔 목록과 같이 게시된 적 있는 것만 싣는다. 응답이 끝없이 커지지 않도록 각각 최근 [ACTIVITY_LIMIT]건까지다.
     */
    fun getGeneralMember(userId: Long): AdminGeneralMemberDetailResult {
        val member = userManagementReader.readGeneralMember(userId)
        val jobs = jobBookmarkReader.readBookmarkedPublishedPage(userId, JobSearchCondition.NONE, 0, ACTIVITY_LIMIT).jobs
        val jobMetrics = jobMetricReader.readAll(jobs.map { it.requiredId() })
        val bootcamps = bootcampBookmarkReader.readBookmarkedPublicPage(
            userId,
            BootcampSearchCondition.NONE,
            0,
            ACTIVITY_LIMIT,
        ).bootcamps
        val posts = recruitmentPostReader.readConsolePage(
            RecruitmentPostConsoleSearchCondition(authorUserId = userId),
            RecruitmentPostSortType.LATEST,
            0,
            ACTIVITY_LIMIT,
        ).posts
        val postIds = posts.map { it.requiredId() }
        val applicationCounts = recruitmentPostApplicationReader.countByPostIds(postIds)
        val postMetrics = recruitmentPostMetricReader.readAll(postIds)

        return AdminGeneralMemberDetailResult(
            member = member,
            bookmarkedJobs = jobs.map { job ->
                AdminMemberBookmarkedJob.from(job, jobMetrics[job.requiredId()] ?: JobMetricDto.EMPTY)
            },
            bookmarkedBootcamps = bootcamps.map(AdminMemberBookmarkedBootcamp::from),
            authoredSideStudies = posts.map { post ->
                AdminMemberAuthoredSideStudy.from(
                    post = post,
                    appliedCount = applicationCounts[post.requiredId()] ?: 0L,
                    metric = postMetrics[post.requiredId()] ?: RecruitmentPostMetricDto.EMPTY,
                )
            },
        )
    }

    /** 기업 회원 상세. 이 회원이 등록한 공고를 최근 등록 순으로 [ACTIVITY_LIMIT]건까지 싣는다. 검수 중이거나 숨긴 공고도 싣는다. */
    fun getCompanyMember(userId: Long): AdminCompanyMemberDetailResult {
        val member = userManagementReader.readCompanyMember(userId)
        val jobs = jobReader.readOwnedPage(userId, 0, ACTIVITY_LIMIT).jobs
        val metrics = jobMetricReader.readAll(jobs.map { it.requiredId() })

        return AdminCompanyMemberDetailResult(
            member = member,
            jobs = jobs.map { job -> AdminCompanyMemberJob.from(job, metrics[job.requiredId()] ?: JobMetricDto.EMPTY) },
        )
    }

    /**
     * 관리자가 일반 회원을 멘토·관리자로 올리거나 내린다.
     * 자기 역할은 바꿀 수 없다. 실수로 자기를 내리면 바로 콘솔에서 막히고, 관리자가 한 명뿐이면 DB를 직접 고쳐야 한다.
     */
    fun changeGeneralMemberRole(adminUserId: Long, userId: Long, role: UserRole) {
        if (adminUserId == userId) {
            throw InvalidValueException(UserErrorCode.OWN_ROLE_UNCHANGEABLE)
        }
        userManager.changeRole(userId, role)
    }

    companion object {
        /** 회원 상세에 싣는 활동 목록마다의 최대 건수다. */
        const val ACTIVITY_LIMIT = 50
    }
}
