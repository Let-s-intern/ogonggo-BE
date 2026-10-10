package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.persistence.ConcernCommentJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConcernCommentRemover internal constructor(
    private val commentRepository: ConcernCommentJpaRepository,
) {

    fun remove(comment: ConcernComment, deletedAt: LocalDateTime) {
        comment.delete(deletedAt)
        commentRepository.save(comment)
    }
}
