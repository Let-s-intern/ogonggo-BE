package com.ogonggo.core.job.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysis
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.persistence.JobAnalysisJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class JobAnalysisManager internal constructor(
    private val jobAnalysisRepository: JobAnalysisJpaRepository,
    private val objectMapper: ObjectMapper,
) {

    /**
     * AI 서버가 만든 분석을 공고의 지금 본문에 대한 분석으로 저장한다. 이미 있으면 바꾼다.
     * 본문이 분석한 것과 같은지는 호출자가 먼저 확인한다. 호출자는 공고를 잠가 읽어([JobReader.readForUpdate]) 같은 공고의
     * 분석 쓰기를 차례로 돌린다. 분석 행에는 잠글 행이 없을 수 있어(첫 저장) 공고 행으로 묶는다.
     * 운영자가 지금 본문에 대해 고친 분석은 덮어쓰지 않고 충돌로 알린다. 본문이 바뀐 뒤에는 평소처럼 바꾼다.
     */
    fun save(job: Job, content: JobAnalysisContent, guideVersion: Int?, model: String) {
        val jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." }
        val json = objectMapper.writeValueAsString(content)
        val existing = jobAnalysisRepository.findByJobId(jobId)
        if (existing?.isEditedByAdminFor(job.contentHash()) == true) {
            throw ConflictException(JobErrorCode.JOB_ANALYSIS_EDITED_BY_ADMIN)
        }
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

    /**
     * 운영자가 고친 분석을 지금 본문에 대한 분석으로 저장한다. 이미 있으면 바꾼다.
     * 사용자 화면에 바로 보이고, 본문이 바뀌기 전까지 AI 서버가 덮어쓰지 못한다. [save]와 같이 잠근 공고로 부른다.
     */
    fun edit(job: Job, content: JobAnalysisContent, userId: Long, now: LocalDateTime) {
        val jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." }
        val json = objectMapper.writeValueAsString(content)
        val existing = jobAnalysisRepository.findByJobId(jobId)
        if (existing == null) {
            jobAnalysisRepository.save(
                JobAnalysis.editedByAdmin(jobId, job.contentHash(), json, userId, now, job.updatedAt),
            )
            return
        }
        existing.edit(job.contentHash(), json, userId, now, job.updatedAt)
    }

    /**
     * 운영자가 관리자 콘솔에서 본문을 고친 뒤 부른다. 고치기 전 본문([previousContentHash])에 대해 운영자가 고친 분석이 있으면
     * 새 본문 해시로 옮겨 계속 사용자에게 보이고 AI 서버가 덮지 못하게 한다. 운영자 수정은 마지막 방어선이라
     * 운영자 자신의 본문 수정이 그 분석을 버리게 하지 않는다.
     * AI 서버 분석이거나 이미 낡은 운영자 분석이면 그대로 두어, 바뀐 본문을 AI 서버가 다시 분석한다. 잠근 공고로 부른다.
     */
    fun keepAdminEditCurrent(job: Job, previousContentHash: String) {
        val jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." }
        val existing = jobAnalysisRepository.findByJobId(jobId) ?: return
        if (existing.isEditedByAdminFor(previousContentHash)) {
            existing.restamp(job.contentHash(), job.updatedAt)
        }
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
