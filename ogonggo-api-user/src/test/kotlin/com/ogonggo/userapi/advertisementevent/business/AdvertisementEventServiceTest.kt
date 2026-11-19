package com.ogonggo.userapi.advertisementevent.business

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import com.ogonggo.core.advertisementevent.implement.AdvertisementEventAppender
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementEventAppendDto
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureEventTargetDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementEventIdempotencyStore
import com.ogonggo.userapi.advertisementevent.presentation.request.CreateAdvertisementEventRequest
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class AdvertisementEventServiceTest {

    @Test
    @DisplayName("Redis가 같은 사용자·노출·서울 날짜의 노출을 알면 DB 적재기를 호출하지 않는다")
    fun `이미 처리한 일별 노출은 DB 쓰기 전에 반환한다`() {
        val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
        val eventAppender = Mockito.mock(AdvertisementEventAppender::class.java)
        val idempotencyStore = Mockito.mock(AdvertisementEventIdempotencyStore::class.java)
        val userProfileReader = Mockito.mock(UserProfileReader::class.java)
        val eventDate = LocalDate.of(2026, 11, 19)
        val anonymousId = "browser-anonymous-id"
        val exposureId = 77L
        val target = AdvertisementExposureEventTargetDto(
            exposureId = exposureId,
            productId = 8,
            contractId = 12,
            category = AdvertisementProductCategory.BANNER,
            position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
            targetType = null,
            targetId = null,
            exposed = true,
            startAt = null,
            endAt = null,
        )
        Mockito.`when`(exposureReader.readEventTarget(exposureId)).thenReturn(target)
        Mockito.`when`(idempotencyStore.isDailyImpressionRecorded(exposureId, eventDate, "A:$anonymousId"))
            .thenReturn(true)
        val service = AdvertisementEventService(
            exposureReader,
            eventAppender,
            idempotencyStore,
            userProfileReader,
            Clock.fixed(Instant.parse("2026-11-19T03:00:00Z"), ZoneOffset.UTC),
        )

        assertDoesNotThrow {
            service.record(
                null,
                CreateAdvertisementEventRequest(exposureId, AdvertisementEventType.IMPRESSION, anonymousId),
                UUID.randomUUID(),
            )
        }

        Mockito.verify(idempotencyStore).isDailyImpressionRecorded(exposureId, eventDate, "A:$anonymousId")
        Mockito.verifyNoInteractions(eventAppender)
        Mockito.verifyNoInteractions(userProfileReader)
    }

    @Test
    @DisplayName("클릭은 반복 수집해야 하므로 일별 노출 캐시를 조회하지 않는다")
    fun `click 이벤트는 Redis 일별 노출 캐시를 거치지 않는다`() {
        val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
        val eventAppender = Mockito.mock(AdvertisementEventAppender::class.java)
        val idempotencyStore = Mockito.mock(AdvertisementEventIdempotencyStore::class.java)
        val userProfileReader = Mockito.mock(UserProfileReader::class.java)
        val exposureId = 77L
        Mockito.`when`(exposureReader.readEventTarget(exposureId)).thenReturn(
            AdvertisementExposureEventTargetDto(
                exposureId = exposureId,
                productId = 8,
                contractId = 12,
                category = AdvertisementProductCategory.BANNER,
                position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                targetType = null,
                targetId = null,
                exposed = true,
                startAt = null,
                endAt = null,
            ),
        )
        val service = AdvertisementEventService(
            exposureReader,
            eventAppender,
            idempotencyStore,
            userProfileReader,
            Clock.fixed(Instant.parse("2026-11-19T03:00:00Z"), ZoneOffset.UTC),
        )

        service.record(
            null,
            CreateAdvertisementEventRequest(exposureId, AdvertisementEventType.CLICK, "browser-anonymous-id"),
            UUID.randomUUID(),
        )

        Mockito.verifyNoInteractions(idempotencyStore)
        val appendInvocation = Mockito.mockingDetails(eventAppender).invocations.single()
        org.junit.jupiter.api.Assertions.assertEquals("append", appendInvocation.method.name)
        val appendedEvent = appendInvocation.arguments.single() as AdvertisementEventAppendDto
        org.junit.jupiter.api.Assertions.assertEquals(AdvertisementEventType.CLICK, appendedEvent.eventType)
    }
}
