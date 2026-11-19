package com.ogonggo.userapi.advertisementevent.business

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventAudienceType
import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.AdvertisementEventAppender
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementEventAppendDto
import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.LetsCareerWishJobCatalog
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.userapi.advertisementevent.presentation.request.CreateAdvertisementEventRequest
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementEventIdempotencyStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Service
class AdvertisementEventService(
    private val advertisementExposureReader: AdvertisementExposureReader,
    private val advertisementEventAppender: AdvertisementEventAppender,
    private val advertisementEventIdempotencyStore: AdvertisementEventIdempotencyStore,
    private val userProfileReader: UserProfileReader,
    private val clock: Clock,
) {

    @Transactional
    fun record(userId: Long?, request: CreateAdvertisementEventRequest, idempotencyKey: UUID) {
        val eventAt = LocalDateTime.now(clock.withZone(SEOUL))
        val target = advertisementExposureReader.readEventTarget(request.exposureId)
        if (!target.category.exposable) {
            throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
        }
        if (!target.isShownAt(eventAt)) {
            throw InvalidValueException(AdvertisementEventErrorCode.ADVERTISEMENT_EVENT_EXPOSURE_NOT_SHOWN)
        }

        val eventDate = eventAt.toLocalDate()
        val viewerKey = userId?.let { "M:$it" } ?: "A:${request.anonymousId}"
        if (request.eventType == AdvertisementEventType.IMPRESSION &&
            advertisementEventIdempotencyStore.isDailyImpressionRecorded(target.exposureId, eventDate, viewerKey)
        ) {
            return
        }

        val jobRoles = if (request.eventType == AdvertisementEventType.CLICK && userId != null) {
            LetsCareerWishJobCatalog.jobRolesOf(userProfileReader.read(userId)?.wishJob)
        } else {
            emptySet()
        }
        val audienceType = when {
            userId == null -> AdvertisementEventAudienceType.NON_MEMBER
            jobRoles.isEmpty() -> AdvertisementEventAudienceType.UNSPECIFIED
            else -> AdvertisementEventAudienceType.MEMBER
        }
        advertisementEventAppender.append(
            AdvertisementEventAppendDto(
                eventDate = eventDate,
                eventAt = eventAt,
                exposureId = target.exposureId,
                productId = target.productId,
                contractId = target.contractId,
                category = target.category,
                position = target.position,
                targetType = target.targetType,
                targetId = target.targetId,
                eventType = request.eventType,
                memberId = userId,
                anonymousId = request.anonymousId,
                viewerKey = viewerKey,
                idempotencyKey = idempotencyKey.toString(),
                audienceType = audienceType,
                jobRoles = jobRoles,
            ),
        )

        if (request.eventType == AdvertisementEventType.IMPRESSION) {
            val cacheAfterCommit = {
                advertisementEventIdempotencyStore.markDailyImpressionRecorded(
                    target.exposureId,
                    eventDate,
                    viewerKey,
                )
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCommit() = cacheAfterCommit()
                    },
                )
            } else {
                cacheAfterCommit()
            }
        }
    }

    companion object {
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
