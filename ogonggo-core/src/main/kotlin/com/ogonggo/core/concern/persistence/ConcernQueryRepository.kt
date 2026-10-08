package com.ogonggo.core.concern.persistence

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.domain.QConcern.concern
import com.ogonggo.core.concern.domain.QConcernMetric.concernMetric
import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
internal class ConcernQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 지표는 고민글과 연관관계가 없어 식별자로 조인한다. 지표 행이 없으면 0으로 본다. */
    fun findPage(
        category: ConcernCategory?,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): Page<Concern> {
        val pageable = PageRequest.of(page, size)
        val predicates = arrayOf(
            concern.deletedAt.isNull,
            category?.let { concern.category.eq(it) },
        )
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

    private fun ConcernSortType.toOrder(): Array<OrderSpecifier<*>> = when (this) {
        ConcernSortType.LATEST -> arrayOf(concern.id.desc())
        ConcernSortType.VIEW_COUNT -> arrayOf(concernMetric.viewCount.coalesce(0L).desc(), concern.id.desc())
        ConcernSortType.COMMENT_COUNT -> arrayOf(concernMetric.commentCount.coalesce(0L).desc(), concern.id.desc())
    }
}
