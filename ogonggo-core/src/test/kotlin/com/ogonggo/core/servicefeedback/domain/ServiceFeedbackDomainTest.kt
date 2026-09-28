package com.ogonggo.core.servicefeedback.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServiceFeedbackDomainTest {

    @Test
    fun `두 문항이 모두 비었으면 개선 의견을 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) { ServiceFeedback(userId = 1L, satisfaction = null, improvement = null) }
        assertThrows(IllegalArgumentException::class.java) { ServiceFeedback(userId = 1L, satisfaction = " ", improvement = "") }
    }

    @Test
    fun `한 문항만 채워도 비로그인으로 만들 수 있다`() {
        val serviceFeedback = ServiceFeedback(userId = null, satisfaction = null, improvement = "필터가 더 많았으면 좋겠어요.")

        assertNull(serviceFeedback.userId)
        assertEquals("필터가 더 많았으면 좋겠어요.", serviceFeedback.improvement)
    }

    @Test
    fun `각 문항은 1000자를 넘을 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServiceFeedback(userId = null, satisfaction = "가".repeat(1001), improvement = null)
        }
    }
}
