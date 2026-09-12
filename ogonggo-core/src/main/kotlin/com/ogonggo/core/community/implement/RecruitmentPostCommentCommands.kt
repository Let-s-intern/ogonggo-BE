package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.RecruitmentPostComment

data class RecruitmentPostCommentAppendCommand(
    val post: Post,
    val parent: RecruitmentPostComment?,
    val userId: Long,
    val content: String,
) {
    fun toEntity(): RecruitmentPostComment = RecruitmentPostComment.create(
        post = post,
        parent = parent,
        userId = userId,
        content = content,
    )
}
