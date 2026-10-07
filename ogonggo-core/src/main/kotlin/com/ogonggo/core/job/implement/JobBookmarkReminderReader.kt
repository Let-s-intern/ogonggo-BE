package com.ogonggo.core.job.implement

import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.job.persistence.JobBookmarkReminderQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobBookmarkReminderReader internal constructor(
    private val queryRepository: JobBookmarkReminderQueryRepository,
) {

    /** due 공고의 커서 다음부터 현재도 발송 자격이 있고 아직 적재되지 않은 수신자를 읽는다. */
    fun readEligibleCandidates(
        now: LocalDateTime,
        afterBookmarkId: Long?,
        limit: Int,
    ): List<JobBookmarkReminderCandidateDto> {
        require(limit > 0)
        return queryRepository.findEligibleCandidates(
            now = now,
            afterBookmarkId = afterBookmarkId,
            limit = limit,
        )
    }
}
