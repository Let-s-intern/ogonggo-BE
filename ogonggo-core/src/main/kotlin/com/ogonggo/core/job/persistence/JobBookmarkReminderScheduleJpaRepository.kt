package com.ogonggo.core.job.persistence

import com.ogonggo.core.job.domain.JobBookmarkReminderSchedule
import com.ogonggo.core.job.domain.JobBookmarkReminderScheduleStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface JobBookmarkReminderScheduleJpaRepository : JpaRepository<JobBookmarkReminderSchedule, Long> {

    /** 공고와 정확한 모집 종료 시각 조합은 일정당 하나만 유지한다. */
    fun findByJobIdAndRecruitmentEndAt(jobId: Long, recruitmentEndAt: LocalDateTime): JobBookmarkReminderSchedule?

    /** 매분 ShedLock으로 직렬화된 적재 작업이 평가 중인 일정을 읽는다. */
    @Query(
        """
        select schedule from JobBookmarkReminderSchedule schedule
        where schedule.status = :status and schedule.reminderAt <= :now
        order by schedule.reminderAt, schedule.id
        """,
    )
    fun findDue(
        @Param("status") status: JobBookmarkReminderScheduleStatus,
        @Param("now") now: LocalDateTime,
        pageable: org.springframework.data.domain.Pageable,
    ): List<JobBookmarkReminderSchedule>
}
