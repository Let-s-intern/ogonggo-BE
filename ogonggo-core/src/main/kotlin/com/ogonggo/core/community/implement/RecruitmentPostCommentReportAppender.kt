package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostCommentReport
import com.ogonggo.core.community.persistence.RecruitmentPostCommentReportJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostCommentReportAppender internal constructor(
    private val reportRepository: RecruitmentPostCommentReportJpaRepository,
) {

    fun append(command: RecruitmentPostCommentReportAppendCommand) {
        reportRepository.save(
            RecruitmentPostCommentReport(
                commentId = command.commentId,
                userId = command.userId,
                reason = command.reason,
            ),
        )
    }
}
