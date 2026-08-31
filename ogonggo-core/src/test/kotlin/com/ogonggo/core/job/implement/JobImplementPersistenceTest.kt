package com.ogonggo.core.job.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import com.ogonggo.core.job.persistence.JobMetricJpaRepository
import com.ogonggo.core.job.persistence.JobSourceUrlClickJpaRepository
import com.ogonggo.core.job.persistence.JobTagJpaRepository
import com.ogonggo.core.job.persistence.TagJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    JobReaderImpl::class,
    JobAppenderImpl::class,
    JobManagerImpl::class,
    JobBookmarkReaderImpl::class,
    JobBookmarkManagerImpl::class,
    JobMetricReaderImpl::class,
    JobMetricManagerImpl::class,
    JobTagAppenderImpl::class,
    JobSourceUrlClickAppenderImpl::class,
)
internal class JobImplementPersistenceTest @Autowired constructor(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobBookmarkReader: JobBookmarkReader,
    private val jobBookmarkManager: JobBookmarkManager,
    private val jobMetricReader: JobMetricReader,
    private val jobMetricManager: JobMetricManager,
    private val jobTagAppender: JobTagAppender,
    private val jobSourceUrlClickAppender: JobSourceUrlClickAppender,
    private val jobSourceUrlClickRepository: JobSourceUrlClickJpaRepository,
    private val tagRepository: TagJpaRepository,
    private val jobTagRepository: JobTagJpaRepository,
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
    private val jobMetricRepository: JobMetricJpaRepository,
) {

    @Test
    fun `Appender로 저장하고 Reader로 조회한다`() {
        val savedJob = jobAppender.append(createCommand())

        assertNotNull(savedJob.id)
        assertEquals(savedJob.id, jobReader.read(checkNotNull(savedJob.id)).id)
    }

    @Test
    fun `사용자 조회는 게시된 공고만 반환한다`() {
        val savedJob = jobAppender.append(createCommand())
        val jobId = checkNotNull(savedJob.id)

        val exception = assertThrows(EntityNotFoundException::class.java) { jobReader.readPublished(jobId) }
        assertEquals(JobErrorCode.JOB_NOT_FOUND, exception.errorCode)

        val lockedJob = jobReader.readForUpdate(jobId)
        jobManager.publish(lockedJob)

        assertEquals(JobPublicationStatus.PUBLISHED, jobReader.readPublished(jobId).publicationStatus)
    }

    @Test
    fun `사용자 공고 목록은 게시된 미삭제 공고를 최신순으로 페이징한다`() {
        val first = jobAppender.append(createCommand())
        val deleted = jobAppender.append(createCommand())
        val latest = jobAppender.append(createCommand())
        listOf(first, deleted, latest).forEach(jobManager::publish)
        jobManager.delete(deleted, java.time.LocalDateTime.of(2026, 8, 27, 12, 0))

        val firstPage = jobReader.readPublishedPage(page = 0, size = 1, sortType = JobSortType.LATEST)
        val secondPage = jobReader.readPublishedPage(page = 1, size = 1, sortType = JobSortType.LATEST)

        assertEquals(listOf(latest.id), firstPage.jobs.map { it.id })
        assertEquals(listOf(first.id), secondPage.jobs.map { it.id })
        assertEquals(2L, firstPage.totalElements)
        assertEquals(true, firstPage.hasNext)
        assertEquals(false, secondPage.hasNext)
    }

    @Test
    fun `사용자 공고 목록의 페이지 범위를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { jobReader.readPublishedPage(-1, 20, JobSortType.LATEST) }
        assertThrows(IllegalArgumentException::class.java) { jobReader.readPublishedPage(0, 0, JobSortType.LATEST) }
        assertThrows(IllegalArgumentException::class.java) { jobReader.readPublishedPage(0, 101, JobSortType.LATEST) }
    }

    @Test
    fun `공고 달력은 조회 기간과 모집 기간이 겹치는 게시 공고만 반환한다`() {
        val spanning = jobAppender.append(
            createCommand(
                recruitmentStartAt = LocalDateTime.of(2026, 7, 20, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 9, 5, 23, 59),
            ),
        )
        val inside = jobAppender.append(
            createCommand(
                recruitmentStartAt = LocalDateTime.of(2026, 8, 10, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 8, 20, 23, 59),
            ),
        )
        val before = jobAppender.append(
            createCommand(
                recruitmentStartAt = LocalDateTime.of(2026, 7, 1, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 7, 31, 23, 59),
            ),
        )
        val alwaysOpen = jobAppender.append(createCommand(recruitmentType = JobRecruitmentType.ALWAYS_OPEN))
        val draft = jobAppender.append(
            createCommand(
                recruitmentStartAt = LocalDateTime.of(2026, 8, 1, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
            ),
        )
        val deleted = jobAppender.append(
            createCommand(
                recruitmentStartAt = LocalDateTime.of(2026, 8, 1, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
            ),
        )
        listOf(spanning, inside, before, alwaysOpen, deleted).forEach(jobManager::publish)
        jobManager.delete(deleted, LocalDateTime.of(2026, 8, 27, 12, 0))

        val result = jobReader.readPublishedCalendar(
            rangeStart = LocalDateTime.of(2026, 8, 1, 0, 0),
            rangeEndExclusive = LocalDateTime.of(2026, 9, 1, 0, 0),
        )

        assertEquals(listOf(inside.id, spanning.id), result.map { it.id })
        assertEquals(JobPublicationStatus.DRAFT, draft.publicationStatus)
    }

    @Test
    fun `삭제된 공고는 Reader에서 조회하지 않는다`() {
        val savedJob = jobAppender.append(createCommand())
        val jobId = checkNotNull(savedJob.id)
        val lockedJob = jobReader.readForUpdate(jobId)

        jobManager.delete(lockedJob, java.time.LocalDateTime.of(2026, 8, 26, 12, 0))

        assertThrows(EntityNotFoundException::class.java) { jobReader.read(jobId) }
        assertThrows(EntityNotFoundException::class.java) { jobReader.readForUpdate(jobId) }
    }

    @Test
    fun `북마크 등록과 해제와 재등록은 행을 보존하고 지표는 다시 세어 맞춘다`() {
        val job = jobAppender.append(createCommand())
        val jobId = checkNotNull(job.id)
        jobManager.publish(job)

        jobReader.readPublishedForUpdate(jobId)
        jobBookmarkManager.append(USER_ID, jobId)
        jobMetricManager.syncBookmarkCount(jobId)

        assertEquals(setOf(jobId), jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(jobId)))
        assertEquals(1L, jobMetricRepository.findByJobId(jobId)?.bookmarkCount)
        val duplicate = assertThrows(ConflictException::class.java) {
            jobBookmarkManager.append(USER_ID, jobId)
        }
        assertEquals(JobErrorCode.JOB_BOOKMARK_ALREADY_EXISTS, duplicate.errorCode)

        jobBookmarkManager.delete(USER_ID, jobId, NOW)
        jobBookmarkManager.delete(USER_ID, jobId, NOW.plusMinutes(1))
        jobMetricManager.syncBookmarkCount(jobId)

        assertEquals(emptySet<Long>(), jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(jobId)))
        assertEquals(0L, jobMetricRepository.findByJobId(jobId)?.bookmarkCount)
        assertEquals(NOW, jobBookmarkRepository.findByJobIdAndUserId(jobId, USER_ID)?.deletedAt)

        jobBookmarkManager.append(USER_ID, jobId)
        jobMetricManager.syncBookmarkCount(jobId)

        assertEquals(1L, jobMetricRepository.findByJobId(jobId)?.bookmarkCount)
        assertEquals(null, jobBookmarkRepository.findByJobIdAndUserId(jobId, USER_ID)?.deletedAt)
        assertEquals(1L, jobBookmarkRepository.count())
    }

    @Test
    fun `조회 수는 지표 행이 없으면 만들고 있으면 증가시킨다`() {
        val job = jobAppender.append(createCommand())
        val jobId = checkNotNull(job.id)

        jobMetricManager.increaseViewCount(jobId, NOW)

        assertEquals(1L, jobMetricReader.read(jobId).viewCount)

        jobMetricManager.increaseViewCount(jobId, NOW.plusMinutes(1))

        assertEquals(2L, jobMetricReader.read(jobId).viewCount)
        assertEquals(1L, jobMetricRepository.count())
    }

    @Test
    fun `지표를 한 번에 조회하며 지표 행이 없는 공고는 0으로 채운다`() {
        val viewed = jobAppender.append(createCommand())
        val untouched = jobAppender.append(createCommand())
        val viewedId = checkNotNull(viewed.id)
        val untouchedId = checkNotNull(untouched.id)
        jobMetricManager.increaseViewCount(viewedId, NOW)

        val metrics = jobMetricReader.readAll(listOf(viewedId, untouchedId))

        assertEquals(1L, metrics[viewedId]?.viewCount)
        assertEquals(0L, metrics[untouchedId]?.viewCount)
        assertEquals(emptyMap<Long, JobMetricData>(), jobMetricReader.readAll(emptyList()))
    }

    @Test
    fun `북마크 수 갱신은 여러 번 실행해도 결과가 같다`() {
        val job = jobAppender.append(createCommand())
        val jobId = checkNotNull(job.id)
        jobBookmarkManager.append(USER_ID, jobId)

        jobMetricManager.syncBookmarkCount(jobId)
        jobMetricManager.syncBookmarkCount(jobId)
        jobMetricManager.syncBookmarkCount(jobId)

        assertEquals(1L, jobMetricReader.read(jobId).bookmarkCount)
        assertEquals(1L, jobMetricRepository.count())
    }

    @Test
    fun `조회수순은 조회 수 내림차순이며 같으면 최신순으로 정렬한다`() {
        val quiet = jobAppender.append(createCommand())
        val tiedOlder = jobAppender.append(createCommand())
        val tiedNewer = jobAppender.append(createCommand())
        val popular = jobAppender.append(createCommand())
        listOf(quiet, tiedOlder, tiedNewer, popular).forEach(jobManager::publish)
        repeat(3) { jobMetricManager.increaseViewCount(checkNotNull(popular.id), NOW) }
        jobMetricManager.increaseViewCount(checkNotNull(tiedOlder.id), NOW)
        jobMetricManager.increaseViewCount(checkNotNull(tiedNewer.id), NOW)

        val page = jobReader.readPublishedPage(page = 0, size = 10, sortType = JobSortType.VIEW_COUNT)

        assertEquals(
            listOf(popular.id, tiedNewer.id, tiedOlder.id, quiet.id),
            page.jobs.map { it.id },
        )
        assertEquals(4L, page.totalElements)
    }

    @Test
    fun `조회수순도 게시된 미삭제 공고만 페이징한다`() {
        val published = jobAppender.append(createCommand())
        val deleted = jobAppender.append(createCommand())
        val draft = jobAppender.append(createCommand())
        listOf(published, deleted).forEach(jobManager::publish)
        jobManager.delete(deleted, NOW)

        val page = jobReader.readPublishedPage(page = 0, size = 10, sortType = JobSortType.VIEW_COUNT)

        assertEquals(listOf(published.id), page.jobs.map { it.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `원문 이동 기록은 사용자와 공고마다 한 행만 남는다`() {
        val jobId = checkNotNull(jobAppender.append(createCommand()).id)
        val otherJobId = checkNotNull(jobAppender.append(createCommand()).id)

        jobSourceUrlClickAppender.append(USER_ID, jobId)
        jobSourceUrlClickAppender.append(USER_ID, jobId)
        jobSourceUrlClickAppender.append(OTHER_USER_ID, jobId)
        jobSourceUrlClickAppender.append(USER_ID, otherJobId)

        assertEquals(3L, jobSourceUrlClickRepository.count())
        assertEquals(true, jobSourceUrlClickRepository.existsByJobIdAndUserId(jobId, USER_ID))
        assertEquals(false, jobSourceUrlClickRepository.existsByJobIdAndUserId(otherJobId, OTHER_USER_ID))
    }

    @Test
    fun `태그는 이름을 정리해 중복을 없애고 없는 태그만 새로 만든다`() {
        val jobId = checkNotNull(jobAppender.append(createCommand()).id)

        jobTagAppender.append(jobId, listOf(" 백엔드 ", "백엔드", "스프링  부트", ""))

        assertEquals(setOf("백엔드", "스프링 부트"), tagRepository.findAll().mapTo(mutableSetOf()) { it.name })
        assertEquals(2, jobTagRepository.findAllByJobId(jobId).size)
    }

    @Test
    fun `같은 태그를 다시 연결해도 태그와 연결이 늘지 않는다`() {
        val jobId = checkNotNull(jobAppender.append(createCommand()).id)
        jobTagAppender.append(jobId, listOf("백엔드"))

        jobTagAppender.append(jobId, listOf("백엔드"))

        assertEquals(1, tagRepository.count().toInt())
        assertEquals(1, jobTagRepository.findAllByJobId(jobId).size)
    }

    @Test
    fun `원본 URL 중복 확인은 삭제된 공고를 제외한다`() {
        val job = jobAppender.append(createCommand())
        val sourceUrl = checkNotNull(job.sourceUrl)

        assertEquals(true, jobReader.existsBySourceUrl(sourceUrl))

        jobManager.delete(job, NOW)

        assertEquals(false, jobReader.existsBySourceUrl(sourceUrl))
    }

    @Test
    fun `북마크 목록은 게시된 미삭제 공고만 반환한다`() {
        val published = jobAppender.append(createCommand())
        val draft = jobAppender.append(createCommand())
        jobManager.publish(published)
        jobBookmarkManager.append(USER_ID, checkNotNull(published.id))
        jobBookmarkManager.append(USER_ID, checkNotNull(draft.id))

        val result = jobBookmarkReader.readBookmarkedPublishedPage(USER_ID, page = 0, size = 10)

        assertEquals(listOf(published.id), result.jobs.map { it.id })
        assertEquals(1L, result.totalElements)
    }

    private fun createCommand(
        recruitmentType: JobRecruitmentType = JobRecruitmentType.PERIOD,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
        sourceUrl: String? = "https://example.com/jobs/1",
    ): JobAppendCommand = JobAppendCommand(
        companyName = "오공고",
        title = "백엔드 개발자",
        sourceUrl = sourceUrl,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        experienceMinYears = 1,
        experienceMaxYears = 3,
        educationLevel = EducationLevel.ANY,
        region = "서울",
        recruitmentType = recruitmentType,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        responsibilities = "주요 업무",
    )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_USER_ID = 18L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
    }
}
