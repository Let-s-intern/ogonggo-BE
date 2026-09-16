package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.implement.RecruitmentPostCursor
import com.ogonggo.core.community.domain.QRecruitmentPost.recruitmentPost
import com.ogonggo.core.community.domain.QPostMetric.postMetric
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
internal class RecruitmentPostQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublishedCursorPage(
        cursor: RecruitmentPostCursor?,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): List<RecruitmentPost> {
        val predicates = publishedPredicates(filter).filterNotNull().toMutableList().apply {
            cursor?.let { add(cursorPredicate(it, sortType)) }
        }
        return queryFactory.selectFrom(recruitmentPost)
            .leftJoin(postMetric).on(postMetric.postId.eq(recruitmentPost.id))
            .where(*predicates.toTypedArray())
            .distinct()
            .orderBy(*sortType.toOrder())
            .limit((size + 1).toLong())
            .fetch()
    }

    private fun cursorPredicate(
        cursor: RecruitmentPostCursor,
        sortType: RecruitmentPostSortType,
    ) = when (sortType) {
        RecruitmentPostSortType.LATEST -> recruitmentPost.id.lt(cursor.id)
        RecruitmentPostSortType.DEADLINE -> recruitmentPost.recruitmentEndDate.gt(checkNotNull(cursor.deadline))
            .or(
                recruitmentPost.recruitmentEndDate.eq(cursor.deadline)
                    .and(recruitmentPost.id.lt(cursor.id)),
            )
        RecruitmentPostSortType.VIEW_COUNT -> VIEW_COUNT_OR_ZERO.lt(checkNotNull(cursor.metricCount))
            .or(
                VIEW_COUNT_OR_ZERO.eq(cursor.metricCount)
                    .and(recruitmentPost.id.lt(cursor.id)),
            )
        RecruitmentPostSortType.COMMENT_COUNT -> COMMENT_COUNT_OR_ZERO.lt(checkNotNull(cursor.metricCount))
            .or(
                COMMENT_COUNT_OR_ZERO.eq(cursor.metricCount)
                    .and(recruitmentPost.id.lt(cursor.id)),
            )
    }

    private fun publishedPredicates(filter: RecruitmentPostListFilter): Array<Predicate?> = arrayOf(
        recruitmentPost.publicationStatus.eq(PublicationStatus.PUBLISHED),
        recruitmentPost.deletedAt.isNull,
        filter.recruitmentTypes.takeIf { it.isNotEmpty() }?.let(recruitmentPost.recruitmentType::`in`),
        filter.progressMethods.takeIf { it.isNotEmpty() }?.let(recruitmentPost.progressMethod::`in`),
        filter.recruitmentStatuses.takeIf { it.isNotEmpty() }?.let(recruitmentPost.recruitmentStatus::`in`),
        filter.positions.takeIf { it.isNotEmpty() }?.let { recruitmentPost.positions.any().`in`(it) },
    )

    private fun RecruitmentPostSortType.toOrder() = when (this) {
        RecruitmentPostSortType.LATEST -> arrayOf(recruitmentPost.id.desc())
        RecruitmentPostSortType.DEADLINE -> arrayOf(recruitmentPost.recruitmentEndDate.asc(), recruitmentPost.id.desc())
        RecruitmentPostSortType.VIEW_COUNT -> arrayOf(VIEW_COUNT_OR_ZERO.desc(), recruitmentPost.id.desc())
        RecruitmentPostSortType.COMMENT_COUNT -> arrayOf(COMMENT_COUNT_OR_ZERO.desc(), recruitmentPost.id.desc())
    }

    companion object {
        private val VIEW_COUNT_OR_ZERO = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "coalesce({0}, 0)",
            postMetric.viewCount,
        )
        private val COMMENT_COUNT_OR_ZERO = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "coalesce({0}, 0)",
            postMetric.commentCount,
        )
    }
}
