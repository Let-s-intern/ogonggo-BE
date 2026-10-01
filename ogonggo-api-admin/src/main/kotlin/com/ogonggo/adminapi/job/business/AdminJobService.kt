package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.TodayJobManager
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.review.domain.ReviewStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminJobService(
    private val jobReader: JobReader,
    private val jobManager: JobManager,
    private val jobMetricReader: JobMetricReader,
    private val todayJobManager: TodayJobManager,
    private val clock: Clock,
) {

    /** 모집 상태 필터와 응답의 모집 상태가 같은 기준 시각을 쓰도록 한 번만 만든다. */
    fun getJobs(
        condition: JobManagementSearchCondition,
        sortType: JobSortType,
        page: Int,
        size: Int,
    ): AdminJobPageResult {
        val now = LocalDateTime.now(clock)
        val result = jobReader.readManagementPage(condition, sortType, page, size, now)
        return AdminJobPageResult.from(
            result = result,
            metrics = jobMetricReader.readAll(result.jobs.map { it.requiredId() }),
            now = now,
        )
    }

    fun getJob(jobId: Long): AdminJobResult =
        AdminJobResult.from(jobReader.read(jobId), jobMetricReader.read(jobId), LocalDateTime.now(clock))

    /**
     * 검수 상태를 먼저 바꾸고 노출을 바꾼다. 승인은 곧 노출이므로 승인과 숨김을 함께 보내면 숨김이 남아야 한다.
     * 이미 같은 검수 상태면 다시 전이하지 않는다. 화면이 현재 값을 함께 보내도 노출이 되돌아가지 않게 하기 위해서다.
     */
    @Transactional
    fun updateJob(jobId: Long, command: AdminJobUpdateCommand) {
        val now = LocalDateTime.now(clock)
        val job = jobReader.readForUpdate(jobId)

        command.reviewStatus
            ?.takeIf { it != job.reviewStatus }
            ?.let { changeReview(job, it, now) }
        command.visibility?.let { changeVisibility(job, it) }
        if (command.title != null || command.contents.isNotEmpty()) {
            jobManager.editContent(job, JobContentEditDto(title = command.title, contents = command.contents))
        }
    }

    /**
     * 한 트랜잭션에서 모두 바꾼다. 없거나 삭제된 공고, 승인 전 기업회원 공고의 노출처럼 하나라도 실패하면 아무것도 바꾸지 않는다.
     * 일부만 바뀌면 운영자가 어느 공고가 바뀌었는지 다시 찾아야 하기 때문이다.
     */
    @Transactional
    fun changeVisibilities(command: AdminJobVisibilityChangeCommand) {
        jobReader.readAllForUpdate(command.jobIds).forEach { job -> changeVisibility(job, command.visibility) }
    }

    /** 반려 기록은 지우지 않는다. 반려 보관에서 "반려하고 지웠다"는 기록으로 남는다. */
    @Transactional
    fun deleteJob(jobId: Long) {
        jobManager.delete(jobReader.readForDelete(jobId), LocalDateTime.now(clock))
    }

    /** 숨긴 공고도 운영자가 알아보고 뺄 수 있도록 게시 상태와 무관하게 고른 순서대로 보여 준다. */
    fun getTodayJobs(): List<AdminJobSummary> {
        val now = LocalDateTime.now(clock)
        val jobs = jobReader.readToday()
        val metrics = jobMetricReader.readAll(jobs.map { it.requiredId() })
        return jobs.map { job -> AdminJobSummary.from(job, metrics[job.requiredId()] ?: JobMetricDto.EMPTY, now) }
    }

    @Transactional
    fun replaceTodayJobs(jobIds: List<Long>) {
        todayJobManager.replace(jobIds, LocalDateTime.now(clock))
    }

    private fun changeVisibility(job: Job, visibility: AdminContentVisibility) {
        when (visibility) {
            AdminContentVisibility.VISIBLE -> jobManager.publish(job)
            AdminContentVisibility.HIDDEN -> jobManager.hide(job)
        }
    }

    private fun changeReview(job: Job, reviewStatus: ReviewStatus, now: LocalDateTime) {
        when (reviewStatus) {
            ReviewStatus.APPROVED -> jobManager.approveReview(job, now)
            ReviewStatus.PENDING -> jobManager.requestReview(job, now)
            ReviewStatus.REJECTED -> throw IllegalArgumentException("반려는 사유와 함께 검수 화면에서 처리합니다.")
        }
    }
}
