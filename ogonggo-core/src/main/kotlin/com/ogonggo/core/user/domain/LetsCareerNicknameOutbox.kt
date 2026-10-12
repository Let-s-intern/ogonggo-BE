package com.ogonggo.core.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 렛츠커리어로 보낼 닉네임 변경이다. `LetsCareerJobProfileOutbox`와 같이 사용자당 한 행만 두고,
 * 보낼 때 그 시점의 최신 닉네임과 고친 일시를 읽어 보낸다.
 */
@Entity
@Table(name = "letscareer_nickname_outbox")
internal class LetsCareerNicknameOutbox(
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

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null /* 보낸 일시. 비어 있으면 아직 보낼 대상이다 */
        protected set

    fun requestAgain(requestedAt: LocalDateTime) {
        this.requestedAt = requestedAt
        attemptCount = 0
        sentAt = null
    }
}
