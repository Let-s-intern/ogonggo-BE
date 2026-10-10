package com.ogonggo.userapi.notification.delivery.implement.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationDispatchSummaryDtoTest {

    @Test
    fun `접수 미확정 건수를 실패 및 결과 기록 미확정과 별도로 집계한다`() {
        val summary = NotificationDispatchSummaryDto()
            .include(NotificationDispatchOutcome.UNKNOWN)
            .include(NotificationDispatchOutcome.FAILED)
            .include(NotificationDispatchOutcome.UNRESOLVED)

        assertEquals(1, summary.unknownCount)
        assertEquals(1, summary.failedCount)
        assertEquals(1, summary.unresolvedCount)
        assertTrue(summary.hasResults)
    }
}
