package com.ogonggo.core.job.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.TodayJobDto
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    JobReader::class,
    JobQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    TodayJobManager::class,
    ContentRejectionManager::class,
)
internal class TodayJobPersistenceTest @Autowired constructor(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val todayJobManager: TodayJobManager,
) {

    @Test
    fun `오늘의 공고는 등록 순서가 아니라 운영자가 고른 순서대로 읽는다`() {
        val first = publishedJob()
        val second = publishedJob()
        val third = publishedJob()

        todayJobManager.replace(todayJobs(third, first, second), NOW)

        assertEquals(listOf(third.id, first.id, second.id), jobReader.readPublishedToday().map { it.job.id })
    }

    @Test
    fun `공고마다 설정한 추천 문구를 함께 읽는다`() {
        val first = publishedJob()
        val second = publishedJob()

        todayJobManager.replace(
            listOf(
                TodayJobDto.Request(first.requiredId(), "경력 없이 시작하고 싶다면", "실무 중심 프로젝트로 빠른 성장"),
                TodayJobDto.Request(second.requiredId(), "연봉이 중요하다면", "업계 상위 보상"),
            ),
            NOW,
        )

        assertEquals(
            listOf(
                Triple(first.id, "경력 없이 시작하고 싶다면", "실무 중심 프로젝트로 빠른 성장"),
                Triple(second.id, "연봉이 중요하다면", "업계 상위 보상"),
            ),
            jobReader.readPublishedToday().map { Triple(it.job.id, it.recommendationTitle, it.recommendationDescription) },
        )
    }

    @Test
    fun `사용자에게는 게시 중인 공고만 보이고 관리 조회는 숨긴 공고도 보이며 삭제된 공고는 둘 다 뺀다`() {
        val published = publishedJob()
        val hidden = publishedJob()
        val deleted = publishedJob()
        val closed = publishedJob()
        todayJobManager.replace(todayJobs(published, hidden, deleted, closed), NOW)

        jobManager.hide(hidden)
        jobManager.delete(deleted, NOW)
        jobManager.close(closed, NOW)

        assertEquals(listOf(published.id, closed.id), jobReader.readPublishedToday().map { it.job.id })
        assertEquals(listOf(published.id, hidden.id, closed.id), jobReader.readToday().map { it.job.id })
    }

    @Test
    fun `다시 설정하면 이전 목록을 모두 빼고 새 목록으로 바꾸며 뺐던 공고도 다시 넣을 수 있다`() {
        val first = publishedJob()
        val second = publishedJob()
        val third = publishedJob()
        todayJobManager.replace(todayJobs(first, second), NOW)

        todayJobManager.replace(todayJobs(third, first), NOW.plusHours(1))
        assertEquals(listOf(third.id, first.id), jobReader.readPublishedToday().map { it.job.id })

        todayJobManager.replace(todayJobs(second), NOW.plusHours(2))
        assertEquals(listOf(second.id), jobReader.readPublishedToday().map { it.job.id })
    }

    @Test
    fun `빈 목록으로 설정하면 오늘의 공고를 비운다`() {
        todayJobManager.replace(todayJobs(publishedJob()), NOW)

        todayJobManager.replace(emptyList(), NOW.plusHours(1))

        assertEquals(emptyList<Long?>(), jobReader.readToday().map { it.job.id })
    }

    @Test
    fun `없거나 삭제된 공고가 섞여 있으면 거절하고 기존 목록을 그대로 둔다`() {
        val current = publishedJob()
        val deleted = publishedJob()
        jobManager.delete(deleted, NOW)
        todayJobManager.replace(todayJobs(current), NOW)

        listOf(deleted.requiredId(), 999_999L).forEach { invalidJobId ->
            val exception = assertThrows(EntityNotFoundException::class.java) {
                todayJobManager.replace(
                    todayJobs(publishedJob()) + todayJobRequest(invalidJobId),
                    NOW.plusHours(1),
                )
            }
            assertEquals(JobErrorCode.JOB_NOT_FOUND, exception.errorCode)
        }

        assertEquals(listOf(current.id), jobReader.readToday().map { it.job.id })
    }

    private fun publishedJob(): Job = jobAppender.append(
        JobAppendDto(
            companyName = "오공고",
            title = "백엔드 개발자",
            employmentType = JobEmploymentType.FULL_TIME,
            experienceType = JobExperienceType.EXPERIENCED,
            recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
            publicationStatus = JobPublicationStatus.PUBLISHED,
        ),
    )

    private fun Job.requiredId(): Long = checkNotNull(id)

    private fun todayJobs(vararg jobs: Job): List<TodayJobDto.Request> = jobs.map { todayJobRequest(it.requiredId()) }

    private fun todayJobRequest(jobId: Long): TodayJobDto.Request =
        TodayJobDto.Request(jobId = jobId, recommendationTitle = "추천 제목", recommendationDescription = "추천 설명")

    companion object {
        private val NOW = LocalDateTime.of(2026, 9, 30, 12, 0)
    }
}
