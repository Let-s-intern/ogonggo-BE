package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobBookmarkManager
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobPage
import com.ogonggo.core.job.implement.JobReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class UserJobBookmarkServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobBookmarkReader = Mockito.mock(JobBookmarkReader::class.java)
    private val jobBookmarkManager = Mockito.mock(JobBookmarkManager::class.java)
    private val jobMetricReader = Mockito.mock(JobMetricReader::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)
    private val service = UserJobBookmarkService(
        jobReader,
        jobBookmarkReader,
        jobBookmarkManager,
        jobMetricReader,
        eventPublisher,
        clock,
    )

    @Test
    fun `게시된 공고를 확인한 뒤 북마크를 등록한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readPublishedForUpdate(JOB_ID)).thenReturn(job)

        service.addBookmark(USER_ID, JOB_ID)

        val inOrder = Mockito.inOrder(jobReader, jobBookmarkManager, eventPublisher)
        inOrder.verify(jobReader).readPublishedForUpdate(JOB_ID)
        inOrder.verify(jobBookmarkManager).append(USER_ID, JOB_ID)
        inOrder.verify(eventPublisher).publishEvent(JobBookmarkChangedEvent(JOB_ID))
    }

    @Test
    fun `삭제된 공고도 잠근 뒤 북마크를 멱등하게 해제한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readIncludingDeletedForUpdate(JOB_ID)).thenReturn(job)

        service.deleteBookmark(USER_ID, JOB_ID)

        Mockito.verify(jobBookmarkManager).delete(USER_ID, JOB_ID, NOW)
        Mockito.verify(eventPublisher).publishEvent(JobBookmarkChangedEvent(JOB_ID))
    }

    @Test
    fun `내 북마크 목록은 모든 항목을 북마크 상태로 변환한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.id).thenReturn(JOB_ID)
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.employmentType).thenReturn(EmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(ExperienceType.EXPERIENCED)
        Mockito.`when`(job.educationLevel).thenReturn(EducationLevel.ANY)
        Mockito.`when`(job.region).thenReturn("서울")
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.PERIOD)
        Mockito.`when`(jobBookmarkReader.readBookmarkedPublishedPage(USER_ID, 0, 10)).thenReturn(
            JobPage(listOf(job), 0, 10, 1, 1, false),
        )

        val result = service.getBookmarks(USER_ID, 0, 10)

        assertEquals(true, result.items.single().bookmarked)
    }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
        private const val USER_ID = 17L
        private const val JOB_ID = 3L
    }
}
