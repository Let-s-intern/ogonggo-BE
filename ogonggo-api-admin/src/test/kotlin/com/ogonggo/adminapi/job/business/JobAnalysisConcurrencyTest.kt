package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAnalysisReader
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 운영자 분석 수정과 AI 서버 분석 저장이 같은 공고에 동시에 들어와도 운영자 수정이 남아야 한다.
 * 두 쓰기가 공고 행을 잠가 차례로 돈다. 잠그지 않으면 분석이 없던 공고에 두 INSERT가 겹쳐 job_id 유일 제약으로 500이 나거나,
 * AI 저장이 운영자 수정 표시를 지운다.
 */
@Tag("concurrency")
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:job-analysis-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class JobAnalysisConcurrencyTest @Autowired constructor(
    private val adminJobService: AdminJobService,
    private val aiJobAnalysisService: AiJobAnalysisService,
    private val jobAppender: JobAppender,
    private val jobReader: JobReader,
    private val jobAnalysisReader: JobAnalysisReader,
) {

    @Test
    fun `운영자 분석 수정과 AI 분석 저장이 겹쳐도 500 없이 운영자 수정이 남는다`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(ROUNDS) {
                // given
                val jobId = checkNotNull(jobAppender.append(command()).id)
                val contentHash = jobReader.read(jobId).contentHash()
                val start = CountDownLatch(1)

                // when
                val admin = executor.submit<Result<Unit>> {
                    start.await(5, TimeUnit.SECONDS)
                    runCatching { adminJobService.replaceAnalysis(jobId, ADMIN_CONTENT, adminUserId = 1L) }
                }
                val ai = executor.submit<Result<Unit>> {
                    start.await(5, TimeUnit.SECONDS)
                    runCatching {
                        aiJobAnalysisService.saveAnalysis(
                            jobId,
                            AiJobAnalysisCommand(contentHash = contentHash, guideVersion = 1, model = "deepseek", content = AI_CONTENT),
                        )
                    }
                }
                start.countDown()

                // then
                admin.get(10, TimeUnit.SECONDS).getOrThrow()
                ai.get(10, TimeUnit.SECONDS).exceptionOrNull()?.let { error ->
                    assertEquals(JobErrorCode.JOB_ANALYSIS_EDITED_BY_ADMIN, (error as ConflictException).errorCode)
                }
                val saved = checkNotNull(jobAnalysisReader.readForAdmin(jobReader.read(jobId)))
                assertEquals(ADMIN_CONTENT, saved.content)
                assertNotNull(saved.editedAt)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun command() = JobAppendDto(
        companyName = "오공고",
        title = "백엔드 개발자",
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.NEWCOMER,
        recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
        responsibilities = "서버 개발",
        publicationStatus = JobPublicationStatus.PUBLISHED,
    )

    private companion object {
        const val ROUNDS = 20

        private fun content(required: String) = JobAnalysisContent(
            tasks = emptyList(),
            required = listOf(required),
            preferred = emptyList(),
            employment = JobAnalysisContent.Employment(fact(), fact(), fact(), fact()),
            submission = JobAnalysisContent.Submission(fact(), fact(), fact(), fact()),
            competencies = emptyList(),
        )

        private fun fact() = JobAnalysisContent.Fact(value = null, note = null)

        val ADMIN_CONTENT = content("운영자가 고친 조건")
        val AI_CONTENT = content("AI가 만든 조건")
    }
}
