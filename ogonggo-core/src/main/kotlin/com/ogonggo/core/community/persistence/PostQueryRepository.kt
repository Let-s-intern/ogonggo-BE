package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.domain.QPost.post
import com.ogonggo.core.community.domain.QPostMetric.postMetric
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
internal class PostQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublishedPage(
        page: Int,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): Page<Post> {
        val predicates = publishedPredicates(filter)
        val pageable = PageRequest.of(page, size)

        val content = queryFactory.selectFrom(post)
            .leftJoin(postMetric).on(postMetric.postId.eq(post.id))
            .where(*predicates)
            .distinct()
            .orderBy(*sortType.toOrder())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(post.id.countDistinct())
            .from(post)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun publishedPredicates(filter: RecruitmentPostListFilter): Array<Predicate?> = arrayOf(
        post.publicationStatus.eq(PublicationStatus.PUBLISHED),
        post.deletedAt.isNull,
        filter.recruitmentTypes.takeIf { it.isNotEmpty() }?.let(post.recruitmentType::`in`),
        filter.progressMethods.takeIf { it.isNotEmpty() }?.let(post.progressMethod::`in`),
        filter.recruitmentStatuses.takeIf { it.isNotEmpty() }?.let(post.recruitmentStatus::`in`),
        filter.positions.takeIf { it.isNotEmpty() }?.let { post.positions.any().`in`(it) },
    )

    private fun RecruitmentPostSortType.toOrder() = when (this) {
        RecruitmentPostSortType.LATEST -> arrayOf(post.id.desc())
        RecruitmentPostSortType.DEADLINE -> arrayOf(post.recruitmentEndDate.asc(), post.id.desc())
        RecruitmentPostSortType.VIEW_COUNT -> arrayOf(VIEW_COUNT_OR_ZERO.desc(), post.id.desc())
        RecruitmentPostSortType.COMMENT_COUNT -> arrayOf(COMMENT_COUNT_OR_ZERO.desc(), post.id.desc())
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
