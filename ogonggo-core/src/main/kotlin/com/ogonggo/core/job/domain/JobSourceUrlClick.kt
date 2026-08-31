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

/**
 * 사용자가 채용공고 원문으로 이동하는 버튼을 눌렀다는 기록이다.
 *
 * 북마크와 달리 취소할 수 없는 사실이므로 소프트 삭제 컬럼을 두지 않는다.
 * 한 사용자가 같은 공고를 여러 번 눌러도 행은 하나만 남기며, `createdAt`이 최초 이동 시각이 된다.
 */
@Entity
@Table(
    name = "job_source_url_clicks",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_job_source_url_click_job_user", columnNames = ["job_id", "user_id"]),
    ],
    indexes = [Index(name = "idx_job_source_url_click_user", columnList = "user_id, created_at")],
)
internal class JobSourceUrlClick(
    @Column(name = "job_id", nullable = false)
    val jobId: Long, /* 원문으로 이동한 채용공고 식별자 */

    @Column(name = "user_id", nullable = false)
    val userId: Long, /* 원문으로 이동한 사용자 식별자 */
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 원문 이동 기록 식별자 */
        protected set
}
