package com.ogonggo.core.advertisementevent.implement

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AdvertisementEventCleanupManager internal constructor(
    private val jdbcTemplate: JdbcTemplate,
) {

    /** 식별 정보가 있는 이벤트 원본과 고유 사용자 행만 나눠 지운다. 익명 집계는 유지한다. */
    fun deleteExpired(cutoff: LocalDate, limit: Int): Int {
        val viewers = jdbcTemplate.update(
            "DELETE FROM advertisement_event_daily_viewers WHERE event_date < ? LIMIT ?",
            cutoff,
            limit,
        )
        val events = jdbcTemplate.update(
            "DELETE FROM advertisement_events WHERE event_date < ? LIMIT ?",
            cutoff,
            limit,
        )
        return viewers + events
    }
}
