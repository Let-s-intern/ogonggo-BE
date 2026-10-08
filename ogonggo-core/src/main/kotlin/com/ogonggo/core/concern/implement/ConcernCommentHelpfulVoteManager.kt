package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernCommentHelpfulVote
import com.ogonggo.core.concern.persistence.ConcernCommentHelpfulVoteJpaRepository
import com.ogonggo.core.jpa.ensureRowCreated
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
internal class ConcernCommentHelpfulVoteAppender(
    private val voteRepository: ConcernCommentHelpfulVoteJpaRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(commentId: Long, userId: Long) {
        voteRepository.saveAndFlush(ConcernCommentHelpfulVote(commentId = commentId, userId = userId))
    }
}

/**
 * "도움돼요"는 누를 때마다 결과가 같도록 만든다. 이미 누른 상태에서 다시 눌러도, 취소한 상태에서 다시 취소해도 그대로다.
 * 같은 사용자의 요청이 겹쳐도 판단을 조회한 값에 맡기지 않고 UPDATE 조건으로 처리한다.
 */
@Component
class ConcernCommentHelpfulVoteManager internal constructor(
    private val voteRepository: ConcernCommentHelpfulVoteJpaRepository,
    private val voteAppender: ConcernCommentHelpfulVoteAppender,
) {

    fun vote(commentId: Long, userId: Long, now: LocalDateTime) {
        ensureRowCreated(
            exists = { voteRepository.findByCommentIdAndUserId(commentId, userId) != null },
            create = { voteAppender.create(commentId, userId) },
        )
        voteRepository.restore(commentId, userId, now)
    }

    fun cancel(commentId: Long, userId: Long, now: LocalDateTime) {
        voteRepository.cancel(commentId, userId, now)
    }
}
