package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceManager
import com.ogonggo.core.job.implement.JobTagAppender
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.stubbing.Answer

class AiJobServiceTest {

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

    private var updatedCommand: JobUpdateDto? = null
    private val managerCalls = mutableListOf<String>()
    private val jobManager = Mockito.mock(JobManager::class.java, Answer { invocation ->
        managerCalls += invocation.method.name
        when (invocation.method.name) {
            "update" -> {
                updatedCommand = invocation.arguments[1] as JobUpdateDto
                null
            }
            else -> null
        }
    })

    private val savedSources = mutableListOf<Pair<Long, String>>()
    private val jobSourceManager = Mockito.mock(JobSourceManager::class.java, Answer { invocation ->
        savedSources += invocation.arguments[0] as Long to invocation.arguments[1] as String
        null
    })

    private val clock = Clock.fixed(Instant.parse("2026-09-14T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AiJobService(jobReader, jobAppender, jobManager, jobTagAppender, jobSourceManager, clock)

    @Test
    fun `수집한 공고를 검수 없이 게시 상태로 저장하고 보낸 값을 그대로 옮긴다`() {
        Mockito.`when`(jobReader.existsBySourceUrl(SOURCE_URL)).thenReturn(false)

        val jobId = service.register(AiJobRegistrationCommand(job = command(), tags = listOf("백엔드", "스프링")))

        assertEquals(JOB_ID, jobId)
        val appended = checkNotNull(appendedCommand)
        assertEquals(JobPublicationStatus.PUBLISHED, appended.publicationStatus)
        assertEquals("오공고", appended.companyName)
        assertEquals("렛츠커리어", appended.parentCompanyName)
        assertEquals(JobField.IT_DEVELOPMENT, appended.jobField)
        assertEquals(JobRole.IT_BACKEND, appended.jobRole)
        assertEquals("IT·정보통신업", appended.industry)
        assertEquals("https://example.com/cover.png", appended.coverImageUrl)
        assertEquals("https://example.com/logo.png", appended.logoUrl)
        // AI 서버가 고른 판단 값을 서버가 다른 값으로 바꾸지 않는다.
        assertEquals(JobExperienceType.BOTH, appended.experienceType)
        assertEquals(0, appended.experienceMinYears)
        assertEquals(JobEducationLevel.BACHELOR, appended.educationLevel)
        assertEquals(JobRecruitmentType.PERIOD, appended.recruitmentType)
        assertEquals(3, appended.recruitmentHeadcount)
        assertEquals(true, appended.closesWhenFilled)
        assertEquals(true, appended.autoCloseEnabled)
        assertEquals("제출 서류는 반환하지 않습니다.", appended.recruitmentNotice)
        assertEquals(JobApplicationMethod.EXTERNAL_PAGE, appended.applicationMethod)
        assertEquals("recruit@example.com", appended.applyEmail)
        assertEquals("hr@example.com", appended.inquiryEmail)
        assertEquals(listOf(JOB_ID to listOf("백엔드", "스프링")), appendedTags)
        assertEquals(listOf(JOB_ID to "<h2>주요 업무</h2><p>백엔드 개발</p>"), savedSources)
    }

    @Test
    fun `이미 등록된 원문은 저장하지 않고 충돌로 알린다`() {
        Mockito.`when`(jobReader.existsBySourceUrl(SOURCE_URL)).thenReturn(true)

        val exception = assertThrows<ConflictException> {
            service.register(AiJobRegistrationCommand(job = command(), tags = listOf("백엔드")))
        }

        assertEquals(JobErrorCode.JOB_ALREADY_EXISTS, exception.errorCode)
        assertNull(appendedCommand)
        assertEquals(emptyList<Pair<Long, List<String>>>(), appendedTags)
    }

    @Test
    fun `교체는 값만 바꾸고 검수나 게시 상태는 건드리지 않는다`() {
        stubCrawledJob()

        service.replace(JOB_ID, command(title = "바뀐 제목", sourceHtml = null))

        assertEquals("바뀐 제목", updatedCommand?.title)
        assertEquals(listOf("update"), managerCalls)
        assertEquals(emptyList<Pair<Long, String>>(), savedSources)
    }

    @Test
    fun `교체에 원문을 함께 보내면 원문도 바꾼다`() {
        stubCrawledJob()

        service.replace(JOB_ID, command())

        assertEquals(listOf("update"), managerCalls)
        assertEquals(listOf(JOB_ID to "<h2>주요 업무</h2><p>백엔드 개발</p>"), savedSources)
    }

    @Test
    fun `원문만 바꾼다`() {
        stubCrawledJob()

        service.changeSourceHtml(JOB_ID, "<p onclick=\"steal()\">상대 <a href=\"/apply\">지원</a></p><script>x()</script>")

        assertEquals(
            listOf(JOB_ID to """<p>상대 <a href="https://example.com/apply" rel="nofollow noopener noreferrer" target="_blank">지원</a></p>"""),
            savedSources,
        )
        assertEquals(emptyList<String>(), managerCalls)
    }

    @Test
    fun `걸러 낸 뒤 글도 이미지도 없는 원문은 저장하지 않는다`() {
        stubCrawledJob()

        service.changeSourceHtml(JOB_ID, "<div style=\"color:red\"> </div><script>alert(1)</script>")

        assertEquals(emptyList<Pair<Long, String>>(), savedSources)
    }

    @Test
    fun `다른 공고가 쓰는 원문 URL로 바꾸려 하면 충돌로 알린다`() {
        stubCrawledJob()
        val otherUrl = "https://example.com/jobs/2"
        Mockito.`when`(jobReader.existsBySourceUrl(otherUrl)).thenReturn(true)

        val exception = assertThrows<ConflictException> { service.replace(JOB_ID, command(sourceUrl = otherUrl)) }

        assertEquals(JobErrorCode.JOB_ALREADY_EXISTS, exception.errorCode)
        assertEquals(emptyList<String>(), managerCalls)
    }

    @Test
    fun `수집 공고를 삭제한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readCrawledForDelete(JOB_ID)).thenReturn(job)

        service.delete(JOB_ID)

        assertEquals(listOf("delete"), managerCalls)
    }

    @Test
    fun `수집 공고를 숨긴다`() {
        Mockito.`when`(jobReader.readCollectedForUpdate(JOB_ID)).thenReturn(Mockito.mock(Job::class.java))

        service.hide(JOB_ID)

        assertEquals(listOf("hide"), managerCalls)
    }

    @Test
    fun `원문 URL로 수집 공고의 식별자를 찾는다`() {
        Mockito.`when`(jobReader.readCrawledBySourceUrl(SOURCE_URL)).thenReturn(savedJob)

        assertEquals(JOB_ID, service.getJobId(SOURCE_URL))
    }

    private fun stubCrawledJob() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.sourceUrl).thenReturn(SOURCE_URL)
        Mockito.`when`(jobReader.readCrawledForUpdate(JOB_ID)).thenReturn(job)
    }

    private fun command(
        title: String = "백엔드 개발자",
        sourceUrl: String = SOURCE_URL,
        sourceHtml: String? = SOURCE_HTML,
    ): AiJobCommand = AiJobCommand(
        companyName = "오공고",
        parentCompanyName = "렛츠커리어",
        title = title,
        jobField = JobField.IT_DEVELOPMENT,
        jobRole = JobRole.IT_BACKEND,
        industry = "IT·정보통신업",
        coverImageUrl = "https://example.com/cover.png",
        logoUrl = "https://example.com/logo.png",
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.BOTH,
        experienceMinYears = 0,
        educationLevel = JobEducationLevel.BACHELOR,
        region = Region.SEOUL,
        subRegion = SubRegion.SEOUL_GANGNAM_GU,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentHeadcount = 3,
        recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59, 59),
        closesWhenFilled = true,
        autoCloseEnabled = true,
        companyAndTeamIntroduction = null,
        responsibilities = "주요 업무",
        qualifications = null,
        preferredQualifications = null,
        compensation = null,
        benefits = null,
        hiringProcess = null,
        recruitmentNotice = "제출 서류는 반환하지 않습니다.",
        applicationMethod = JobApplicationMethod.EXTERNAL_PAGE,
        applyEmail = "recruit@example.com",
        inquiryEmail = "hr@example.com",
        sourceUrl = sourceUrl,
        sourceHtml = sourceHtml,
    )

    companion object {
        private const val JOB_ID = 7L
        private const val SOURCE_URL = "https://example.com/jobs/1"
        private const val SOURCE_HTML = """<h2 style="color:red">주요 업무</h2><p class="x">백엔드 개발</p><script>alert(1)</script>"""
    }
}
