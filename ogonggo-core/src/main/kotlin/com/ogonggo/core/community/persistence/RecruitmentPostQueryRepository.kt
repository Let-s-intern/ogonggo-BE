package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.domain.QRecruitmentPostApplication.recruitmentPostApplication
import com.ogonggo.core.community.domain.QRecruitmentPost.recruitmentPost
import com.ogonggo.core.community.domain.QPostMetric.postMetric
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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
        val predicates = publishedPredicates(filter).filterNotNull()
        val content = queryFactory.selectFrom(recruitmentPost)
            .leftJoin(postMetric).on(postMetric.postId.eq(recruitmentPost.id))
            .where(*predicates.toTypedArray())
            .distinct()
            .orderBy(*sortType.toOrder())
            .offset(page.toLong() * size)
            .limit(size.toLong())
            .fetch()
        val total = queryFactory.select(recruitmentPost.count())
            .from(recruitmentPost)
            .where(*predicates.toTypedArray())
            .fetchOne() ?: 0L
        val pageable = PageRequest.of(page, size)
        return PageImpl(content, pageable, total)
    }

    fun findOwnedPage(
        ownerUserId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        pageable: Pageable,
        sort: RecruitmentPostManagementSortType,
    ): Page<RecruitmentPost> {
        val predicates = ownedPredicates(
            ownerUserId = ownerUserId,
            status = status,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
        )
        val content = queryFactory.selectFrom(recruitmentPost)
            .where(*predicates)
            .orderBy(*sort.toOrder())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val total = queryFactory.select(recruitmentPost.count())
            .from(recruitmentPost)
            .where(*predicates)
            .fetchOne() ?: 0L
        return PageImpl(content, pageable, total)
    }

    private fun ownedPredicates(
        ownerUserId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
    ): Array<Predicate?> = buildList {
        add(recruitmentPost.authorUserId.eq(ownerUserId))
        add(recruitmentPost.deletedAt.isNull)
        add(statusPredicate(status))
        recruitmentStatus?.let {
            add(recruitmentPost.publicationStatus.ne(PublicationStatus.DRAFT))
            add(recruitmentPost.recruitmentStatus.eq(it))
        }
        recruitmentType?.let { add(recruitmentPost.recruitmentType.eq(it)) }
        applicationStatus?.let {
            val hasApplications = JPAExpressions.selectOne()
                .from(recruitmentPostApplication)
                .where(
                    recruitmentPostApplication.postId.eq(recruitmentPost.id),
                    recruitmentPostApplication.deletedAt.isNull,
                )
                .exists()
            add(if (it == RecruitmentPostApplicationStatus.HAS_APPLICATIONS) hasApplications else hasApplications.not())
        }
        keyword?.takeIf(String::isNotBlank)?.let { add(recruitmentPost.title.containsIgnoreCase(it)) }
    }.toTypedArray()

    private fun statusPredicate(status: RecruitmentPostManagementStatus): Predicate = when (status) {
        RecruitmentPostManagementStatus.ALL -> recruitmentPost.publicationStatus.`in`(
            PublicationStatus.DRAFT,
            PublicationStatus.PUBLISHED,
            PublicationStatus.HIDDEN,
        )
        RecruitmentPostManagementStatus.DRAFT -> recruitmentPost.publicationStatus.eq(PublicationStatus.DRAFT)
        RecruitmentPostManagementStatus.PUBLISHED -> recruitmentPost.publicationStatus.eq(PublicationStatus.PUBLISHED)
        RecruitmentPostManagementStatus.HIDDEN -> recruitmentPost.publicationStatus.eq(PublicationStatus.HIDDEN)
    }

    private fun RecruitmentPostManagementSortType.toOrder() = when (this) {
        RecruitmentPostManagementSortType.LATEST_SAVED -> arrayOf(recruitmentPost.updatedAt.desc(), recruitmentPost.id.desc())
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
