package com.ogonggo.core.job.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractManager
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class, JobSourceCheckPersistenceTest.ObjectMapperConfig::class])
@Import(
    JobReader::class,
    JobQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    AdvertisementContractManager::class,
    ContentRejectionManager::class,
)
internal class JobSourceCheckPersistenceTest @Autowired constructor(
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobReader: JobReader,
    private val entityManager: EntityManager,
) {

    class ObjectMapperConfig {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()
    }

    @Test
    fun `게시 중인 모집 중 상시 채용 공고 가운데 원문이 있는 것을 등록 경로와 관계없이 식별자 순으로 준다`() {
        // given
        val crawled = job(source = ContentSource.CRAWLER)
        val work24 = job(source = ContentSource.WORK24)
        job(recruitmentType = JobRecruitmentType.PERIOD)
        job(sourceUrl = null)
        job(publicationStatus = JobPublicationStatus.HIDDEN)
        jobManager.close(job(), NOW)
        jobManager.delete(job(), NOW)
        entityManager.flush()

        // when
        val targets = jobReader.readSourceCheckTargets(afterId = 0, size = 10)

        // then
        assertEquals(listOf(crawled.id, work24.id), targets.map(Job::id))
    }

    @Test
    fun `afterId 다음부터 size건만 준다`() {
        // given
        val first = job()
        val second = job()
        val third = job()
        entityManager.flush()

        // when
        val page = jobReader.readSourceCheckTargets(afterId = checkNotNull(first.id), size = 1)

        // then
        assertEquals(listOf(second.id), page.map(Job::id))
        assertEquals(listOf(third.id), jobReader.readSourceCheckTargets(checkNotNull(second.id), 10).map(Job::id))
    }

    private fun job(
        source: ContentSource = ContentSource.CRAWLER,
        recruitmentType: JobRecruitmentType = JobRecruitmentType.ALWAYS_OPEN,
        sourceUrl: String? = "https://example.com/jobs/${counter++}",
        publicationStatus: JobPublicationStatus = JobPublicationStatus.PUBLISHED,
    ): Job = jobAppender.append(
        JobAppendDto(
            companyName = "오공고",
            title = "백엔드 개발자",
            employmentType = JobEmploymentType.FULL_TIME,
            experienceType = JobExperienceType.EXPERIENCED,
            recruitmentType = recruitmentType,
            recruitmentEndAt = if (recruitmentType == JobRecruitmentType.PERIOD) NOW.plusYears(10) else null,
            responsibilities = "서버 개발",
            sourceUrl = sourceUrl,
            source = source,
            publicationStatus = publicationStatus,
        ),
    )

    companion object {
        private val NOW = LocalDateTime.of(2026, 11, 15, 12, 0)
        private var counter = 0
    }
}
