package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.domain.QRecruitmentPost.recruitmentPost
import com.ogonggo.core.community.domain.QPostMetric.postMetric
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
internal class RecruitmentPostQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublishedPage(
        page: Int,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): Page<RecruitmentPost> {
        val predicates = publishedPredicates(filter)
        val pageable = PageRequest.of(page, size)

        val content = queryFactory.selectFrom(recruitmentPost)
            .leftJoin(postMetric).on(postMetric.postId.eq(recruitmentPost.id))
            .where(*predicates)
            .distinct()
            .orderBy(*sortType.toOrder())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(recruitmentPost.id.countDistinct())
            .from(recruitmentPost)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
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
