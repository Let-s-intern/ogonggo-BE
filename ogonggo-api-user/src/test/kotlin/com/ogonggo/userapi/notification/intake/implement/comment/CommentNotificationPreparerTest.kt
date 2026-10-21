package com.ogonggo.userapi.notification.intake.implement.comment

import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason
import com.ogonggo.core.usernotification.domain.UserNotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CommentNotificationPreparerTest {
    @Test
    fun `미리보기는 공백을 정리하고 Unicode 문자를 쪼개지 않는 최대 100자다`() {
        // given
        val profiles = Mockito.mock(UserProfileReader::class.java)
        val preparer = CommentNotificationPreparer(profiles)

        // when
        val prepared = preparer.prepare(
            UserNotificationType.CONCERN_COMMENT, 1, 2, 3, 4, "제목",
            "\n  ${"가".repeat(98)}😀\n  끝 이후", UserNotificationPushSkipReason.NO_ACTIVE_TOKEN,
        )

        // then
        assertEquals("${"가".repeat(98)}😀 ", prepared.preview)
        assertEquals(100, prepared.preview.codePointCount(0, prepared.preview.length))
        assertEquals("회원", prepared.actorNickname)
        assertNull(prepared.actorProfileImageUrl)
        assertEquals(UserNotificationPushSkipReason.NO_ACTIVE_TOKEN, prepared.pushSkipReason)
    }
}
