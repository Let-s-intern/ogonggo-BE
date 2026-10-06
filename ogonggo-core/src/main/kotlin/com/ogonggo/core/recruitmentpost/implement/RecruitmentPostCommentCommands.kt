package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment

data class RecruitmentPostCommentAppendCommand(
    val postId: Long,
    val parentId: Long?,
    val userId: Long,
    val content: String,
) {
    fun toEntity(): RecruitmentPostComment = RecruitmentPostComment.create(
        postId = postId,
        parentId = parentId,
        userId = userId,
        content = content,
    )
}

data class RecruitmentPostCommentReportAppendCommand(
    val commentId: Long,
    val userId: Long,
    val reason: String?,
)
