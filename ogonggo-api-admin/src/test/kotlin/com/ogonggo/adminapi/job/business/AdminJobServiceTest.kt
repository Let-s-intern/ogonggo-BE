package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.TodayJobManager
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import org.junit.jupiter.api.Assertions.assertEquals
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
    private val todayJobManager = Mockito.mock(TodayJobManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminJobService(jobReader, jobManager, jobMetricReader, todayJobManager, clock)

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
    fun `노출 일괄 변경은 잠가 읽은 공고를 같은 노출로 바꾸고 검수 상태는 건드리지 않는다`() {
        val first = jobWithStatus(JobPublicationStatus.PUBLISHED)
        val second = jobWithStatus(JobPublicationStatus.PUBLISHED)
        Mockito.`when`(jobReader.readAllForUpdate(listOf(3L, 1L))).thenReturn(listOf(first, second))

        service.changeVisibilities(AdminJobVisibilityChangeCommand(listOf(3L, 1L), AdminContentVisibility.HIDDEN))

        Mockito.verify(jobManager).hide(first)
        Mockito.verify(jobManager).hide(second)
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `노출 일괄 변경은 이미 같은 노출인 공고를 건드리지 않아 보관 공고를 숨겨도 실패하지 않는다`() {
        val archived = jobWithStatus(JobPublicationStatus.ARCHIVED)
        val draft = jobWithStatus(JobPublicationStatus.DRAFT)
        val hidden = jobWithStatus(JobPublicationStatus.HIDDEN)
        Mockito.`when`(jobReader.readAllForUpdate(listOf(1L, 2L, 3L))).thenReturn(listOf(archived, draft, hidden))

        service.changeVisibilities(AdminJobVisibilityChangeCommand(listOf(1L, 2L, 3L), AdminContentVisibility.HIDDEN))

        Mockito.verifyNoInteractions(jobManager)
    }

    @Test
    fun `삭제는 이미 삭제된 공고까지 잠가 찾아 멱등하게 처리한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readForDelete(JOB_ID)).thenReturn(job)

        service.deleteJob(JOB_ID)

        Mockito.verify(jobManager).delete(job, NOW)
    }

    @Test
    fun `오늘의 공고는 고른 순서를 유지하고 지표가 없는 공고는 0으로 채운다`() {
        val first = todayJob(id = 5L)
        val second = todayJob(id = 2L)
        Mockito.`when`(jobReader.readToday()).thenReturn(listOf(first, second))
        Mockito.`when`(jobMetricReader.readAll(listOf(5L, 2L)))
            .thenReturn(mapOf(2L to JobMetricDto(viewCount = 9, bookmarkCount = 1, commentCount = 0)))

        val result = service.getTodayJobs()

        assertEquals(listOf(5L, 2L), result.map { it.id })
        assertEquals(listOf(0L, 9L), result.map { it.viewCount })
    }

    @Test
    fun `오늘의 공고 설정은 받은 순서 그대로 현재 시각과 함께 넘긴다`() {
        service.replaceTodayJobs(listOf(7L, 3L))

        Mockito.verify(todayJobManager).replace(listOf(7L, 3L), NOW)
    }

    private fun todayJob(id: Long): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.id).thenReturn(id)
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.employmentType).thenReturn(EmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(ExperienceType.EXPERIENCED)
        Mockito.`when`(job.educationLevel).thenReturn(EducationLevel.ANY)
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.ALWAYS_OPEN)
        Mockito.`when`(job.publicationStatus).thenReturn(JobPublicationStatus.PUBLISHED)
        Mockito.`when`(job.source).thenReturn(ContentSource.CRAWLER)
        Mockito.`when`(job.recruitmentStatus(NOW)).thenReturn(JobRecruitmentStatus.RECRUITING)
        Mockito.`when`(job.createdAt).thenReturn(NOW)
    }

    private fun jobWithStatus(publicationStatus: JobPublicationStatus): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.publicationStatus).thenReturn(publicationStatus)
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
