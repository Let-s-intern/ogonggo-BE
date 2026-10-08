package com.ogonggo.core.recruitmentpost.persistence

import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.ogonggo.core.recruitmentpost.domain.QRecruitmentPost.recruitmentPost
import com.ogonggo.core.recruitmentpost.domain.QRecruitmentPostApplication.recruitmentPostApplication
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplication
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.querydsl.core.Tuple
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

internal interface RecruitmentPostApplicationJpaRepository : JpaRepository<RecruitmentPostApplication, Long> {

    fun findByPostIdAndUserId(postId: Long, userId: Long): RecruitmentPostApplication?

    fun findByPostIdAndUserIdAndDeletedAtIsNull(postId: Long, userId: Long): RecruitmentPostApplication?
}

@Repository
internal class RecruitmentPostApplicationQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPage(
        userId: Long,
        publicationStatus: RecruitmentPostPublicationStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        recruitmentType: RecruitmentPostType?,
        keyword: String?,
        applicationStatus: RecruitmentPostApplicationProgressStatus? = null,
        sort: RecruitmentPostApplicationSortType = RecruitmentPostApplicationSortType.LATEST,
        pageable: Pageable,
    ): Page<RecruitmentPostApplicationRow> {
        val predicates = predicates(
            userId = userId,
            publicationStatus = publicationStatus,
            recruitmentStatus = recruitmentStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            applicationStatus = applicationStatus,
        )
        val content = queryFactory
            .select(
                Projections.constructor(
                    RecruitmentPostApplicationRow::class.java,
                    recruitmentPostApplication.id,
                    recruitmentPost.id,
                    recruitmentPost.title,
                    recruitmentPost.recruitmentType,
                    recruitmentPost.recruitmentStatus,
                    recruitmentPost.recruitmentEndDate,
                    recruitmentPost.progressMethod,
                    recruitmentPost.activityDurationMonths,
                    recruitmentPostApplication.applicationStatus,
                    recruitmentPostApplication.lastClickedAt,
                    recruitmentPost.authorUserId,
                ),
            )
            .from(recruitmentPostApplication)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostApplication.postId))
            .where(*predicates)
            .orderBy(*sort.toOrder())
            .paged(pageable)
            .fetch()

        val countQuery = queryFactory
            .select(recruitmentPostApplication.count())
            .from(recruitmentPostApplication)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostApplication.postId))
            .where(*predicates)

        return pageOf(content, pageable, countQuery)
    }

    fun countByPostIds(postIds: Collection<Long>): Map<Long, Long> {
        if (postIds.isEmpty()) return emptyMap()

        return queryFactory
            .select(recruitmentPostApplication.postId, recruitmentPostApplication.count())
            .from(recruitmentPostApplication)
            .where(recruitmentPostApplication.postId.`in`(postIds))
            .where(recruitmentPostApplication.deletedAt.isNull)
            .groupBy(recruitmentPostApplication.postId)
            .fetch()
            .associate { row: Tuple ->
                row.get(recruitmentPostApplication.postId)!! to
                    (row.get(recruitmentPostApplication.count()) ?: 0L)
            }
    }

    fun countByRecruitmentType(
        userId: Long,
        publicationStatus: RecruitmentPostPublicationStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        keyword: String?,
        applicationStatus: RecruitmentPostApplicationProgressStatus? = null,
    ): Map<RecruitmentPostType, Long> {
        val predicates = predicates(
            userId = userId,
            publicationStatus = publicationStatus,
            recruitmentStatus = recruitmentStatus,
            recruitmentType = null,
            keyword = keyword,
            applicationStatus = applicationStatus,
        )
        return queryFactory
            .select(recruitmentPost.recruitmentType, recruitmentPostApplication.count())
            .from(recruitmentPostApplication)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostApplication.postId))
            .where(
                *predicates,
                recruitmentPost.recruitmentType.`in`(RecruitmentPostType.SIDE_PROJECT, RecruitmentPostType.STUDY),
            )
            .groupBy(recruitmentPost.recruitmentType)
            .fetch()
            .associate { row: Tuple ->
                row.get(recruitmentPost.recruitmentType)!! to
                    (row.get(recruitmentPostApplication.count()) ?: 0L)
            }
    }

    private fun predicates(
        userId: Long,
        publicationStatus: RecruitmentPostPublicationStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        recruitmentType: RecruitmentPostType?,
        keyword: String?,
        applicationStatus: RecruitmentPostApplicationProgressStatus?,
    ): Array<Predicate?> = arrayOf(
        recruitmentPostApplication.userId.eq(userId),
        recruitmentPostApplication.deletedAt.isNull,
        recruitmentPost.publicationStatus.eq(publicationStatus),
        recruitmentPost.deletedAt.isNull,
        recruitmentStatus?.let(recruitmentPost.recruitmentStatus::eq),
        recruitmentType?.let(recruitmentPost.recruitmentType::eq),
        keywordContains(keyword),
        applicationStatus?.let(recruitmentPostApplication.applicationStatus::eq),
    )

    private fun keywordContains(keyword: String?): BooleanExpression? =
        keyword?.takeIf(String::isNotBlank)?.let(recruitmentPost.title::containsIgnoreCase)

    private fun RecruitmentPostApplicationSortType.toOrder() = when (this) {
        RecruitmentPostApplicationSortType.LATEST -> arrayOf(
            recruitmentPostApplication.firstClickedAt.desc(),
            recruitmentPostApplication.id.desc(),
        )
    }
}

data class RecruitmentPostApplicationRow(
    val applicationId: Long,
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod =
        com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentPostApplicationProgressStatus = RecruitmentPostApplicationProgressStatus.PREPARING,
    val lastClickedAt: LocalDateTime,
    val authorUserId: Long,
)
