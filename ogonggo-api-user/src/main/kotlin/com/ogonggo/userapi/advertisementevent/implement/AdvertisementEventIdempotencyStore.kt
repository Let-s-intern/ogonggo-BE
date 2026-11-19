package com.ogonggo.userapi.advertisementevent.implement

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.security.MessageDigest
import java.util.UUID

/**
 * 완료된 이벤트 키를 Redis에서 빠르게 확인한다.
 * Redis는 중복 요청 최적화용이고, 영구 중복 방지는 advertisement_events의 DB unique key가 맡는다.
 */
@Component
class AdvertisementEventIdempotencyStore(
    private val redisTemplate: StringRedisTemplate,
    private val clock: Clock,
) {

    fun isCompleted(idempotencyKey: UUID): Boolean = try {
        redisTemplate.hasKey(redisKey(idempotencyKey)) == true
    } catch (exception: DataAccessException) {
        log.warn("광고 이벤트 멱등 키의 Redis 조회에 실패했습니다. DB 제약으로 중복을 확인합니다.", exception)
        false
    }

    /** 호출자는 DB 트랜잭션 커밋이 끝난 뒤에만 이 메서드를 호출한다. */
    fun markCompleted(idempotencyKey: UUID) {
        try {
            redisTemplate.opsForValue().set(redisKey(idempotencyKey), COMPLETED, CACHE_TTL)
        } catch (exception: DataAccessException) {
            // DB에 저장된 멱등 키가 최종 방어선이므로 캐시 실패로 정상 수집을 되돌리지 않는다.
            log.warn("광고 이벤트 멱등 키의 Redis 저장에 실패했습니다. DB 제약으로 중복을 확인합니다.", exception)
        }
    }

    /** 반복 impression은 이벤트 적재 전에 빠르게 거른다. DB unique key가 동시 요청을 최종 방어한다. */
    fun isDailyImpressionRecorded(exposureId: Long, eventDate: LocalDate, viewerKey: String): Boolean = try {
        redisTemplate.hasKey(dailyImpressionKey(exposureId, eventDate, viewerKey)) == true
    } catch (exception: DataAccessException) {
        log.warn("광고 노출 중복 캐시 조회에 실패했습니다. DB 제약으로 중복을 확인합니다.", exception)
        false
    }

    /** 호출자는 이벤트 DB 트랜잭션 커밋 뒤에만 이 메서드를 호출한다. */
    fun markDailyImpressionRecorded(exposureId: Long, eventDate: LocalDate, viewerKey: String) {
        try {
            val ttl = Duration.between(clock.instant(), eventDate.plusDays(1).atStartOfDay(SEOUL).toInstant())
                .coerceAtLeast(MIN_CACHE_TTL)
            redisTemplate.opsForValue().set(
                dailyImpressionKey(exposureId, eventDate, viewerKey),
                COMPLETED,
                ttl,
            )
        } catch (exception: DataAccessException) {
            log.warn("광고 노출 중복 캐시 저장에 실패했습니다. DB 제약으로 중복을 확인합니다.", exception)
        }
    }

    private fun redisKey(idempotencyKey: UUID): String = "$KEY_PREFIX$idempotencyKey"

    private fun dailyImpressionKey(exposureId: Long, eventDate: LocalDate, viewerKey: String): String {
        val viewerHash = MessageDigest.getInstance("SHA-256")
            .digest(viewerKey.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        return "$IMPRESSION_KEY_PREFIX$exposureId:$eventDate:$viewerHash"
    }

    private companion object {
        const val COMPLETED = "1"
        const val KEY_PREFIX = "ogonggo:advertisement-event:idempotency:"
        const val IMPRESSION_KEY_PREFIX = "ogonggo:advertisement-event:impression:"
        val CACHE_TTL: Duration = Duration.ofHours(24)
        val MIN_CACHE_TTL: Duration = Duration.ofSeconds(1)
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val log = LoggerFactory.getLogger(AdvertisementEventIdempotencyStore::class.java)
    }
}
