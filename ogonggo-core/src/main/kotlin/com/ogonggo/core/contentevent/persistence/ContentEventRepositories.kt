package com.ogonggo.core.contentevent.persistence

import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

internal interface ContentEventJpaRepository : JpaRepository<ContentEvent, Long> {

    /** 보관 기간이 지난 이벤트 식별자를 오래된 것부터 준다. */
    @Query("select e.id from ContentEvent e where e.occurredAt < :cutoff order by e.id asc")
    fun findIdsOccurredBefore(cutoff: LocalDateTime, pageable: Pageable): List<Long>

    /** [since] 뒤에 같은 방문자가 같은 대상을 연 조회 수다. 익명 방문자는 방문자 식별자로만 찾는다. */
    @Query(
        """
        select count(e) from ContentEvent e
        where e.eventType = com.ogonggo.core.contentevent.domain.ContentEventType.VIEW
          and e.targetType = :targetType and e.targetId = :targetId
          and e.occurredAt > :since and e.visitorId = :visitorId
        """,
    )
    fun countViewsByVisitorSince(targetType: ContentEventTargetType, targetId: Long, visitorId: String, since: LocalDateTime): Long

    /** [countViewsByVisitorSince]와 같고, 회원은 회원 식별자로 남은 조회도 같은 사람으로 본다. */
    @Query(
        """
        select count(e) from ContentEvent e
        where e.eventType = com.ogonggo.core.contentevent.domain.ContentEventType.VIEW
          and e.targetType = :targetType and e.targetId = :targetId
          and e.occurredAt > :since and (e.userId = :userId or e.visitorId = :visitorId)
        """,
    )
    fun countViewsByMemberSince(targetType: ContentEventTargetType, targetId: Long, userId: Long, visitorId: String, since: LocalDateTime): Long
}
