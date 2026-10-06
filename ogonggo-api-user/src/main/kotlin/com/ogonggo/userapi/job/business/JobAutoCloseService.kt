package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.implement.JobManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** 모집 종료 일시가 지난 채용공고를 마감하고 마감한 건수를 돌려준다. 스케줄러가 주기적으로 부른다. */
@Service
class JobAutoCloseService(
    private val jobManager: JobManager,
    private val clock: Clock,
) {

    @Transactional
    fun closeExpired(): Int = jobManager.closeExpired(LocalDateTime.now(clock))
}
