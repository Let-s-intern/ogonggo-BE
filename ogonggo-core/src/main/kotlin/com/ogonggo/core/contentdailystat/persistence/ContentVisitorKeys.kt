package com.ogonggo.core.contentdailystat.persistence

import com.ogonggo.core.contentevent.domain.QContentEvent
import com.ogonggo.core.contentevent.domain.QContentEvent.contentEvent
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.jpa.JPAExpressions
import java.time.LocalDateTime

/**
 * 서로 다른 방문자를 가르는 키다. 코틀린 계산(`ContentMetricCalculator`)과 SQL 집계(`ContentDailyStatQueryRepository`)가
 * 같은 규칙으로 세도록 이 파일 한 곳에 둔다. 방문자 수를 세는 곳은 이 키를 쓴다.
 *
 * 규칙은 `userId ?: 표[visitorId] ?: visitorId`다. 표는 계산 범위 안에서 `userId`가 있는 이벤트로 만든 방문자id에서 회원id로 가는 대응이다.
 * 같은 브라우저가 로그인한 채로 보낸 이벤트와 로그인 정보 없이 보낸 이벤트를 한 사람으로 잇는다.
 * 한 방문자id가 회원 둘 이상과 이어지면(공용 기기) 가장 작은 회원id로 잇는다. 코틀린과 SQL이 같은 결과를 내려면 이 선택이 같아야 한다.
 *
 * 코틀린은 표를 [ContentDailyStatQueryRepository.findVisitorKeys]로 받아 [of]로 키를 만들고,
 * SQL은 [expression]이 같은 표를 하위 쿼리로 풀어 키 식을 만든다.
 */
internal class ContentVisitorKeys(private val userByVisitor: Map<String, Long>) {

    fun of(row: ContentEventRow): String = of(row.userId, row.visitorId)

    fun of(userId: Long?, visitorId: String): String =
        (userId ?: userByVisitor[visitorId])?.let { "$MEMBER_PREFIX$it" } ?: "$GUEST_PREFIX$visitorId"

    companion object {
        /** 표가 없는 키다. 방문자id를 회원과 잇지 않는다. */
        val NONE = ContentVisitorKeys(emptyMap())

        const val MEMBER_PREFIX = "u:"
        const val GUEST_PREFIX = "v:"

        /**
         * [of]와 같은 규칙의 SQL 식이다. [from]~[toExclusive] 안의 같은 방문자id 이벤트에서 표를 푼다.
         * 이 하위 쿼리는 `content_events(visitor_id, occurred_at)` 인덱스를 탄다.
         */
        fun expression(from: LocalDateTime, toExclusive: LocalDateTime): StringExpression {
            val sibling = QContentEvent("visitorKeySibling")
            val mappedUserId = Expressions.numberTemplate(
                Long::class.javaObjectType,
                "({0})",
                JPAExpressions.select(sibling.userId.min())
                    .from(sibling)
                    .where(
                        sibling.visitorId.eq(contentEvent.visitorId),
                        sibling.userId.isNotNull,
                        sibling.occurredAt.goe(from),
                        sibling.occurredAt.lt(toExclusive),
                    ),
            )
            return CaseBuilder()
                .`when`(contentEvent.userId.isNotNull).then(contentEvent.userId.stringValue().prepend(MEMBER_PREFIX))
                .otherwise(mappedUserId.stringValue().prepend(MEMBER_PREFIX).coalesce(contentEvent.visitorId.prepend(GUEST_PREFIX)))
        }
    }
}
