package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "job_bookmarks",
    uniqueConstraints = [UniqueConstraint(name = "uk_job_bookmark_job_user", columnNames = ["job_id", "user_id"])],
    indexes = [Index(name = "idx_job_bookmark_user_active", columnList = "user_id, deleted_at, updated_at")],
)
internal class JobBookmark(
    @Column(name = "job_id", nullable = false)
    val jobId: Long, /* 북마크한 채용공고 식별자 */

    @Column(name = "user_id", nullable = false)
    val userId: Long, /* 북마크한 사용자 식별자 */
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채용공고 북마크 식별자 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 북마크 해제 일시 */
        protected set

    val isActive: Boolean
        get() = deletedAt == null

    fun restore() {
        deletedAt = null
    }

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}
