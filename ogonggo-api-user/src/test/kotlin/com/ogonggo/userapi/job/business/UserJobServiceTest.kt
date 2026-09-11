package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceUrlClickAppender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

class UserJobServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobBookmarkReader = Mockito.mock(JobBookmarkReader::class.java)
    private val jobMetricReader = Mockito.mock(JobMetricReader::class.java)
    private val jobSourceUrlClickAppender = Mockito.mock(JobSourceUrlClickAppender::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val service = UserJobService(
        jobReader,
        jobBookmarkReader,
        jobMetricReader,
        jobSourceUrlClickAppender,
        eventPublisher,
    )

    @Test
    fun `게시된 공고를 조회해 사용자 결과로 변환한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.id).thenReturn(1L)
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.employmentType).thenReturn(EmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(ExperienceType.EXPERIENCED)
        Mockito.`when`(job.educationLevel).thenReturn(EducationLevel.ANY)
        Mockito.`when`(job.region).thenReturn("서울")
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.PERIOD)
        Mockito.`when`(job.companyAndTeamIntroduction).thenReturn("회사 및 팀 소개")
        Mockito.`when`(job.responsibilities).thenReturn("주요 업무")
        Mockito.`when`(job.qualifications).thenReturn("자격 요건")
        Mockito.`when`(job.preferredQualifications).thenReturn("우대 사항")
        Mockito.`when`(job.compensation).thenReturn("급여 및 처우")
        Mockito.`when`(job.benefits).thenReturn("복지 및 혜택")
        Mockito.`when`(job.hiringProcess).thenReturn("채용 절차")
        Mockito.`when`(jobReader.readPublished(1L)).thenReturn(job)
        Mockito.`when`(jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(1L))).thenReturn(setOf(1L))
        Mockito.`when`(jobMetricReader.read(1L)).thenReturn(JobMetricDto(viewCount = 8, bookmarkCount = 3, commentCount = 1))

        val result = service.getJob(USER_ID, 1L)

        assertEquals(1L, result.id)
        assertEquals("오공고", result.companyName)
        assertEquals("백엔드 개발자", result.title)
        assertEquals("주요 업무", result.responsibilities)
        assertEquals("자격 요건", result.qualifications)
        assertEquals(true, result.bookmarked)
        assertEquals(8L, result.viewCount)
        assertEquals(3L, result.bookmarkCount)
        assertEquals(1L, result.commentCount)
        Mockito.verify(jobReader).readPublished(1L)
    }

    @Test
    fun `상세 조회는 지표를 읽은 뒤 조회 이벤트를 발행한다`() {
        val job = createJobMock()
        Mockito.`when`(jobReader.readPublished(1L)).thenReturn(job)
        Mockito.`when`(jobMetricReader.read(1L)).thenReturn(JobMetricDto(viewCount = 1, bookmarkCount = 0, commentCount = 0))

        val result = service.getJob(USER_ID, 1L)

        val inOrder = Mockito.inOrder(jobMetricReader, eventPublisher)
        inOrder.verify(jobMetricReader).read(1L)
        inOrder.verify(eventPublisher).publishEvent(JobViewedEvent(1L))
        assertEquals(1L, result.viewCount)
    }

    @Test
    fun `게시된 공고 목록을 페이지 결과로 변환한다`() {
        val job = createJobMock()
        Mockito.`when`(jobReader.readPublishedPage(JobSearchCondition.NONE, JobSortType.LATEST, 0, 20)).thenReturn(
            JobPageDto(
                jobs = listOf(job),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
                hasNext = false,
            ),
        )
        Mockito.`when`(jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(1L))).thenReturn(emptySet())
        Mockito.`when`(jobMetricReader.readAll(listOf(1L))).thenReturn(
            mapOf(1L to JobMetricDto(viewCount = 5, bookmarkCount = 2, commentCount = 0)),
        )

        val result = service.getJobs(USER_ID, JobSearchCondition.NONE, JobSortType.LATEST, 0, 20)

        assertEquals(1, result.items.size)
        assertEquals(1L, result.totalElements)
        assertEquals("백엔드 개발자", result.items.single().title)
        assertEquals(false, result.items.single().bookmarked)
        assertEquals(5L, result.items.single().viewCount)
        assertEquals(2L, result.items.single().bookmarkCount)
        Mockito.verify(jobReader).readPublishedPage(JobSearchCondition.NONE, JobSortType.LATEST, 0, 20)
    }

    @Test
    fun `비로그인 조회는 북마크 저장소를 조회하지 않고 모두 북마크되지 않은 것으로 본다`() {
        val job = createJobMock()
        Mockito.`when`(jobReader.readPublished(1L)).thenReturn(job)
        Mockito.`when`(jobReader.readPublishedPage(JobSearchCondition.NONE, JobSortType.LATEST, 0, 20)).thenReturn(
            JobPageDto(
                jobs = listOf(job),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
                hasNext = false,
            ),
        )
        Mockito.`when`(jobMetricReader.read(1L))
            .thenReturn(JobMetricDto(viewCount = 5, bookmarkCount = 2, commentCount = 0))
        Mockito.`when`(jobMetricReader.readAll(listOf(1L))).thenReturn(
            mapOf(1L to JobMetricDto(viewCount = 5, bookmarkCount = 2, commentCount = 0)),
        )

        Mockito.`when`(jobReader.readPopularRecruiting(UserJobService.POPULAR_JOB_LIMIT)).thenReturn(listOf(job))

        assertEquals(false, service.getJob(null, 1L).bookmarked)
        assertEquals(false, service.getJobs(null, JobSearchCondition.NONE, JobSortType.LATEST, 0, 20).items.single().bookmarked)
        assertEquals(false, service.getPopularJobs(null).single().bookmarked)

        Mockito.verifyNoInteractions(jobBookmarkReader)
    }

    @Test
    fun `인기 공고는 네 건을 요청해 북마크 여부와 지표를 채운다`() {
        val popular = createJobMock()
        val other = createJobMock()
        Mockito.`when`(other.id).thenReturn(2L)
        Mockito.`when`(jobReader.readPopularRecruiting(4)).thenReturn(listOf(popular, other))
        Mockito.`when`(jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(1L, 2L))).thenReturn(setOf(2L))
        Mockito.`when`(jobMetricReader.readAll(listOf(1L, 2L))).thenReturn(
            mapOf(
                1L to JobMetricDto(viewCount = 9, bookmarkCount = 1, commentCount = 0),
                2L to JobMetricDto(viewCount = 7, bookmarkCount = 2, commentCount = 0),
            ),
        )

        val result = service.getPopularJobs(USER_ID)

        assertEquals(listOf(1L, 2L), result.map { it.id })
        assertEquals(listOf(false, true), result.map { it.bookmarked })
        assertEquals(listOf(9L, 7L), result.map { it.viewCount })
        assertEquals(listOf(1L, 2L), result.map { it.bookmarkCount })
        Mockito.verify(jobReader).readPopularRecruiting(4)
    }

    @Test
    fun `원문 이동은 게시된 공고를 확인한 뒤 사용자를 기록한다`() {
        val job = createJobMock()
        Mockito.`when`(jobReader.readPublished(1L)).thenReturn(job)

        service.recordSourceUrlClick(USER_ID, 1L)

        val inOrder = Mockito.inOrder(jobReader, jobSourceUrlClickAppender)
        inOrder.verify(jobReader).readPublished(1L)
        inOrder.verify(jobSourceUrlClickAppender).append(USER_ID, 1L)
    }

    @Test
    fun `달력 조회 기간을 일시 경계로 변환하고 필요한 필드만 반환한다`() {
        val job = createJobMock()
        val startAt = LocalDateTime.of(2026, 8, 10, 9, 0)
        val endAt = LocalDateTime.of(2026, 8, 31, 23, 59)
        Mockito.`when`(job.recruitmentStartAt).thenReturn(startAt)
        Mockito.`when`(job.recruitmentEndAt).thenReturn(endAt)
        Mockito.`when`(
            jobReader.readPublishedCalendar(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 9, 1, 0, 0),
            ),
        ).thenReturn(listOf(job))

        val result = service.getJobCalendar(
            from = LocalDate.of(2026, 8, 1),
            to = LocalDate.of(2026, 8, 31),
        )

        assertEquals(1L, result.single().id)
        assertEquals("오공고", result.single().companyName)
        assertEquals(startAt, result.single().recruitmentStartAt)
        assertEquals(endAt, result.single().recruitmentEndAt)
    }

    private fun createJobMock(): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.id).thenReturn(1L)
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.employmentType).thenReturn(EmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(ExperienceType.EXPERIENCED)
        Mockito.`when`(job.educationLevel).thenReturn(EducationLevel.ANY)
        Mockito.`when`(job.region).thenReturn("서울")
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.PERIOD)
    }

    companion object {
        private const val USER_ID = 17L
    }
}
