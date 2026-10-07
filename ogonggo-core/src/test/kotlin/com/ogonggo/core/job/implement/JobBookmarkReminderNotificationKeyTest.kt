package com.ogonggo.core.job.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JobBookmarkReminderNotificationKeyTest {

    @Test
    @DisplayName("같은 공고·마감 시각·수신자의 리마인드는 재실행해도 같은 중복 키를 사용한다")
    fun `같은 발송 건은 결정적인 중복 키를 만든다`() {
        // given
        val reminderAt = LocalDateTime.of(2026, 10, 6, 9, 30)

        // when
        val firstKey = JobBookmarkReminderNotificationKey.forRecipient(7, 42, "KAKAO", reminderAt)
        val replayedKey = JobBookmarkReminderNotificationKey.forRecipient(7, 42, "KAKAO", reminderAt)
        val nextDeadlineKey = JobBookmarkReminderNotificationKey.forRecipient(
            7,
            42,
            "KAKAO",
            reminderAt.plusDays(1),
        )

        // then
        assertEquals(firstKey, replayedKey)
        assertNotEquals(firstKey, nextDeadlineKey)
    }
}
