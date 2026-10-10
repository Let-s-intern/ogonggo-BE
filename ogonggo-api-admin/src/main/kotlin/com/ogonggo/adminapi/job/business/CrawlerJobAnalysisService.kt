package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAnalysisManager
import com.ogonggo.core.job.implement.JobAnalysisReader
import com.ogonggo.core.job.implement.JobReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 크롤러가 오공고 공고를 분석해 돌려주는 흐름이다. 대상을 고르는 것은 서버이고, 크롤러는 받아서 분석해 보낸다.
 * 크롤러가 등록한 공고만이 아니라 고용24·기업회원 공고도 대상이다.
 */
@Service
class CrawlerJobAnalysisService(
    private val jobReader: JobReader,
    private val jobAnalysisReader: JobAnalysisReader,
    private val jobAnalysisManager: JobAnalysisManager,
) {

    /**
     * 분석이 없거나 본문이 바뀐 게시 중 모집 공고를 최근 것부터 `size`건까지 고른다.
     *
     * 후보에는 본문이 아닌 칸만 바뀐 공고도 섞여 있다. 크롤러가 같은 내용으로 다시 교체하는 일이 잦아서다.
     * 그런 공고는 확인한 것으로 남겨 다음에 다시 읽지 않고, 대상이 `size`건 찰 때까지 후보를 더 읽는다.
     */
    @Transactional
    fun getTargets(size: Int): List<Job> {
        val targets = mutableListOf<Job>()
        repeat(MAX_ROUNDS) {
            val candidates = jobAnalysisReader.readCandidates(size)
            jobAnalysisManager.confirm(candidates.unchanged)
            targets += candidates.targets.filter { job -> targets.none { it.id == job.id } }
            if (targets.size >= size || candidates.unchanged.isEmpty()) {
                return targets.take(size)
            }
        }
        return targets.take(size)
    }

    /** 분석을 저장한다. 분석한 본문이 지금 본문과 다르면 저장하지 않고 충돌로 알린다. 공고가 지워졌으면 찾지 않는다. */
    @Transactional
    fun saveAnalysis(jobId: Long, command: CrawlerJobAnalysisCommand) {
        val job = jobReader.read(jobId)
        if (job.contentHash() != command.contentHash) {
            throw ConflictException(JobErrorCode.JOB_ANALYSIS_OUTDATED)
        }
        jobAnalysisManager.save(job, command.content, command.guideVersion, command.model)
    }

    companion object {
        /** 확인만 하고 끝나는 후보가 이어져도 한 요청이 한없이 돌지 않게 한다. */
        private const val MAX_ROUNDS = 5
    }
}
