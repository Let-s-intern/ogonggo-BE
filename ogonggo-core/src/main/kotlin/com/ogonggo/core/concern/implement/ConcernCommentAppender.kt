package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.implement.dto.ConcernCommentAppendDto
import com.ogonggo.core.concern.persistence.ConcernCommentJpaRepository
import org.springframework.stereotype.Component

@Component
class ConcernCommentAppender internal constructor(
    private val commentRepository: ConcernCommentJpaRepository,
) {

    fun append(dto: ConcernCommentAppendDto): ConcernComment = commentRepository.save(dto.toEntity())
}
