package com.ogonggo.core.recruitmentpost.persistence

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.ogonggo.core.recruitmentpost.domain.QRecruitmentPost.recruitmentPost
import com.ogonggo.core.recruitmentpost.domain.QRecruitmentPostApplication.recruitmentPostApplication
import com.ogonggo.core.recruitmentpost.domain.QRecruitmentPostBookmark.recruitmentPostBookmark
import com.ogonggo.core.recruitmentpost.domain.QRecruitmentPostMetric.recruitmentPostMetric
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostListFilterDto
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
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
        filter: RecruitmentPostListFilterDto,
        sortType: RecruitmentPostSortType,
    ): Page<RecruitmentPost> {
        val pageable = PageRequest.of(page, size)
        val predicates = publishedPredicates(filter).filterNotNull()
        val content = queryFactory
            .select(recruitmentPost, VIEW_COUNT_OR_ZERO, COMMENT_COUNT_OR_ZERO)
            .from(recruitmentPost)
            .leftJoin(recruitmentPostMetric).on(recruitmentPostMetric.postId.eq(recruitmentPost.id))
            .where(*predicates.toTypedArray())
            .distinct()
            .orderBy(*sortType.toOrder())
            .paged(pageable)
            .fetch()
            .map { checkNotNull(it.get(recruitmentPost)) { "조회된 모집글이 없습니다." } }
        val countQuery = queryFactory.select(recruitmentPost.count())
            .from(recruitmentPost)
            .where(*predicates.toTypedArray())
        return pageOf(content, pageable, countQuery)
    }

    fun findConsolePage(
        condition: RecruitmentPostConsoleSearchCondition,
        sortType: RecruitmentPostSortType,
        pageable: Pageable,
    ): Page<RecruitmentPost> {
        val predicates = consolePredicates(condition)
        val content = queryFactory.selectFrom(recruitmentPost)
            .leftJoin(recruitmentPostMetric).on(recruitmentPostMetric.postId.eq(recruitmentPost.id))
            .where(*predicates)
            .orderBy(*sortType.toOrder())
            .paged(pageable)
            .fetch()
        val countQuery = queryFactory.select(recruitmentPost.count())
            .from(recruitmentPost)
            .where(*predicates)
        return pageOf(content, pageable, countQuery)
    }

    fun findOwnedPage(
        ownerUserId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentPostType?,
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
            .paged(pageable)
            .fetch()
        val countQuery = queryFactory.select(recruitmentPost.count())
            .from(recruitmentPost)
            .where(*predicates)
        return pageOf(content, pageable, countQuery)
    }

    /**
     * 북마크한 모집글 중 공개된 것만 읽는다. 모집글과 북마크의 읽기 전용 연관관계를 쓰지 않고 식별자로 조인한다.
     */
    fun findBookmarkedPublishedPage(
        userId: Long,
        condition: RecruitmentPostBookmarkSearchCondition,
        pageable: Pageable,
    ): Page<RecruitmentPost> {
        val predicates = arrayOf(
            recruitmentPostBookmark.userId.eq(userId),
            recruitmentPostBookmark.deletedAt.isNull,
            recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.PUBLISHED),
            recruitmentPost.deletedAt.isNull,
            condition.recruitmentStatus?.let { recruitmentPost.recruitmentStatus.eq(it) },
            condition.recruitmentType?.let { recruitmentPost.recruitmentType.eq(it) },
            condition.keyword?.takeIf(String::isNotBlank)?.let { recruitmentPost.title.containsIgnoreCase(it) },
        )
        val content = queryFactory.select(recruitmentPost)
            .from(recruitmentPostBookmark)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostBookmark.postId))
            .where(*predicates)
            .orderBy(*bookmarkOrders(condition.sortType))
            .paged(pageable)
            .fetch()
        val countQuery = queryFactory.select(recruitmentPostBookmark.count())
            .from(recruitmentPostBookmark)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostBookmark.postId))
            .where(*predicates)
        return pageOf(content, pageable, countQuery)
    }

    private fun bookmarkOrders(sortType: BookmarkSortType): Array<OrderSpecifier<*>> = when (sortType) {
        BookmarkSortType.RECENTLY_SAVED -> arrayOf(recruitmentPostBookmark.updatedAt.desc(), recruitmentPostBookmark.id.desc())
    }

    private fun ownedPredicates(
        ownerUserId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentPostType?,
        keyword: String?,
    ): Array<Predicate?> = buildList {
        add(recruitmentPost.authorUserId.eq(ownerUserId))
        add(recruitmentPost.deletedAt.isNull)
        add(statusPredicate(status))
        recruitmentStatus?.let {
            add(recruitmentPost.publicationStatus.ne(RecruitmentPostPublicationStatus.DRAFT))
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
            RecruitmentPostPublicationStatus.DRAFT,
            RecruitmentPostPublicationStatus.PUBLISHED,
            RecruitmentPostPublicationStatus.HIDDEN,
        )
        RecruitmentPostManagementStatus.DRAFT -> recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.DRAFT)
        RecruitmentPostManagementStatus.PUBLISHED -> recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.PUBLISHED)
        RecruitmentPostManagementStatus.HIDDEN -> recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.HIDDEN)
    }

    private fun RecruitmentPostManagementSortType.toOrder() = when (this) {
        RecruitmentPostManagementSortType.LATEST_SAVED -> arrayOf(recruitmentPost.updatedAt.desc(), recruitmentPost.id.desc())
    }

    private fun consolePredicates(condition: RecruitmentPostConsoleSearchCondition): Array<Predicate?> = arrayOf(
        recruitmentPost.deletedAt.isNull,
        when (condition.published) {
            null -> recruitmentPost.publicationStatus.`in`(RecruitmentPostPublicationStatus.PUBLISHED, RecruitmentPostPublicationStatus.HIDDEN)
            true -> recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.PUBLISHED)
            false -> recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.HIDDEN)
        },
        condition.recruitmentType?.let(recruitmentPost.recruitmentType::eq),
        condition.recruitmentStatus?.let(recruitmentPost.recruitmentStatus::eq),
        condition.keyword?.takeIf(String::isNotBlank)?.let(recruitmentPost.title::containsIgnoreCase),
    )

    private fun publishedPredicates(filter: RecruitmentPostListFilterDto): Array<Predicate?> = arrayOf(
        recruitmentPost.publicationStatus.eq(RecruitmentPostPublicationStatus.PUBLISHED),
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
            recruitmentPostMetric.viewCount,
        )
        private val COMMENT_COUNT_OR_ZERO = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "coalesce({0}, 0)",
            recruitmentPostMetric.commentCount,
        )
    }
}
