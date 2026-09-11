package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobTagAppender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.stubbing.Answer

class CrawlerJobServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val savedJob = Mockito.mock(Job::class.java).also { Mockito.`when`(it.id).thenReturn(JOB_ID) }

    /**
     * 코틀린에서는 `any()`와 `capture()`가 null을 돌려줘 non-null 파라미터에 넘길 수 없다.
     * 인자 매처 대신 호출을 그대로 받아 기록한다.
     */
    private var appendedCommand: JobAppendDto? = null
    private val jobAppender = Mockito.mock(JobAppender::class.java, Answer { invocation ->
        appendedCommand = invocation.arguments[0] as JobAppendDto
        savedJob
    })

    private val appendedTags = mutableListOf<Pair<Long, List<String>>>()
    private val jobTagAppender = Mockito.mock(JobTagAppender::class.java, Answer { invocation ->
        @Suppress("UNCHECKED_CAST")
        appendedTags += invocation.arguments[0] as Long to (invocation.arguments[1] as Collection<String>).toList()
        null
    })
    private val service = CrawlerJobService(jobReader, jobAppender, jobTagAppender)

    @Test
    fun `수집한 공고를 게시 상태로 저장하고 태그를 연결한다`() {
        Mockito.`when`(jobReader.existsBySourceUrl(SOURCE_URL)).thenReturn(false)

        val jobId = service.register(command())

        assertEquals(JOB_ID, jobId)
        val appended = checkNotNull(appendedCommand)
        assertEquals(JobPublicationStatus.PUBLISHED, appended.publicationStatus)
        assertEquals("오공고", appended.companyName)
        assertEquals("렛츠커리어", appended.parentCompanyName)
        assertEquals(JobRecruitmentType.ALWAYS_OPEN, appended.recruitmentType)
        assertEquals("개발", appended.jobField)
        assertEquals("백엔드 개발자", appended.jobRole)
        assertEquals("IT", appended.industry)
        assertEquals(listOf(JOB_ID to listOf("백엔드", "스프링")), appendedTags)
    }

    @Test
    fun `이미 등록된 원문은 저장하지 않고 충돌로 알린다`() {
        Mockito.`when`(jobReader.existsBySourceUrl(SOURCE_URL)).thenReturn(true)

        val exception = assertThrows<ConflictException> { service.register(command()) }

        assertEquals(JobErrorCode.JOB_ALREADY_EXISTS, exception.errorCode)
        assertNull(appendedCommand)
        assertEquals(emptyList<Pair<Long, List<String>>>(), appendedTags)
    }

    private fun command(): CrawlerJobRegistrationCommand = CrawlerJobRegistrationCommand(
        companyName = "오공고",
        parentCompanyName = "렛츠커리어",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.IRRELEVANT,
        experienceMinYears = null,
        experienceMaxYears = null,
        educationLevel = EducationLevel.ANY,
        region = null,
        jobField = "개발",
        jobRole = "백엔드 개발자",
        industry = "IT",
        recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
        recruitmentStartAt = null,
        recruitmentEndAt = null,
        companyAndTeamIntroduction = null,
        responsibilities = null,
        qualifications = null,
        preferredQualifications = null,
        compensation = null,
        benefits = null,
        hiringProcess = null,
        sourceUrl = SOURCE_URL,
        tags = listOf("백엔드", "스프링"),
        publicationStatus = JobPublicationStatus.PUBLISHED,
    )


    companion object {
        private const val JOB_ID = 7L
        private const val SOURCE_URL = "https://example.com/jobs/1"
    }
}
