package com.ogonggo.core.job.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysis
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.persistence.JobAnalysisJpaRepository
import org.springframework.stereotype.Component

@Component
class JobAnalysisManager internal constructor(
    private val jobAnalysisRepository: JobAnalysisJpaRepository,
    private val objectMapper: ObjectMapper,
) {

    /**
     * 공고의 지금 본문에 대한 분석으로 저장한다. 이미 있으면 바꾼다.
     * 본문이 분석한 것과 같은지는 호출자가 먼저 확인한다.
     */
    fun save(job: Job, content: JobAnalysisContent, guideVersion: Int?, model: String) {
        val jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." }
        val json = objectMapper.writeValueAsString(content)
        val existing = jobAnalysisRepository.findByJobId(jobId)
        if (existing == null) {
            jobAnalysisRepository.save(
                JobAnalysis(
                    jobId = jobId,
                    contentHash = job.contentHash(),
                    content = json,
                    guideVersion = guideVersion,
                    model = model,
                    jobUpdatedAt = job.updatedAt,
                ),
            )
            return
        }
        existing.replace(job.contentHash(), json, guideVersion, model, job.updatedAt)
    }

    /** 본문이 그대로인 공고를 확인한 것으로 남긴다. 다음 대상 선정에서 다시 비교하지 않는다. */
    fun confirm(jobs: Collection<Job>) {
        if (jobs.isEmpty()) {
            return
        }
        val byId = jobs.associateBy { checkNotNull(it.id) { "채용공고 식별자가 없습니다." } }
        jobAnalysisRepository.findAllByJobIdIn(byId.keys).forEach { analysis ->
            byId[analysis.jobId]?.let { analysis.confirm(it.updatedAt) }
        }
    }
}
