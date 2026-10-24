package com.ogonggo.userapi.recruitmentpost.business

import java.time.LocalDateTime

/** 모집글 댓글의 작성 사실이다. 최상위 댓글은 부모 식별자와 부모 작성자가 모두 null이다. */
data class RecruitmentPostCommentCreatedEvent(
    val postId: Long,
    val postTitle: String,
    val commentId: Long,
    val parentId: Long?,
    val parentAuthorUserId: Long?,
    val authorUserId: Long,
    val content: String,
    val occurredAt: LocalDateTime,
)
