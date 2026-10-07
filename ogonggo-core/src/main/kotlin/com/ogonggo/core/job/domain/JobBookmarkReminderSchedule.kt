package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import com.ogonggo.core.enumeration.EnumField
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

enum class JobBookmarkReminderScheduleStatus(override val code: Int, override val desc: String) : EnumField {
    PENDING(1, "평가 대기"), // D-1 대상 평가 중이며 커서 뒤 페이지가 남아 있을 수 있다.
    COMPLETED(2, "평가 완료"), // 현재 커서 뒤에 남은 후보가 없다.
    CANCELLED(3, "일정 취소"), // 마감 변경으로 이전 일정이 더 이상 유효하지 않다.
    SKIPPED(4, "일정 제외"), // D-1 시각이 이미 지나 신규 예약 알림을 만들지 않는다.
}

/**
 * 공고의 특정 모집 종료 일시에 대한 D-1 대상 평가 진행 상태다.
 * 알림 발송 이력은 이 테이블이 아니라 notifications가 소유하며, 새 마감은 이 일정 행을 만들거나 재사용한다.
 */
@Entity
@Table(
    name = "job_bookmark_reminder_schedules",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_job_reminder_schedule_deadline", columnNames = ["job_id", "recruitment_end_at"]),
    ],
    indexes = [
        Index(name = "idx_job_reminder_schedule_due", columnList = "status, reminder_at, id"),
    ],
)
internal class JobBookmarkReminderSchedule(
    @Column(name = "job_id", nullable = false)
    val jobId: Long,

    /** 같은 공고라도 마감 시각이 달라지면 별도의 일정으로 식별한다. */
    @Column(name = "recruitment_end_at", nullable = false)
    val recruitmentEndAt: LocalDateTime,

    reminderAt: LocalDateTime,

    status: JobBookmarkReminderScheduleStatus,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: JobBookmarkReminderScheduleStatus = status
        protected set

    /** 대상자를 ID 오름차순으로 나눠 읽을 때 마지막으로 평가한 스크랩 ID다. */
    @Column(name = "last_bookmark_id")
    var lastBookmarkId: Long? = null
        protected set

    @Column(name = "reminder_at", nullable = false)
    var reminderAt: LocalDateTime = reminderAt
        protected set

    fun cancel() {
        if (status == JobBookmarkReminderScheduleStatus.PENDING) {
            status = JobBookmarkReminderScheduleStatus.CANCELLED
        }
    }

    fun applyChange(reminderAt: LocalDateTime, status: JobBookmarkReminderScheduleStatus) {
        this.reminderAt = reminderAt
        this.status = status
        // 같은 마감 행을 새 평가에 재사용하므로 이전 마감의 페이지 위치는 이어받지 않는다.
        lastBookmarkId = null
    }

    fun skip() {
        if (status == JobBookmarkReminderScheduleStatus.PENDING) {
            status = JobBookmarkReminderScheduleStatus.SKIPPED
        }
    }

    fun advance(lastBookmarkId: Long?, isComplete: Boolean) {
        // 커서는 앞으로만 이동한다. 지난 ID는 이후 대상 자격이 바뀌어도 이 일정에서 재평가하지 않는다.
        // 마지막 후보까지 처리한 위치를 저장하고, 남은 대상이 있을 때 다음 실행에서 이어간다.
        if (lastBookmarkId != null) this.lastBookmarkId = lastBookmarkId
        status = if (isComplete) JobBookmarkReminderScheduleStatus.COMPLETED else JobBookmarkReminderScheduleStatus.PENDING
    }
}
