package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobTagAppender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class CrawlerJobServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobAppender = RecordingJobAppender()
    private val jobTagAppender = RecordingJobTagAppender()
    private val service = CrawlerJobService(jobReader, jobAppender, jobTagAppender)

    @Test
    fun `수집한 공고를 게시 상태로 저장하고 태그를 연결한다`() {
        Mockito.`when`(jobReader.existsBySourceUrl(SOURCE_URL)).thenReturn(false)

        val jobId = service.register(command())

        assertEquals(JOB_ID, jobId)
        val appended = checkNotNull(jobAppender.lastCommand)
        assertEquals(JobPublicationStatus.PUBLISHED, appended.publicationStatus)
        assertEquals("오공고", appended.companyName)
        assertEquals("렛츠커리어", appended.parentCompanyName)
        assertEquals(JobRecruitmentType.ALWAYS_OPEN, appended.recruitmentType)
        assertEquals(listOf(JOB_ID to listOf("백엔드", "스프링")), jobTagAppender.calls)
    }

    @Test
    fun `이미 등록된 원문은 저장하지 않고 충돌로 알린다`() {
        Mockito.`when`(jobReader.existsBySourceUrl(SOURCE_URL)).thenReturn(true)

        val exception = assertThrows<ConflictException> { service.register(command()) }

        assertEquals(JobErrorCode.JOB_ALREADY_EXISTS, exception.errorCode)
        assertNull(jobAppender.lastCommand)
        assertEquals(emptyList<Pair<Long, List<String>>>(), jobTagAppender.calls)
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

    private class RecordingJobAppender : JobAppender {
        var lastCommand: JobAppendCommand? = null

        override fun append(command: JobAppendCommand): Job {
            lastCommand = command
            return Mockito.mock(Job::class.java).also { Mockito.`when`(it.id).thenReturn(JOB_ID) }
        }
    }

    private class RecordingJobTagAppender : JobTagAppender {
        val calls = mutableListOf<Pair<Long, List<String>>>()

        override fun append(jobId: Long, tagNames: Collection<String>) {
            calls += jobId to tagNames.toList()
        }
    }

    companion object {
        private const val JOB_ID = 7L
        private const val SOURCE_URL = "https://example.com/jobs/1"
    }
}
