package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobContentEditDto
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
        when (command.visibility) {
            AdminContentVisibility.VISIBLE -> jobManager.publish(job)
            AdminContentVisibility.HIDDEN -> jobManager.hide(job)
            null -> Unit
        }
        if (command.title != null || command.contents.isNotEmpty()) {
            jobManager.editContent(job, JobContentEditDto(title = command.title, contents = command.contents))
        }
    }

    /** 반려 기록은 지우지 않는다. 반려 보관에서 "반려하고 지웠다"는 기록으로 남는다. */
    @Transactional
    fun deleteJob(jobId: Long) {
        jobManager.delete(jobReader.readForDelete(jobId), LocalDateTime.now(clock))
    }

    private fun changeReview(job: Job, reviewStatus: ReviewStatus, now: LocalDateTime) {
        when (reviewStatus) {
            ReviewStatus.APPROVED -> jobManager.approveReview(job, now)
            ReviewStatus.PENDING -> jobManager.requestReview(job, now)
            ReviewStatus.REJECTED -> throw IllegalArgumentException("반려는 사유와 함께 검수 화면에서 처리합니다.")
        }
    }
}
