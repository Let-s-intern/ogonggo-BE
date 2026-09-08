package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobSourceUrlClick
import com.ogonggo.core.job.persistence.JobSourceUrlClickJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class JobSourceUrlClickAppender internal constructor(
    private val jobSourceUrlClickRepository: JobSourceUrlClickJpaRepository,
) {

    /**

    * 같은 사용자가 같은 공고를 여러 번 눌러도 최초 기록만 남기고 조용히 넘어간다.

    *

    * 유니크 제약 위반을 삼키므로 호출자가 트랜잭션을 열어 둔 채로 부르면 안 된다.

    * 제약 위반은 그 트랜잭션을 롤백 대상으로 만들고, 예외를 잡아도 커밋 시점에 다시 터진다.

    */

    fun append(userId: Long, jobId: Long) {
        if (jobSourceUrlClickRepository.existsByJobIdAndUserId(jobId, userId)) {
            return
        }

        try {
            jobSourceUrlClickRepository.saveAndFlush(JobSourceUrlClick(jobId = jobId, userId = userId))
        } catch (exception: DataIntegrityViolationException) {
            // 같은 사용자가 버튼을 연속으로 눌러 동시에 저장되면 유니크 제약이 막는다.
            // 저장이 자기 트랜잭션에서 롤백되고 기록은 상대 요청이 이미 남겼으므로 성공으로 본다.
        }
    }
}
