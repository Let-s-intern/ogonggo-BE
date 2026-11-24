package com.ogonggo.userapi.featureflag.implement

import com.ogonggo.core.featureflag.domain.FeatureFlag
import com.ogonggo.core.featureflag.implement.FeatureFlagReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class UserFeatureFlagCacheTest {

    private val featureFlagReader: FeatureFlagReader = Mockito.mock(FeatureFlagReader::class.java)
    private val clock = MutableClock(Instant.parse("2026-11-24T00:00:00Z"))
    private lateinit var cache: UserFeatureFlagCache

    @BeforeEach
    fun setUp() {
        cache = UserFeatureFlagCache(featureFlagReader, clock)
    }

    @Test
    fun `30초 안에 다시 조회하면 저장소를 다시 읽지 않는다`() {
        // given
        val flags = listOf(featureFlag("flag_a", true))
        Mockito.`when`(featureFlagReader.readAll()).thenReturn(flags)

        // when
        val first = cache.readAll()
        clock.advanceMillis(29_999)
        val second = cache.readAll()

        // then
        assertEquals(listOf("flag_a"), first.map { it.flagKey })
        assertEquals(first, second)
        Mockito.verify(featureFlagReader, Mockito.times(1)).readAll()
    }

    @Test
    fun `30초가 지나면 저장소를 다시 읽어 바뀐 값을 준다`() {
        // given
        val enabledFlags = listOf(featureFlag("flag_a", true))
        val disabledFlags = listOf(featureFlag("flag_a", false))
        Mockito.`when`(featureFlagReader.readAll()).thenReturn(enabledFlags).thenReturn(disabledFlags)

        // when
        val before = cache.readAll()
        clock.advanceMillis(30_000)
        val after = cache.readAll()

        // then
        assertEquals(listOf(true), before.map { it.enabled })
        assertEquals(listOf(false), after.map { it.enabled })
        Mockito.verify(featureFlagReader, Mockito.times(2)).readAll()
    }

    @Test
    fun `읽기에 실패하면 캐시를 남기지 않고 다음 요청이 다시 읽는다`() {
        // given
        val flags = listOf(featureFlag("flag_a", true))
        Mockito.`when`(featureFlagReader.readAll()).thenThrow(IllegalStateException("DB 오류")).thenReturn(flags)

        // when & then
        assertThrows(IllegalStateException::class.java) { cache.readAll() }
        assertEquals(listOf("flag_a"), cache.readAll().map { it.flagKey })
        Mockito.verify(featureFlagReader, Mockito.times(2)).readAll()
    }

    private fun featureFlag(key: String, enabled: Boolean): FeatureFlag {
        val featureFlag = Mockito.mock(FeatureFlag::class.java)
        Mockito.`when`(featureFlag.flagKey).thenReturn(key)
        Mockito.`when`(featureFlag.enabled).thenReturn(enabled)
        return featureFlag
    }

    /** 테스트가 시간을 직접 옮기는 Clock이다. */
    private class MutableClock(start: Instant) : Clock() {
        private var instant: Instant = start

        fun advanceMillis(millis: Long) {
            instant = instant.plusMillis(millis)
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instant
    }
}
