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
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
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
    JobReader::class,
    JobQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    JobBookmarkReader::class,
    JobBookmarkManager::class,
    JobMetricReader::class,
    JobMetricManager::class,
    JobMetricRegistrar::class,
    JobTagAppender::class,
    TagRegistrar::class,
    JobSourceUrlClickAppender::class,
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

        val firstPage = readPage(page = 0, size = 1, sortType = JobSortType.LATEST)
        val secondPage = readPage(page = 1, size = 1, sortType = JobSortType.LATEST)

        assertEquals(listOf(latest.id), firstPage.jobs.map { it.id })
        assertEquals(listOf(first.id), secondPage.jobs.map { it.id })
        assertEquals(2L, firstPage.totalElements)
        assertEquals(true, firstPage.hasNext)
        assertEquals(false, secondPage.hasNext)
    }

    @Test
    fun `사용자 공고 목록의 페이지 범위를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { readPage(page = -1, size = 20) }
        assertThrows(IllegalArgumentException::class.java) { readPage(page = 0, size = 0) }
        assertThrows(IllegalArgumentException::class.java) { readPage(page = 0, size = 101) }
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

        jobReader.readPublished(jobId)
        jobBookmarkManager.append(USER_ID, jobId, NOW)
        jobMetricManager.syncBookmarkCount(jobId, NOW)

        assertEquals(setOf(jobId), jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(jobId)))
        assertEquals(1L, jobMetricRepository.findByJobId(jobId)?.bookmarkCount)

        jobBookmarkManager.delete(USER_ID, jobId, NOW.plusMinutes(1))
        jobBookmarkManager.delete(USER_ID, jobId, NOW.plusMinutes(2))
        jobMetricManager.syncBookmarkCount(jobId, NOW)

        assertEquals(emptySet<Long>(), jobBookmarkReader.readBookmarkedJobIds(USER_ID, listOf(jobId)))
        assertEquals(0L, jobMetricRepository.findByJobId(jobId)?.bookmarkCount)
        // 이미 해제된 북마크를 다시 해제해도 최초 해제 일시가 덮어써지지 않는다.
        assertEquals(NOW.plusMinutes(1), jobBookmarkRepository.findByJobIdAndUserId(jobId, USER_ID)?.deletedAt)

        jobBookmarkManager.append(USER_ID, jobId, NOW.plusMinutes(3))
        jobMetricManager.syncBookmarkCount(jobId, NOW)

        val restored = jobBookmarkRepository.findByJobIdAndUserId(jobId, USER_ID)
        assertEquals(1L, jobMetricRepository.findByJobId(jobId)?.bookmarkCount)
        assertEquals(null, restored?.deletedAt)
        // 북마크 목록이 수정 일시로 정렬하므로 복구는 벌크 갱신에서도 수정 일시를 남겨야 한다.
        assertEquals(NOW.plusMinutes(3), restored?.updatedAt)
        assertEquals(1L, jobBookmarkRepository.count())
    }

    @Test
    fun `이미 등록한 북마크를 다시 등록하면 유니크 제약이 막는다`() {
        val job = jobAppender.append(createCommand())
        val jobId = checkNotNull(job.id)
        jobManager.publish(job)
        jobBookmarkManager.append(USER_ID, jobId, NOW)

        val duplicate = assertThrows(ConflictException::class.java) {
            jobBookmarkManager.append(USER_ID, jobId, NOW.plusMinutes(1))
        }

        assertEquals(JobErrorCode.JOB_BOOKMARK_ALREADY_EXISTS, duplicate.errorCode)
    }

    @Test
    fun `조회 수는 지표 행이 없으면 만들고 있으면 증가시킨다`() {
        val job = jobAppender.append(createCommand())
        val jobId = checkNotNull(job.id)

        jobMetricManager.increaseViewCount(jobId, NOW)

        assertEquals(1L, jobMetricReader.read(jobId).viewCount)

        jobMetricManager.increaseViewCount(jobId, NOW.plusMinutes(1))

        assertEquals(2L, jobMetricReader.read(jobId).viewCount)
        assertEquals(1, jobMetricRepository.findAllByJobIdIn(listOf(jobId)).size)
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
        jobBookmarkManager.append(USER_ID, jobId, NOW)

        jobMetricManager.syncBookmarkCount(jobId, NOW)
        jobMetricManager.syncBookmarkCount(jobId, NOW)
        jobMetricManager.syncBookmarkCount(jobId, NOW)

        assertEquals(1L, jobMetricReader.read(jobId).bookmarkCount)
        assertEquals(1, jobMetricRepository.findAllByJobIdIn(listOf(jobId)).size)
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

        val page = readPage(page = 0, size = 10, sortType = JobSortType.VIEW_COUNT)

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

        val page = readPage(page = 0, size = 10, sortType = JobSortType.VIEW_COUNT)

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
        jobBookmarkManager.append(USER_ID, checkNotNull(published.id), NOW)
        jobBookmarkManager.append(USER_ID, checkNotNull(draft.id), NOW)

        val result = jobBookmarkReader.readBookmarkedPublishedPage(USER_ID, page = 0, size = 10)

        assertEquals(listOf(published.id), result.jobs.map { it.id })
        assertEquals(1L, result.totalElements)
    }

    @Test
    fun `선택 필터는 지정한 값만 남기고 지정하지 않으면 적용되지 않는다`() {
        val fullTimeExperienced = publish(EmploymentType.FULL_TIME, ExperienceType.EXPERIENCED)
        val fullTimeNewcomer = publish(EmploymentType.FULL_TIME, ExperienceType.NEWCOMER)
        val internExperienced = publish(EmploymentType.INTERN, ExperienceType.EXPERIENCED)

        assertEquals(
            listOf(internExperienced, fullTimeNewcomer, fullTimeExperienced),
            readIds(JobSearchCondition.NONE),
        )
        assertEquals(
            listOf(fullTimeNewcomer, fullTimeExperienced),
            readIds(JobSearchCondition(employmentType = EmploymentType.FULL_TIME)),
        )
        assertEquals(
            listOf(internExperienced, fullTimeExperienced),
            readIds(JobSearchCondition(experienceType = ExperienceType.EXPERIENCED)),
        )
    }

    @Test
    fun `두 필터를 함께 지정하면 모두 만족하는 공고만 남는다`() {
        val target = publish(EmploymentType.FULL_TIME, ExperienceType.EXPERIENCED)
        publish(EmploymentType.FULL_TIME, ExperienceType.NEWCOMER)
        publish(EmploymentType.INTERN, ExperienceType.EXPERIENCED)

        val page = readPage(
            page = 0,
            size = 10,
            condition = JobSearchCondition(
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.EXPERIENCED,
            ),
        )

        assertEquals(listOf(target), page.jobs.map { it.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `필터는 정렬과 함께 적용되며 전체 건수도 필터를 반영한다`() {
        val quiet = publish(EmploymentType.FULL_TIME, ExperienceType.EXPERIENCED)
        val popular = publish(EmploymentType.FULL_TIME, ExperienceType.EXPERIENCED)
        publish(EmploymentType.INTERN, ExperienceType.EXPERIENCED)
        jobMetricManager.increaseViewCount(popular, NOW)

        val page = readPage(
            page = 0,
            size = 10,
            sortType = JobSortType.VIEW_COUNT,
            condition = JobSearchCondition(employmentType = EmploymentType.FULL_TIME),
        )

        assertEquals(listOf(popular, quiet), page.jobs.map { it.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `검색어는 회사명과 제목을 모두 대상으로 하고 대소문자를 가리지 않는다`() {
        val byTitle = publishNamed(companyName = "오공고", title = "iOS 개발자")
        val byCompany = publishNamed(companyName = "IOS컴퍼니", title = "백엔드 개발자")
        publishNamed(companyName = "다른회사", title = "데이터 엔지니어")

        assertEquals(
            listOf(byCompany, byTitle),
            readIds(JobSearchCondition(keyword = "ios")),
        )
    }

    @Test
    fun `검색어가 비어 있으면 검색 조건을 적용하지 않는다`() {
        val first = publishNamed(companyName = "오공고", title = "백엔드 개발자")
        val second = publishNamed(companyName = "다른회사", title = "데이터 엔지니어")

        assertEquals(listOf(second, first), readIds(JobSearchCondition(keyword = "   ")))
        assertEquals(listOf(second, first), readIds(JobSearchCondition(keyword = null)))
    }

    @Test
    fun `검색어의 와일드카드는 문자 그대로 취급해 전체 조회를 유발하지 않는다`() {
        publishNamed(companyName = "오공고", title = "백엔드 개발자")
        val literal = publishNamed(companyName = "오공고", title = "연봉 100% 인상 백엔드")

        assertEquals(emptyList<Long>(), readIds(JobSearchCondition(keyword = "_")))
        assertEquals(listOf(literal), readIds(JobSearchCondition(keyword = "100%")))
    }

    @Test
    fun `검색어는 필터 정렬과 함께 적용된다`() {
        val target = publishNamed(
            companyName = "오공고",
            title = "백엔드 개발자",
            employmentType = EmploymentType.INTERN,
        )
        publishNamed(companyName = "오공고", title = "백엔드 개발자", employmentType = EmploymentType.FULL_TIME)
        publishNamed(companyName = "오공고", title = "데이터 엔지니어", employmentType = EmploymentType.INTERN)

        val page = readPage(
            page = 0,
            size = 10,
            sortType = JobSortType.VIEW_COUNT,
            condition = JobSearchCondition(employmentType = EmploymentType.INTERN, keyword = "백엔드"),
        )

        assertEquals(listOf(target), page.jobs.map { it.id })
        assertEquals(1L, page.totalElements)
    }

    private fun publishNamed(
        companyName: String,
        title: String,
        employmentType: EmploymentType = EmploymentType.FULL_TIME,
    ): Long {
        val job = jobAppender.append(
            createCommand(employmentType = employmentType, companyName = companyName, title = title),
        )
        jobManager.publish(job)
        return checkNotNull(job.id)
    }

    @Test
    fun `기업회원은 자기 공고만 조회하고 남의 공고는 찾지 못한다`() {
        val mine = jobAppender.append(createCommand(ownerUserId = USER_ID))
        val others = jobAppender.append(createCommand(ownerUserId = OTHER_USER_ID))
        val collected = jobAppender.append(createCommand())
        val mineId = checkNotNull(mine.id)

        val page = jobReader.readOwnedPage(USER_ID, page = 0, size = 10)

        assertEquals(listOf(mineId), page.jobs.map { it.id })
        assertEquals(1L, page.totalElements)
        assertEquals(mineId, jobReader.readOwned(USER_ID, mineId).id)
        assertThrows(EntityNotFoundException::class.java) {
            jobReader.readOwned(USER_ID, checkNotNull(others.id))
        }
        // 수집한 공고는 소유자가 없으므로 어떤 기업회원에게도 보이지 않는다.
        assertThrows(EntityNotFoundException::class.java) {
            jobReader.readOwned(USER_ID, checkNotNull(collected.id))
        }
    }

    @Test
    fun `삭제 조회만 이미 삭제된 내 공고를 찾아 삭제를 멱등하게 만든다`() {
        val job = jobAppender.append(createCommand(ownerUserId = USER_ID))
        val jobId = checkNotNull(job.id)
        jobManager.delete(jobReader.readOwnedForDelete(USER_ID, jobId), NOW)

        assertThrows(EntityNotFoundException::class.java) { jobReader.readOwned(USER_ID, jobId) }
        assertThrows(EntityNotFoundException::class.java) { jobReader.readOwnedForUpdate(USER_ID, jobId) }

        jobManager.delete(jobReader.readOwnedForDelete(USER_ID, jobId), NOW.plusDays(1))

        assertEquals(NOW, jobReader.readIncludingDeleted(jobId).deletedAt)
    }

    private fun publish(employmentType: EmploymentType, experienceType: ExperienceType): Long {
        val job = jobAppender.append(
            createCommand(employmentType = employmentType, experienceType = experienceType),
        )
        jobManager.publish(job)
        return checkNotNull(job.id)
    }

    private fun readIds(condition: JobSearchCondition): List<Long?> =
        readPage(page = 0, size = 10, condition = condition).jobs.map { it.id }

    private fun readPage(
        page: Int,
        size: Int,
        sortType: JobSortType = JobSortType.LATEST,
        condition: JobSearchCondition = JobSearchCondition.NONE,
    ): JobPage = jobReader.readPublishedPage(condition, sortType, page, size)

    private fun createCommand(
        recruitmentType: JobRecruitmentType = JobRecruitmentType.PERIOD,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
        sourceUrl: String? = "https://example.com/jobs/1",
        employmentType: EmploymentType = EmploymentType.FULL_TIME,
        experienceType: ExperienceType = ExperienceType.EXPERIENCED,
        companyName: String = "오공고",
        title: String = "백엔드 개발자",
        ownerUserId: Long? = null,
    ): JobAppendCommand = JobAppendCommand(
        ownerUserId = ownerUserId,
        companyName = companyName,
        title = title,
        sourceUrl = sourceUrl,
        employmentType = employmentType,
        experienceType = experienceType,
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
