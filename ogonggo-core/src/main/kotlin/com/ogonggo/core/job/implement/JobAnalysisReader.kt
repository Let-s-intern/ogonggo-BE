package com.ogonggo.core.job.implement

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.implement.dto.JobAnalysisCandidateDto
import com.ogonggo.core.job.persistence.JobAnalysisJpaRepository
import com.ogonggo.core.job.persistence.JobJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class JobAnalysisReader internal constructor(
    private val jobRepository: JobJpaRepository,
    private val jobAnalysisRepository: JobAnalysisJpaRepository,
    private val objectMapper: ObjectMapper,
) {

    /**
     * 지금 본문에 대한 분석이다. 분석이 없거나 그 뒤로 본문이 바뀌었으면 `null`이다.
     * 운영자가 본문을 고친 뒤 다시 분석하기 전까지 예전 분석을 보여 주지 않기 위해서다.
     * 저장된 내용을 읽지 못해도 상세 조회를 깨지 않도록 `null`로 둔다.
     */
    fun readCurrent(job: Job): JobAnalysisContent? {
        val jobId = job.id ?: return null
        val analysis = jobAnalysisRepository.findByJobId(jobId) ?: return null
        if (analysis.contentHash != job.contentHash()) {
            return null
        }
        return runCatching { JobAnalysisContentJson.read(objectMapper.readTree(analysis.content)) }
            .onFailure { log.warn("공고 분석을 읽지 못했습니다. jobId={}", jobId, it) }
            .getOrNull()
    }

    /**
     * 분석을 다시 볼 후보를 최근 공고부터 `limit`건 읽고, 분석이 없거나 본문이 바뀐 것과 본문이 그대로인 것으로 나눈다.
     * 본문이 그대로인 공고는 확인한 것으로 남겨야 다음에 후보로 다시 읽히지 않는다([JobAnalysisManager.confirm]).
     */
    fun readCandidates(limit: Int): JobAnalysisCandidateDto {
        require(limit in 1..MAX_CANDIDATES) { "분석 후보는 1개 이상 ${MAX_CANDIDATES}개 이하로 읽습니다." }
        val jobs = jobRepository.findAnalysisCandidates(PageRequest.of(0, limit))
        val hashes = jobAnalysisRepository.findAllByJobIdIn(jobs.mapNotNull(Job::id))
            .associate { it.jobId to it.contentHash }
        val (unchanged, targets) = jobs.partition { hashes[it.id] == it.contentHash() }
        return JobAnalysisCandidateDto(targets = targets, unchanged = unchanged)
    }

    companion object {
        const val MAX_CANDIDATES = 500
        private val log = LoggerFactory.getLogger(JobAnalysisReader::class.java)
    }
}

/** 저장한 JSON을 읽는다. 저장은 [JobAnalysisManager]가 같은 칸 이름으로 한다. */
internal object JobAnalysisContentJson {

    fun read(node: JsonNode): JobAnalysisContent = JobAnalysisContent(
        tasks = node.path("tasks").map { JobAnalysisContent.Task(it.text("tag"), it.text("text")) },
        required = node.path("required").map(JsonNode::asText),
        preferred = node.path("preferred").map(JsonNode::asText),
        employment = node.path("employment").let {
            JobAnalysisContent.Employment(
                type = fact(it.path("type")),
                conversion = fact(it.path("conversion")),
                salary = fact(it.path("salary")),
                affiliation = fact(it.path("affiliation")),
            )
        },
        submission = node.path("submission").let {
            JobAnalysisContent.Submission(
                documents = fact(it.path("documents")),
                essay = fact(it.path("essay")),
                process = fact(it.path("process")),
                deadline = fact(it.path("deadline")),
            )
        },
        competencies = node.path("competencies").map {
            JobAnalysisContent.Competency(
                name = it.text("name"),
                quote = it.text("quote"),
                description = it.text("description"),
                experiences = it.path("experiences").map(JsonNode::asText),
            )
        },
    )

    private fun fact(node: JsonNode) = JobAnalysisContent.Fact(node.textOrNull("value"), node.textOrNull("note"))

    private fun JsonNode.text(name: String): String = path(name).asText()

    private fun JsonNode.textOrNull(name: String): String? = path(name).takeIf { it.isTextual }?.asText()
}
