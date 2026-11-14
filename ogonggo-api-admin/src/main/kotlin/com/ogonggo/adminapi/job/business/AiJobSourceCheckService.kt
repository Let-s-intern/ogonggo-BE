package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * AI 서버가 상시 채용 공고의 원문을 열어 아직 모집 중인지 확인하는 흐름이다.
 * 상시 채용은 마감일이 없어 원문이 내려가도 계속 모집 중으로 남기 때문이다.
 * 대상은 등록 경로를 가리지 않는다. 크롤링 공고뿐 아니라 고용24·기업회원 공고도 원문이 내려가면 마감한다.
 */
@Service
class AiJobSourceCheckService(
    private val jobReader: JobReader,
    private val jobManager: JobManager,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getTargets(afterId: Long, size: Int): List<Job> = jobReader.readSourceCheckTargets(afterId, size)

    /** 원문이 내려간 공고를 마감한다. 이미 마감한 공고를 다시 마감해도 처음 마감 일시를 지키고 성공한다. */
    @Transactional
    fun close(jobId: Long) {
        jobManager.close(jobReader.readForUpdate(jobId), LocalDateTime.now(clock))
    }
}
