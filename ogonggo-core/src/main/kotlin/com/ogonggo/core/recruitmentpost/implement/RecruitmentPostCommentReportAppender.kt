package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostCommentReport
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostCommentReportJpaRepository
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostCommentReportAppendDto

@Component
class RecruitmentPostCommentReportAppender internal constructor(
    private val reportRepository: RecruitmentPostCommentReportJpaRepository,
) {

    fun append(command: RecruitmentPostCommentReportAppendDto) {
        reportRepository.save(
            RecruitmentPostCommentReport(
                commentId = command.commentId,
                userId = command.userId,
                reason = command.reason,
            ),
        )
    }
}
