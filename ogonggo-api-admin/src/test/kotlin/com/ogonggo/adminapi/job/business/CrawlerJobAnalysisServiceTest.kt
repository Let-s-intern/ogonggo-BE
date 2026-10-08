package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAnalysisManager
import com.ogonggo.core.job.implement.JobAnalysisReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobAnalysisCandidateDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.stubbing.Answer

class CrawlerJobAnalysisServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val candidateRounds = ArrayDeque<JobAnalysisCandidateDto>()
    private val jobAnalysisReader = Mockito.mock(JobAnalysisReader::class.java, Answer { candidateRounds.removeFirst() })

    private val confirmed = mutableListOf<Long?>()
    private var saved: Triple<Long?, Int?, String>? = null
    private val jobAnalysisManager = Mockito.mock(JobAnalysisManager::class.java, Answer { invocation ->
        when (invocation.method.name) {
            "confirm" -> {
                @Suppress("UNCHECKED_CAST")
                confirmed += (invocation.arguments[0] as Collection<Job>).map(Job::id)
            }
            "save" -> saved = Triple(
                (invocation.arguments[0] as Job).id,
                invocation.arguments[2] as Int?,
                invocation.arguments[3] as String,
            )
        }
        null
    })

    private val service = CrawlerJobAnalysisService(jobReader, jobAnalysisReader, jobAnalysisManager)

    @Test
    fun `본문이 그대로인 후보는 확인으로 남기고 대상이 찰 때까지 후보를 더 읽는다`() {
        // given
        candidateRounds += JobAnalysisCandidateDto(targets = listOf(job(1)), unchanged = listOf(job(2), job(3)))
        candidateRounds += JobAnalysisCandidateDto(targets = listOf(job(4)), unchanged = emptyList())

        // when
        val targets = service.getTargets(size = 3)

        // then
        assertEquals(listOf(1L, 4L), targets.map(Job::id))
        assertEquals(listOf(2L, 3L), confirmed)
    }

    @Test
    fun `대상이 요청한 개수만큼 모이면 더 읽지 않는다`() {
        candidateRounds += JobAnalysisCandidateDto(targets = listOf(job(1), job(2)), unchanged = listOf(job(3)))

        assertEquals(listOf(1L, 2L), service.getTargets(size = 2).map(Job::id))
        assertEquals(0, candidateRounds.size)
    }

    @Test
    fun `분석한 본문이 지금 본문과 같으면 저장한다`() {
        val job = job(7, hash = "now")
        Mockito.`when`(jobReader.read(7L)).thenReturn(job)

        service.saveAnalysis(7L, command(contentHash = "now"))

        assertEquals(Triple(7L, 3, "deepseek"), saved)
    }

    @Test
    fun `분석한 뒤로 본문이 바뀌었으면 저장하지 않고 충돌로 알린다`() {
        val job = job(7, hash = "now")
        Mockito.`when`(jobReader.read(7L)).thenReturn(job)

        val exception = assertThrows<ConflictException> { service.saveAnalysis(7L, command(contentHash = "old")) }

        assertEquals(JobErrorCode.JOB_ANALYSIS_OUTDATED, exception.errorCode)
        assertEquals(null, saved)
    }

    private fun job(id: Long, hash: String = "h$id"): Job = Mockito.mock(Job::class.java).also {
        Mockito.`when`(it.id).thenReturn(id)
        Mockito.`when`(it.contentHash()).thenReturn(hash)
    }

    private fun command(contentHash: String): CrawlerJobAnalysisCommand {
        val empty = JobAnalysisContent.Fact(null, null)
        return CrawlerJobAnalysisCommand(
            contentHash = contentHash,
            guideVersion = 3,
            model = "deepseek",
            content = JobAnalysisContent(
                tasks = emptyList(),
                required = emptyList(),
                preferred = emptyList(),
                employment = JobAnalysisContent.Employment(empty, empty, empty, empty),
                submission = JobAnalysisContent.Submission(empty, empty, empty, empty),
                competencies = emptyList(),
            ),
        )
    }
}
