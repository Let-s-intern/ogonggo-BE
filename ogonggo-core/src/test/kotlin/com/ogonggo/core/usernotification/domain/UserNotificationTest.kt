package com.ogonggo.core.usernotification.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UserNotificationTest {
    @Test
    @DisplayName("관리자 알림은 원글·댓글 없이 발송 묶음과 이동 URL을 가진다")
    fun `관리자 알림은 댓글 식별자를 요구하지 않는다`() {
        // given
        val batchId = UUID.randomUUID().toString()

        // when
        val notification = notification(UserNotificationType.ADMIN_NOTIFICATION, null, null, batchId, "https://www.ogonggo.co.kr")

        // then
        assertEquals(batchId, notification.batchId)
        assertNull(notification.postId)
        assertNull(notification.commentId)
        assertNull(notification.readAt)
    }

    @Test
    @DisplayName("관리자 알림은 발송 묶음과 이동 URL이 필요하고 댓글을 참조할 수 없다")
    fun `불완전한 관리자 알림을 거부한다`() {
        // given
        val type = UserNotificationType.ADMIN_NOTIFICATION
        val batchId = UUID.randomUUID().toString()

        // when
        val attempts = listOf<() -> UserNotification>(
            { notification(type, null, null, null, "https://www.ogonggo.co.kr") },
            { notification(type, null, null, batchId, null) },
            { notification(type, 1, 2, batchId, "https://www.ogonggo.co.kr") },
        )

        // then
        attempts.forEach { assertThrows(IllegalArgumentException::class.java) { it() } }
    }

    @Test
    @DisplayName("댓글 알림은 여전히 양수인 원글·댓글 ID가 필요하다")
    fun `관리자 알림 추가가 댓글 알림의 필수 식별자를 완화하지 않는다`() {
        // given
        val types = listOf(UserNotificationType.CONCERN_COMMENT, UserNotificationType.SIDE_STUDY_COMMENT_REPLY)

        // when
        val attempts = types.flatMap { type -> listOf<() -> UserNotification>(
            { notification(type, null, 2, null, null) },
            { notification(type, 1, null, null, null) },
            { notification(type, 0, 2, null, null) },
        ) }

        // then
        attempts.forEach { assertThrows(IllegalArgumentException::class.java) { it() } }
    }

    @Test
    @DisplayName("관리자 메시지 미리보기는 이모지도 유니코드 한 문자로 계산한다")
    fun `이모지 100자를 미리보기로 보관할 수 있다`() {
        // given
        val preview = "😀".repeat(100)

        // when
        val notification = UserNotification(
            10, UserNotificationType.ADMIN_NOTIFICATION, 42, null, null, "오공고", null, "제목", preview, null,
            UUID.randomUUID().toString(), "https://www.ogonggo.co.kr",
        )

        // then
        assertEquals(preview, notification.preview)
        assertEquals(100, notification.preview.codePointCount(0, notification.preview.length))
    }

    private fun notification(type: UserNotificationType, postId: Long?, commentId: Long?, batchId: String?, url: String?) = UserNotification(
        10, type, 42, postId, commentId, "오공고", null, "제목", "내용", null, batchId, url,
    )
}
