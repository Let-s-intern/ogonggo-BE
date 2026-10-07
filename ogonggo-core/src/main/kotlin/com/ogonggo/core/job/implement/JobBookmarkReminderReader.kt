package com.ogonggo.core.job.implement

import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderScheduleDto
import com.ogonggo.core.job.persistence.JobBookmarkReminderQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobBookmarkReminderReader internal constructor(
    private val queryRepository: JobBookmarkReminderQueryRepository,
) {

    /** 일정의 커서 다음부터 현재도 발송 자격이 있는 수신자만 정해진 크기로 읽는다. */
    fun readEligibleCandidates(
        schedule: JobBookmarkReminderScheduleDto,
        now: LocalDateTime,
        limit: Int,
    ): List<JobBookmarkReminderCandidateDto> {
        require(limit > 0)
        return queryRepository.findEligibleCandidates(
            jobId = schedule.jobId,
            recruitmentEndAt = schedule.recruitmentEndAt,
            scheduledAt = schedule.reminderAt,
            now = now,
            afterBookmarkId = schedule.lastBookmarkId,
            limit = limit,
        )
    }
}
