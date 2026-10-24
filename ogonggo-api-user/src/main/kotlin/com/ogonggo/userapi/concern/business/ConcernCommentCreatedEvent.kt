package com.ogonggo.userapi.concern.business

import java.time.LocalDateTime

/** 고민 댓글이 작성되었다는 사실과 작성 시점의 값이다. 알림 종류나 발송 방법은 담지 않는다. */
data class ConcernCommentCreatedEvent(
    val concernId: Long,
    val concernAuthorUserId: Long,
    val concernTitle: String,
    val commentId: Long,
    val parentId: Long?,
    val authorUserId: Long,
    val content: String,
    val occurredAt: LocalDateTime,
)
