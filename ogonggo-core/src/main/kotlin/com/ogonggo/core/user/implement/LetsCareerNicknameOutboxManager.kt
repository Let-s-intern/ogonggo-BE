package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.LetsCareerNicknameOutbox
import com.ogonggo.core.user.implement.dto.LetsCareerNicknameOutboxDto
import com.ogonggo.core.user.persistence.LetsCareerNicknameOutboxJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class LetsCareerNicknameOutboxManager internal constructor(
    private val outboxRepository: LetsCareerNicknameOutboxJpaRepository,
) {

    /**
     * 호출한 닉네임 수정과 같은 트랜잭션에서 저장해야 한다. 수정이 롤백되면 적재도 함께 사라진다.
     * 이미 적재된 변경이 있으면 새 행을 만들지 않고 적재 일시만 갱신한다.
     */
    fun enqueue(userId: Long, requestedAt: LocalDateTime) {
        val outbox = outboxRepository.findByUserId(userId)
        if (outbox == null) {
            outboxRepository.save(LetsCareerNicknameOutbox(userId = userId, requestedAt = requestedAt))
            return
        }
        outbox.requestAgain(requestedAt)
        outboxRepository.save(outbox)
    }

    fun readPending(): List<LetsCareerNicknameOutboxDto> =
        outboxRepository.findTop100BySentAtIsNullOrderByAttemptCountAscRequestedAtAsc().map {
            LetsCareerNicknameOutboxDto(userId = it.userId, requestedAt = it.requestedAt, attemptCount = it.attemptCount)
        }

    /** 보내는 사이 다시 적재됐으면 보낸 것으로 표시하지 않는다. 그 변경은 다음 전송에서 보낸다. */
    fun markSent(outbox: LetsCareerNicknameOutboxDto, sentAt: LocalDateTime) {
        outboxRepository.markSent(outbox.userId, outbox.requestedAt, sentAt)
    }

    fun markFailed(outbox: LetsCareerNicknameOutboxDto) {
        outboxRepository.increaseAttemptCount(outbox.userId)
    }
}
