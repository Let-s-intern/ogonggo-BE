package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostCommentAppender internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {

    fun append(command: RecruitmentPostCommentAppendCommand): RecruitmentPostComment =
        commentRepository.save(command.toEntity())
}
