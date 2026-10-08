package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.persistence.ConcernCommentHelpfulVoteJpaRepository
import org.springframework.stereotype.Component

@Component
class ConcernCommentHelpfulVoteReader internal constructor(
    private val voteRepository: ConcernCommentHelpfulVoteJpaRepository,
) {

    /** 댓글별 "도움돼요" 수다. 받은 적 없는 댓글은 0이다. */
    fun countAll(commentIds: Collection<Long>): Map<Long, Long> {
        if (commentIds.isEmpty()) return emptyMap()
        val counts = voteRepository.countActiveByCommentIds(commentIds.toSet()).associate { it.id to it.count }
        return commentIds.associateWith { counts[it] ?: 0L }
    }

    fun readVotedCommentIds(userId: Long, commentIds: Collection<Long>): Set<Long> {
        if (commentIds.isEmpty()) return emptySet()
        return voteRepository.findActiveCommentIds(userId, commentIds.toSet()).toSet()
    }
}
