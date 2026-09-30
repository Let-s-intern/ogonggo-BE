package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 운영자가 고른 오늘의 공고 한 건이다. 삭제되지 않은 행 전체가 지금의 오늘의 공고 목록이다.
 * 목록을 바꾸면 기존 행을 소프트 삭제하고 새 행을 넣으므로 같은 공고의 지난 행이 여러 개 남을 수 있어 유니크 제약을 두지 않는다.
 */
@Entity
@Table(
    name = "today_jobs",
    indexes = [Index(name = "idx_today_jobs_active", columnList = "deleted_at, display_order")],
)
internal class TodayJob(
    @Column(name = "job_id", nullable = false)
    val jobId: Long, /* 오늘의 공고로 고른 채용공고 식별자 */

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int, /* 오늘의 공고 노출 순서 */
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 오늘의 공고 식별자 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 오늘의 공고에서 뺀 일시 */
        protected set
}
