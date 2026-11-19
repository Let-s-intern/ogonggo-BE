package com.ogonggo.core.contentevent.persistence

import com.ogonggo.core.contentevent.domain.ContentEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

internal interface ContentEventJpaRepository : JpaRepository<ContentEvent, Long> {

    /** 보관 기간이 지난 이벤트 식별자를 오래된 것부터 준다. */
    @Query("select e.id from ContentEvent e where e.occurredAt < :cutoff order by e.id asc")
    fun findIdsOccurredBefore(cutoff: LocalDateTime, pageable: Pageable): List<Long>
}
