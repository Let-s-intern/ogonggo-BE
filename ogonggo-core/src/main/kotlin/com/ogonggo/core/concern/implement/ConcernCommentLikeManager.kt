package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernCommentLike
import com.ogonggo.core.concern.persistence.ConcernCommentLikeJpaRepository
import com.ogonggo.core.jpa.ensureRowCreated
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
internal class ConcernCommentLikeAppender(
    private val likeRepository: ConcernCommentLikeJpaRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(commentId: Long, userId: Long) {
        likeRepository.saveAndFlush(ConcernCommentLike(commentId = commentId, userId = userId))
    }
}

/**
 * 좋아요는 누를 때마다 결과가 같도록 만든다. 이미 누른 상태에서 다시 눌러도, 취소한 상태에서 다시 취소해도 그대로다.
 * 같은 사용자의 요청이 겹쳐도 판단을 조회한 값에 맡기지 않고 UPDATE 조건으로 처리한다.
 */
@Component
class ConcernCommentLikeManager internal constructor(
    private val likeRepository: ConcernCommentLikeJpaRepository,
    private val likeAppender: ConcernCommentLikeAppender,
) {

    fun like(commentId: Long, userId: Long, now: LocalDateTime) {
        ensureRowCreated(
            exists = { likeRepository.findByCommentIdAndUserId(commentId, userId) != null },
            create = { likeAppender.create(commentId, userId) },
        )
        likeRepository.restore(commentId, userId, now)
    }

    fun unlike(commentId: Long, userId: Long, now: LocalDateTime) {
        likeRepository.cancel(commentId, userId, now)
    }
}
