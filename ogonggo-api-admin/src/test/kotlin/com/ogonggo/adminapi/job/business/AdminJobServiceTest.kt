package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceReader
import com.ogonggo.core.job.implement.TodayJobManager
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.implement.dto.TodayJobDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    private val jobSourceReader = Mockito.mock(JobSourceReader::class.java)
    private val service = AdminJobService(jobReader, jobManager, jobMetricReader, todayJobManager, jobSourceReader, clock)

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
    fun `공고 정보를 보내면 보낸 값만 바꾸고 나머지는 현재 값으로 한 번에 고친다`() {
        val job = lockedJob(reviewStatus = null).also(::stubDetails)
        val endAt = LocalDateTime.of(2026, 9, 30, 23, 59)

        service.updateJob(
            JOB_ID,
            AdminJobUpdateCommand(details = AdminJobDetailsPatch(experienceMinYears = 5, recruitmentEndAt = endAt)),
        )

        Mockito.verify(jobManager).update(
            job,
            currentValues().copy(experienceMinYears = 5, recruitmentEndAt = endAt),
            NOW,
        )
        Mockito.verifyNoMoreInteractions(jobManager)
    }

    @Test
    fun `비우라고 한 공고 정보는 지운다`() {
        val job = mockJob().also(::stubDetails)

        val merged = AdminJobDetailsPatch(
            cleared = setOf(AdminJobClearableField.EXPERIENCE_MIN_YEARS, AdminJobClearableField.RECRUITMENT_END_AT),
        ).mergeInto(job)

        assertEquals(currentValues().copy(experienceMinYears = null, recruitmentEndAt = null), merged)
    }

    @Test
    fun `직군을 바꾸면서 직무를 보내지 않으면 다른 직군의 기존 직무를 지운다`() {
        val job = mockJob().also(::stubDetails)

        val merged = AdminJobDetailsPatch(jobField = JobField.IT_DEVELOPMENT).mergeInto(job)

        assertEquals(JobField.IT_DEVELOPMENT, merged.jobField)
        assertNull(merged.jobRole)
    }

    @Test
    fun `직무만 보내면 직군도 그 직무의 직군으로 맞춘다`() {
        val job = mockJob().also(::stubDetails)

        val merged = AdminJobDetailsPatch(jobRole = JobRole.IT_BACKEND).mergeInto(job)

        assertEquals(JobField.IT_DEVELOPMENT, merged.jobField)
        assertEquals(JobRole.IT_BACKEND, merged.jobRole)
    }

    @Test
    fun `직군과 맞지 않는 직무를 함께 보내면 400 예외를 던진다`() {
        val job = mockJob().also(::stubDetails)

        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            AdminJobDetailsPatch(jobField = JobField.GAME, jobRole = JobRole.IT_BACKEND).mergeInto(job)
        }

        assertEquals("jobRole", exception.fieldName)
    }

    @Test
    fun `시·도를 바꾸면서 시·군·구를 보내지 않으면 다른 시·도의 기존 시·군·구를 지운다`() {
        val job = mockJob().also(::stubDetails)

        val merged = AdminJobDetailsPatch(region = Region.GYEONGGI).mergeInto(job)

        assertEquals(Region.GYEONGGI, merged.region)
        assertNull(merged.subRegion)
    }

    @Test
    fun `상시 채용으로 바꾸면 모집 종료 일시를 지우고 종료 일시를 함께 보내면 400 예외를 던진다`() {
        val job = mockJob().also(::stubDetails)

        val merged = AdminJobDetailsPatch(recruitmentType = JobRecruitmentType.ALWAYS_OPEN).mergeInto(job)
        assertEquals(JobRecruitmentType.ALWAYS_OPEN, merged.recruitmentType)
        assertNull(merged.recruitmentEndAt)

        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            AdminJobDetailsPatch(recruitmentType = JobRecruitmentType.ALWAYS_OPEN, recruitmentEndAt = NOW).mergeInto(job)
        }
        assertEquals("recruitmentEndAt", exception.fieldName)
    }

    @Test
    fun `모집 시작 일시를 종료 일시보다 늦게 바꾸면 400 예외를 던진다`() {
        val job = mockJob().also(::stubDetails)

        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            AdminJobDetailsPatch(recruitmentStartAt = LocalDateTime.of(2026, 10, 1, 0, 0)).mergeInto(job)
        }

        assertEquals("recruitmentStartAt", exception.fieldName)
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

    private fun mockJob(): Job = Mockito.mock(Job::class.java)

    /** [currentValues]와 같은 값을 갖도록 공고 정보를 채운다. 본문 칸은 비워 둔다. */
    private fun stubDetails(job: Job) {
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.jobField).thenReturn(JobField.PLANNING_STRATEGY)
        Mockito.`when`(job.jobRole).thenReturn(JobRole.DESIGN_UX_UI_PRODUCT)
        Mockito.`when`(job.employmentType).thenReturn(JobEmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(JobExperienceType.EXPERIENCED)
        Mockito.`when`(job.experienceMinYears).thenReturn(3)
        Mockito.`when`(job.educationLevel).thenReturn(JobEducationLevel.ANY)
        Mockito.`when`(job.region).thenReturn(Region.SEOUL)
        Mockito.`when`(job.subRegion).thenReturn(SubRegion.SEOUL_GANGNAM_GU)
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.PERIOD)
        Mockito.`when`(job.recruitmentStartAt).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0))
        Mockito.`when`(job.recruitmentEndAt).thenReturn(LocalDateTime.of(2026, 9, 14, 23, 59))
        // Mockito는 감싼 숫자·불리언 게터에 0·false를 돌려주므로 비어 있는 값으로 맞춘다.
        Mockito.`when`(job.recruitmentHeadcount).thenReturn(null)
        Mockito.`when`(job.closesWhenFilled).thenReturn(null)
        Mockito.`when`(job.autoCloseEnabled).thenReturn(null)
    }

    private fun currentValues(): JobUpdateDto = JobUpdateDto(
        companyName = "오공고",
        title = "백엔드 개발자",
        jobField = JobField.PLANNING_STRATEGY,
        jobRole = JobRole.DESIGN_UX_UI_PRODUCT,
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.EXPERIENCED,
        experienceMinYears = 3,
        educationLevel = JobEducationLevel.ANY,
        region = Region.SEOUL,
        subRegion = SubRegion.SEOUL_GANGNAM_GU,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 9, 14, 23, 59),
    )

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
