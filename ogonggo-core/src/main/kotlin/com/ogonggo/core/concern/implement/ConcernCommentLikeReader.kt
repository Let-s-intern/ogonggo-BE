package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.persistence.ConcernCommentLikeJpaRepository
import org.springframework.stereotype.Component

@Component
class ConcernCommentLikeReader internal constructor(
    private val likeRepository: ConcernCommentLikeJpaRepository,
) {

    /** 댓글별 좋아요 수다. 받은 적 없는 댓글은 0이다. */
    fun countAll(commentIds: Collection<Long>): Map<Long, Long> {
        if (commentIds.isEmpty()) return emptyMap()
        val counts = likeRepository.countActiveByCommentIds(commentIds.toSet()).associate { it.id to it.count }
        return commentIds.associateWith { counts[it] ?: 0L }
    }

    fun readLikedCommentIds(userId: Long, commentIds: Collection<Long>): Set<Long> {
        if (commentIds.isEmpty()) return emptySet()
        return likeRepository.findActiveCommentIds(userId, commentIds.toSet()).toSet()
    }
}
