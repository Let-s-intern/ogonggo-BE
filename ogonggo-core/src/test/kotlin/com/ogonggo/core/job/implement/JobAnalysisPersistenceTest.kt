package com.ogonggo.core.job.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobAnalysisJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class, JobAnalysisPersistenceTest.ObjectMapperConfig::class])
@Import(
    JobReader::class,
    JobQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    JobAnalysisReader::class,
    JobAnalysisManager::class,
    ContentRejectionManager::class,
)
internal class JobAnalysisPersistenceTest @Autowired constructor(
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobAnalysisReader: JobAnalysisReader,
    private val jobAnalysisManager: JobAnalysisManager,
    private val jobAnalysisRepository: JobAnalysisJpaRepository,
    private val entityManager: EntityManager,
) {

    class ObjectMapperConfig {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()
    }

    @Test
    fun `저장한 분석을 지금 본문에 대해 읽고 본문이 바뀌면 보여 주지 않는다`() {
        // given
        val job = publishedJob()
        jobAnalysisManager.save(job, CONTENT, guideVersion = 3, model = "deepseek")
        flush()

        // when
        val current = jobAnalysisReader.readCurrent(job)
        job.editContent(title = null, contents = mapOf(JobContentField.RESPONSIBILITIES to "다른 업무"))

        // then
        assertEquals(CONTENT, current)
        assertNull(jobAnalysisReader.readCurrent(job))
    }

    @Test
    fun `다시 저장하면 같은 행을 바꾼다`() {
        val job = publishedJob()
        jobAnalysisManager.save(job, CONTENT, guideVersion = 1, model = "a")
        flush()

        jobAnalysisManager.save(job, CONTENT.copy(required = listOf("새 조건")), guideVersion = 2, model = "b")
        flush()

        val rows = jobAnalysisRepository.findAll()
        assertEquals(1, rows.size)
        assertEquals(2, rows.single().guideVersion)
        assertEquals(listOf("새 조건"), jobAnalysisReader.readCurrent(job)?.required)
    }

    @Test
    fun `분석이 없는 게시 중 모집 공고만 대상이고 숨기거나 마감하거나 지운 공고는 뺀다`() {
        // given
        val target = publishedJob()
        val hidden = publishedJob().also(jobManager::hide)
        val closed = publishedJob().also { jobManager.close(it, NOW) }
        val deleted = publishedJob().also { jobManager.delete(it, NOW) }
        val analyzed = publishedJob().also { jobAnalysisManager.save(it, CONTENT, 1, "m") }
        flush()

        // when
        val candidates = jobAnalysisReader.readCandidates(limit = 10)

        // then
        assertEquals(listOf(target.id), candidates.targets.map(Job::id))
        assertEquals(emptyList<Job>(), candidates.unchanged)
        listOf(hidden, closed, deleted, analyzed).forEach { job ->
            assert(job.id !in candidates.targets.map(Job::id))
        }
    }

    @Test
    fun `분석 뒤 본문이 바뀐 공고는 대상이고 본문이 그대로면 확인한 뒤 다시 읽지 않는다`() {
        // given
        val changed = publishedJob()
        val same = publishedJob()
        listOf(changed, same).forEach { jobAnalysisManager.save(it, CONTENT, 1, "m") }
        flush()
        changed.editContent(title = "바뀐 제목", contents = emptyMap())
        // 본문이 아닌 칸이 바뀐 공고를 흉내 낸다: 분석 뒤로 공고 수정 일시가 달라졌다
        jobAnalysisRepository.findByJobId(checkNotNull(same.id))!!.confirm(LocalDateTime.of(2000, 1, 1, 0, 0))
        flush()

        // when
        val first = jobAnalysisReader.readCandidates(limit = 10)
        jobAnalysisManager.confirm(first.unchanged)
        flush()
        val second = jobAnalysisReader.readCandidates(limit = 10)

        // then
        assertEquals(listOf(changed.id), first.targets.map(Job::id))
        assertEquals(listOf(same.id), first.unchanged.map(Job::id))
        assertEquals(listOf(changed.id), second.targets.map(Job::id))
        assertEquals(emptyList<Job>(), second.unchanged)
    }

    @Test
    fun `대상은 최근 공고부터 요청한 개수까지 읽는다`() {
        val older = publishedJob()
        val newer = publishedJob()
        flush()

        assertEquals(listOf(newer.id), jobAnalysisReader.readCandidates(limit = 1).targets.map(Job::id))
        assertEquals(listOf(newer.id, older.id), jobAnalysisReader.readCandidates(limit = 5).targets.map(Job::id))
    }

    private fun publishedJob(): Job = jobAppender.append(
        JobAppendDto(
            companyName = "오공고",
            title = "백엔드 개발자",
            employmentType = JobEmploymentType.FULL_TIME,
            experienceType = JobExperienceType.EXPERIENCED,
            recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
            responsibilities = "서버 개발",
            publicationStatus = JobPublicationStatus.PUBLISHED,
        ),
    )

    private fun flush() {
        entityManager.flush()
    }

    companion object {
        private val NOW = LocalDateTime.of(2026, 10, 8, 12, 0)
        private val NOT_STATED = JobAnalysisContent.Fact(value = null, note = null)
        private val CONTENT = JobAnalysisContent(
            tasks = listOf(JobAnalysisContent.Task(tag = "개발", text = "서버를 개발해요.")),
            required = listOf("Kotlin 경험이 있는 분"),
            preferred = emptyList(),
            employment = JobAnalysisContent.Employment(
                type = JobAnalysisContent.Fact(value = "정규직", note = null),
                conversion = NOT_STATED,
                salary = NOT_STATED,
                affiliation = NOT_STATED,
            ),
            submission = JobAnalysisContent.Submission(
                documents = JobAnalysisContent.Fact(value = "이력서", note = "PDF 권장"),
                essay = NOT_STATED,
                process = NOT_STATED,
                deadline = NOT_STATED,
            ),
            competencies = listOf(
                JobAnalysisContent.Competency(
                    name = "문제 해결",
                    quote = "서버 개발",
                    description = "막힌 문제를 끝까지 푸는 역량이에요.",
                    experiences = listOf("버그를 끝까지 추적한 경험"),
                ),
            ),
        )
    }
}
