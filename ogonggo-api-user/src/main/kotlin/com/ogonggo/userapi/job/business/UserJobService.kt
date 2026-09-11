package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceUrlClickAppender
import com.ogonggo.core.job.implement.dto.JobMetricDto
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserJobService(
    private val jobReader: JobReader,
    private val jobBookmarkReader: JobBookmarkReader,
    private val jobMetricReader: JobMetricReader,
    private val jobSourceUrlClickAppender: JobSourceUrlClickAppender,
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

    /** 인기 공고도 목록과 같은 항목으로 보여 주므로 북마크 여부와 지표를 목록과 같은 방식으로 채운다. */
    fun getPopularJobs(userId: Long?): List<UserJobSummary> {
        val jobs = jobReader.readPopularRecruiting(POPULAR_JOB_LIMIT)
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

    /** 비로그인 조회에서는 북마크 저장소를 아예 건드리지 않는다. */
    private fun readBookmarkedJobIds(userId: Long?, jobIds: Collection<Long>): Set<Long> =
        if (userId == null) emptySet() else jobBookmarkReader.readBookmarkedJobIds(userId, jobIds)

    companion object {
        /** 메인 화면의 인기 공고 영역에 보여 주는 개수다. */
        const val POPULAR_JOB_LIMIT = 4
    }
}
