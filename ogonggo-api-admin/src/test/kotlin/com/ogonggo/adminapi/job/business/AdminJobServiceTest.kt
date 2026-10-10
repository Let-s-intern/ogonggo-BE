package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
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
import com.ogonggo.core.job.implement.dto.TodayJobDto
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
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
        val job = lockedJob(reviewStatus = ContentReviewStatus.PENDING)

        service.updateJob(
            JOB_ID,
            AdminJobUpdateCommand(visibility = AdminContentVisibility.HIDDEN, reviewStatus = ContentReviewStatus.APPROVED),
        )

        val order = Mockito.inOrder(jobManager)
        order.verify(jobManager).approveReview(job, NOW)
        order.verify(jobManager).hide(job)
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `이미 같은 검수 상태면 다시 전이하지 않아 노출이 되돌아가지 않는다`() {
        lockedJob(reviewStatus = ContentReviewStatus.APPROVED)

        service.updateJob(JOB_ID, AdminJobUpdateCommand(reviewStatus = ContentReviewStatus.APPROVED))

        Mockito.verifyNoInteractions(jobManager)
    }

    @Test
    fun `검수 대기로 바꾸면 검수를 다시 요청한다`() {
        val job = lockedJob(reviewStatus = ContentReviewStatus.APPROVED)

        service.updateJob(JOB_ID, AdminJobUpdateCommand(reviewStatus = ContentReviewStatus.PENDING))

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
    fun `노출 일괄 변경은 이미 같은 노출인 공고를 건드리지 않아 초안을 숨겨도 초안으로 남는다`() {
        val draft = jobWithStatus(JobPublicationStatus.DRAFT)
        val hidden = jobWithStatus(JobPublicationStatus.HIDDEN)
        Mockito.`when`(jobReader.readAllForUpdate(listOf(1L, 2L))).thenReturn(listOf(draft, hidden))

        service.changeVisibilities(AdminJobVisibilityChangeCommand(listOf(1L, 2L), AdminContentVisibility.HIDDEN))

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
        val first = TodayJobDto.Response(todayJob(id = 5L), "추천 제목", "추천 설명")
        val second = TodayJobDto.Response(todayJob(id = 2L), "다른 제목", "다른 설명")
        Mockito.`when`(jobReader.readToday()).thenReturn(listOf(first, second))
        Mockito.`when`(jobMetricReader.readAll(listOf(5L, 2L)))
            .thenReturn(mapOf(2L to JobMetricDto(viewCount = 9, bookmarkCount = 1, commentCount = 0)))

        val result = service.getTodayJobs()

        assertEquals(listOf(5L, 2L), result.map { it.job.id })
        assertEquals(listOf(0L, 9L), result.map { it.job.viewCount })
        assertEquals(listOf("추천 제목", "다른 제목"), result.map { it.recommendationTitle })
    }

    @Test
    fun `오늘의 공고 설정은 받은 순서 그대로 현재 시각과 함께 넘긴다`() {
        val todayJobs = listOf(
            TodayJobDto.Request(7L, "추천 제목", "추천 설명"),
            TodayJobDto.Request(3L, "다른 제목", "다른 설명"),
        )

        service.replaceTodayJobs(todayJobs)

        Mockito.verify(todayJobManager).replace(todayJobs, NOW)
    }

    private fun todayJob(id: Long): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.id).thenReturn(id)
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.employmentType).thenReturn(JobEmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(JobExperienceType.EXPERIENCED)
        Mockito.`when`(job.educationLevel).thenReturn(JobEducationLevel.ANY)
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.ALWAYS_OPEN)
        Mockito.`when`(job.publicationStatus).thenReturn(JobPublicationStatus.PUBLISHED)
        Mockito.`when`(job.source).thenReturn(ContentSource.CRAWLER)
        Mockito.`when`(job.recruitmentStatus).thenReturn(JobRecruitmentStatus.RECRUITING)
        Mockito.`when`(job.createdAt).thenReturn(NOW)
    }

    private fun jobWithStatus(publicationStatus: JobPublicationStatus): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.publicationStatus).thenReturn(publicationStatus)
    }

    private fun lockedJob(reviewStatus: ContentReviewStatus?): Job {
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
