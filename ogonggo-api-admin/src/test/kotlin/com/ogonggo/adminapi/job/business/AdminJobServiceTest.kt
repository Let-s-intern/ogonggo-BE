package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.review.domain.ReviewStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdminJobServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobManager = Mockito.mock(JobManager::class.java)
    private val jobMetricReader = Mockito.mock(JobMetricReader::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminJobService(jobReader, jobManager, jobMetricReader, clock)

    @Test
    fun `승인과 숨김을 함께 보내면 승인한 뒤 숨긴다`() {
        val job = lockedJob(reviewStatus = ReviewStatus.PENDING)

        service.updateJob(
            JOB_ID,
            AdminJobUpdateCommand(visibility = AdminContentVisibility.HIDDEN, reviewStatus = ReviewStatus.APPROVED),
        )

        val order = Mockito.inOrder(jobManager)
        order.verify(jobManager).approveReview(job, NOW)
        order.verify(jobManager).hide(job)
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `이미 같은 검수 상태면 다시 전이하지 않아 노출이 되돌아가지 않는다`() {
        lockedJob(reviewStatus = ReviewStatus.APPROVED)

        service.updateJob(JOB_ID, AdminJobUpdateCommand(reviewStatus = ReviewStatus.APPROVED))

        Mockito.verifyNoInteractions(jobManager)
    }

    @Test
    fun `검수 대기로 바꾸면 검수를 다시 요청한다`() {
        val job = lockedJob(reviewStatus = ReviewStatus.APPROVED)

        service.updateJob(JOB_ID, AdminJobUpdateCommand(reviewStatus = ReviewStatus.PENDING))

        Mockito.verify(jobManager).requestReview(job, NOW)
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `노출만 보내면 게시 상태만 바꾼다`() {
        val job = lockedJob(reviewStatus = null)

        service.updateJob(JOB_ID, AdminJobUpdateCommand(visibility = AdminContentVisibility.VISIBLE))

        Mockito.verify(jobManager).publish(job)
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `제목과 본문 칸을 보내면 내용만 고친다`() {
        val job = lockedJob(reviewStatus = null)
        val contents = mapOf(JobContentField.RESPONSIBILITIES to "고친 업무", JobContentField.BENEFITS to null)

        service.updateJob(JOB_ID, AdminJobUpdateCommand(title = "고친 제목", contents = contents))

        Mockito.verify(jobManager).editContent(job, JobContentEditDto(title = "고친 제목", contents = contents))
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `삭제는 이미 삭제된 공고까지 잠가 찾아 멱등하게 처리한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readForDelete(JOB_ID)).thenReturn(job)

        service.deleteJob(JOB_ID)

        Mockito.verify(jobManager).delete(job, NOW)
    }

    private fun lockedJob(reviewStatus: ReviewStatus?): Job {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.reviewStatus).thenReturn(reviewStatus)
        Mockito.`when`(jobReader.readForUpdate(JOB_ID)).thenReturn(job)
        return job
    }

    companion object {
        private const val JOB_ID = 1L
        private val NOW = LocalDateTime.of(2026, 8, 27, 12, 0)
    }
}
