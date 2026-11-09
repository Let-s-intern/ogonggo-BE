package com.ogonggo.userapi.advertisementcontract.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.userapi.advertisementcontract.implement.dto.AdvertisementContractNotificationDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

class AdvertisementContractNotificationAppenderTest {

    @Test
    @DisplayName("광고 신청 이메일과 Slack을 안정적인 중복 키로 공통 대기열에 적재한다")
    fun `두 채널의 알림을 계약 ID 기반 중복 키와 같은 payload로 적재한다`() {
        // given
        val coreAppender = Mockito.mock(NotificationAppender::class.java)
        val capturedNotifications = AtomicReference<Collection<NotificationAppendDto>?>(null)
        Mockito.doAnswer { invocation ->
            capturedNotifications.set(invocation.getArgument(0))
            2
        }.`when`(coreAppender).appendAll(Mockito.anyCollection())
        val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        val service = AdvertisementContractNotificationAppender(coreAppender, ObjectMapper(), clock)

        // when
        val inserted = service.append(NOTIFICATION)

        // then
        assertEquals(2, inserted)
        Mockito.verify(coreAppender).appendAll(Mockito.anyCollection())
        val notifications = checkNotNull(capturedNotifications.get())
        assertEquals(listOf(NotificationChannel.EMAIL, NotificationChannel.SLACK), notifications.map { it.channel })
        assertEquals(
            listOf("advertisement-contract:1001:email", "advertisement-contract:1001:slack"),
            notifications.map { it.deduplicationKey },
        )
        assertEquals(listOf("advertisement_contract_received", "advertisement_contract_submitted"), notifications.map { it.templateCode })
        assertEquals("marketing@example.com", notifications.first().recipientAddress)
        assertEquals("advertisement-inquiry", notifications.last().recipientAddress)
        assertEquals(setOf(LocalDateTime.of(2026, 11, 8, 12, 0)), notifications.map { it.scheduledAt }.toSet())
        assertEquals(notifications.first().payloadJson, notifications.last().payloadJson)
        val payload = ObjectMapper().readValue(notifications.first().payloadJson, Map::class.java)
        assertEquals("백엔드 인턴 채용", payload["recruitmentTitle"])
        assertEquals("채용 공고", payload["promotionTarget"])
        assertEquals("적합한 지원자 확보", payload["purpose"])
        assertEquals("2026-12-31", payload["recruitmentDeadline"])
        assertEquals("100만~300만원", payload["budgetRange"])
    }

    private companion object {
        val NOTIFICATION = AdvertisementContractNotificationDto(
            contractId = 1001L,
            promotionTarget = "채용 공고",
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentDeadline = "2026-12-31",
            purpose = "적합한 지원자 확보",
            budgetRange = "100만~300만원",
            categories = listOf("배너", "인스타그램 1"),
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
        )
    }
}
