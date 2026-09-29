package com.ogonggo.core.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 렛츠커리어로 보낼 학력·희망 조건 변경이다. 사용자당 한 행만 두고, 보낼 때 그 시점의 최신 값을 읽어 보낸다.
 * 여러 번 고쳐도 마지막 상태 한 번만 보내면 되기 때문이다.
 */
@Entity
@Table(name = "letscareer_job_profile_outbox")
internal class LetsCareerJobProfileOutbox(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long, /* 오공고 사용자 식별자 */

    requestedAt: LocalDateTime,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 아웃박스 식별자 */
        protected set

    @Column(name = "requested_at", nullable = false)
    var requestedAt: LocalDateTime = requestedAt /* 마지막으로 적재한 일시. 보내는 사이 다시 적재됐는지 가리는 데 쓴다 */
        protected set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0 /* 실패한 전송 횟수. 계속 실패하는 행이 다른 행을 막지 않도록 적은 순으로 보낸다 */
        protected set

    fun requestAgain(requestedAt: LocalDateTime) {
        this.requestedAt = requestedAt
        attemptCount = 0
    }
}
