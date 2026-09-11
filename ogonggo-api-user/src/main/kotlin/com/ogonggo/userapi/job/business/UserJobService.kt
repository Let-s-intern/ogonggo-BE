package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceUrlClickAppender
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.user.implement.UserProfileReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserJobService(
    private val jobReader: JobReader,
    private val jobBookmarkReader: JobBookmarkReader,
    private val jobMetricReader: JobMetricReader,
    private val jobSourceUrlClickAppender: JobSourceUrlClickAppender,
    private val userProfileReader: UserProfileReader,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /** 로그인 없이 조회할 수 있어 userId가 없을 수 있고, 그때는 북마크가 하나도 없는 것으로 본다. */
    fun getJobs(
        userId: Long?,
        condition: JobSearchCondition,
        sortType: JobSortType,
        page: Int,
        size: Int,
    ): UserJobPageResult {
        val result = jobReader.readPublishedPage(condition, sortType, page, size)
        val jobIds = result.jobs.map(Job::requiredId)
        return UserJobPageResult.from(
            result = result,
            bookmarkedJobIds = readBookmarkedJobIds(userId, jobIds),
            metrics = jobMetricReader.readAll(jobIds),
        )
    }

    fun getPopularJobs(userId: Long?): List<UserJobSummary> =
        toSummaries(userId, jobReader.readPopularRecruiting(POPULAR_JOB_LIMIT))

    /**
     * 희망 직무와 산업이 모두 맞는 공고부터 직무만, 산업만 맞는 공고 순으로 채운다.
     * 앞 단계에서 담은 공고는 다음 단계에서 빼고, 네 건이 차면 더 조회하지 않는다.
     * 모자라도 다른 공고로 채우지 않는다. 기업 회원처럼 프로필이 없거나 희망 값이 비면 빈 목록이다.
     */
    fun getSimilarJobs(userId: Long): List<UserJobSummary> {
        val profile = userProfileReader.read(userId) ?: return emptyList()
        val jobRoles = profile.wishJob.splitWishValues()
        val industries = profile.wishIndustry.splitWishValues()

        val steps = listOfNotNull(
            (jobRoles to industries).takeIf { jobRoles.isNotEmpty() && industries.isNotEmpty() },
            (jobRoles to emptyList<String>()).takeIf { jobRoles.isNotEmpty() },
            (emptyList<String>() to industries).takeIf { industries.isNotEmpty() },
        )
        val jobs = mutableListOf<Job>()
        for ((stepJobRoles, stepIndustries) in steps) {
            if (jobs.size >= SIMILAR_JOB_LIMIT) {
                break
            }
            jobs += jobReader.readRecruitingMatched(
                jobRoles = stepJobRoles,
                industries = stepIndustries,
                excludedJobIds = jobs.map(Job::requiredId),
                limit = SIMILAR_JOB_LIMIT - jobs.size,
            )
        }
        return toSummaries(userId, jobs)
    }

    /** 조회 기간의 유효성은 Presentation이 검증하고, 여기서는 날짜를 일시 경계로 옮기기만 한다. */
    fun getJobCalendar(from: LocalDate, to: LocalDate): List<UserJobCalendarItem> =
        jobReader.readPublishedCalendar(
            rangeStart = from.atStartOfDay(),
            rangeEndExclusive = to.plusDays(1).atStartOfDay(),
        ).map(UserJobCalendarItem::from)

    /**
     * 조회됐다는 사실만 알리고 지표 갱신은 수신자에게 맡긴다.
     * 기록이 비동기이므로 상세 응답의 조회 수에는 이번 조회가 아직 반영되지 않는다.
     */
    fun getJob(userId: Long?, jobId: Long): UserJobResult {
        val job = jobReader.readPublished(jobId)
        val bookmarked = jobId in readBookmarkedJobIds(userId, listOf(jobId))
        val result = UserJobResult.from(job, bookmarked, jobMetricReader.read(jobId))
        eventPublisher.publishEvent(JobViewedEvent(jobId))
        return result
    }

    /**
     * 원문으로 이동한 사용자를 기록한다.
     * 같은 사용자가 다시 눌러도 실패로 만들지 않고 최초 기록을 유지한다.
     *
     * 쓰기가 한 건뿐이라 묶어야 할 원자성이 없으므로 트랜잭션을 열지 않는다.
     * 열어 두면 동시에 누른 두 요청 중 하나가 유니크 제약에 걸릴 때
     * 그 실패가 트랜잭션을 롤백 대상으로 만들어, 기록은 이미 남았는데도 응답이 실패한다.
     */
    fun recordSourceUrlClick(userId: Long, jobId: Long) {
        jobReader.readPublished(jobId)
        jobSourceUrlClickAppender.append(userId, jobId)
    }

    /** 인기·비슷한 공고도 목록과 같은 항목으로 보여 주므로 북마크 여부와 지표를 목록과 같은 방식으로 채운다. */
    private fun toSummaries(userId: Long?, jobs: List<Job>): List<UserJobSummary> {
        if (jobs.isEmpty()) {
            return emptyList()
        }
        val jobIds = jobs.map(Job::requiredId)
        val bookmarkedJobIds = readBookmarkedJobIds(userId, jobIds)
        val metrics = jobMetricReader.readAll(jobIds)
        return jobs.map { job ->
            val jobId = job.requiredId()
            UserJobSummary.from(
                job = job,
                bookmarked = jobId in bookmarkedJobIds,
                metric = metrics[jobId] ?: JobMetricDto.EMPTY,
            )
        }
    }

    /** 비로그인 조회에서는 북마크 저장소를 아예 건드리지 않는다. */
    private fun readBookmarkedJobIds(userId: Long?, jobIds: Collection<Long>): Set<Long> =
        if (userId == null) emptySet() else jobBookmarkReader.readBookmarkedJobIds(userId, jobIds)

    companion object {
        /** 메인 화면의 인기 공고 영역에 보여 주는 개수다. */
        const val POPULAR_JOB_LIMIT = 4

        /** 비슷한 공고 영역에 보여 주는 개수다. */
        const val SIMILAR_JOB_LIMIT = 4
    }
}

/** 희망 값은 렛츠커리어에서 온 자유 문자열이라 "IT, 금융"처럼 쉼표로 여러 값을 담을 수 있다. */
private fun String?.splitWishValues(): List<String> =
    this?.split(",")?.map(String::trim)?.filter(String::isNotEmpty)?.distinct().orEmpty()
