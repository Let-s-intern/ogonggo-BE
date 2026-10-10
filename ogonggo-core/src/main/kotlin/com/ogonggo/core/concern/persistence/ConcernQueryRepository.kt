package com.ogonggo.core.concern.persistence

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernConsoleSearchCondition
import com.ogonggo.core.concern.domain.ConcernPopularSortType
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.domain.QConcern.concern
import com.ogonggo.core.concern.domain.QConcernMetric.concernMetric
import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class ConcernQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 지표는 고민글과 연관관계가 없어 식별자로 조인한다. 지표 행이 없으면 0으로 본다. 숨긴 고민글은 뺀다. */
    fun findPage(
        category: ConcernCategory?,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): Page<Concern> = findPage(
        predicates = arrayOf(
            concern.hidden.isFalse,
            concern.deletedAt.isNull,
            category?.let { concern.category.eq(it) },
        ),
        sortType = sortType,
        pageable = PageRequest.of(page, size),
    )

    /** 관리자 콘솔 목록이다. 숨긴 고민글도 싣는다. */
    fun findConsolePage(
        condition: ConcernConsoleSearchCondition,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): Page<Concern> = findPage(
        predicates = arrayOf(
            concern.deletedAt.isNull,
            condition.visible?.let { concern.hidden.eq(!it) },
            condition.category?.let { concern.category.eq(it) },
            condition.keyword?.takeIf(String::isNotBlank)?.let(concern.title::containsIgnoreCase),
        ),
        sortType = sortType,
        pageable = PageRequest.of(page, size),
    )

    private fun findPage(
        predicates: Array<Predicate?>,
        sortType: ConcernSortType,
        pageable: PageRequest,
    ): Page<Concern> {
        val content = queryFactory.selectFrom(concern)
            .leftJoin(concernMetric).on(concernMetric.concernId.eq(concern.id))
            .where(*predicates)
            .orderBy(*sortType.toOrder())
            .paged(pageable)
            .fetch()
        val countQuery = queryFactory.select(concern.count())
            .from(concern)
            .where(*predicates)
        return pageOf(content, pageable, countQuery)
    }

    /** [createdFrom] 이후에 등록한 숨기지 않은 고민글 중 기준 값이 큰 순서로 [limit]건을 읽는다. 값이 같으면 최근 글이 앞이다. */
    fun findPopular(
        createdFrom: LocalDateTime,
        sortType: ConcernPopularSortType,
        limit: Int,
    ): List<Concern> = queryFactory.selectFrom(concern)
        .leftJoin(concernMetric).on(concernMetric.concernId.eq(concern.id))
        .where(concern.hidden.isFalse, concern.deletedAt.isNull, concern.createdAt.goe(createdFrom))
        .orderBy(*sortType.toOrder())
        .limit(limit.toLong())
        .fetch()

    private fun ConcernPopularSortType.toOrder(): Array<OrderSpecifier<*>> = when (this) {
        ConcernPopularSortType.VIEW_COUNT -> arrayOf(concernMetric.viewCount.coalesce(0L).desc(), concern.id.desc())
        ConcernPopularSortType.COMMENT_COUNT -> arrayOf(concernMetric.commentCount.coalesce(0L).desc(), concern.id.desc())
    }

    private fun ConcernSortType.toOrder(): Array<OrderSpecifier<*>> = when (this) {
        ConcernSortType.LATEST -> arrayOf(concern.id.desc())
        ConcernSortType.VIEW_COUNT -> arrayOf(concernMetric.viewCount.coalesce(0L).desc(), concern.id.desc())
        ConcernSortType.COMMENT_COUNT -> arrayOf(concernMetric.commentCount.coalesce(0L).desc(), concern.id.desc())
    }
}
