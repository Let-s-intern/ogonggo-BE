package com.ogonggo.userapi.adposting.implement

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.userapi.adposting.business.UserAdPostingResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class UserAdPostingCacheTest {

    private var now: Instant = Instant.parse("2026-11-06T01:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }
    private val cache = UserAdPostingCache(clock)
    private var loads = 0

    @Test
    fun `자리마다 30초 동안 처음 읽은 결과를 주고 그 뒤에 다시 읽는다`() {
        // given
        cache.get(AdPostingPlacement.JOB_LIST, ::load)

        // when
        now = now.plusSeconds(29)
        cache.get(AdPostingPlacement.JOB_LIST, ::load)
        cache.get(AdPostingPlacement.JOB_PICK, ::load)
        val beforeExpiry = loads
        now = now.plusSeconds(1)
        val reloaded = cache.get(AdPostingPlacement.JOB_LIST, ::load)

        // then
        assertEquals(2, beforeExpiry)
        assertEquals(3, loads)
        assertEquals(3L, reloaded.single().id)
    }

    private fun load(): List<UserAdPostingResult> {
        loads += 1
        return listOf(UserAdPostingResult(id = loads.toLong(), placement = AdPostingPlacement.JOB_LIST, targetType = AdPostingTargetType.JOB))
    }
}
