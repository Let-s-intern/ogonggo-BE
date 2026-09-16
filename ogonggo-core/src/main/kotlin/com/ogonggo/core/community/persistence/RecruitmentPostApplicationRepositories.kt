package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostApplication
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentApplicationSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.domain.QRecruitmentPost.recruitmentPost
import com.ogonggo.core.community.domain.QRecruitmentPostApplication.recruitmentPostApplication
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

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
        publicationStatus: PublicationStatus,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        applicationStatus: RecruitmentApplicationProgressStatus? = null,
        sort: RecruitmentApplicationSortType = RecruitmentApplicationSortType.LATEST,
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
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(recruitmentPostApplication.count())
            .from(recruitmentPostApplication)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostApplication.postId))
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
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
        publicationStatus: PublicationStatus,
        recruitmentStatus: RecruitmentStatus?,
        keyword: String?,
        applicationStatus: RecruitmentApplicationProgressStatus? = null,
    ): Map<RecruitmentType, Long> {
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
                recruitmentPost.recruitmentType.`in`(RecruitmentType.SIDE_PROJECT, RecruitmentType.STUDY),
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
        publicationStatus: PublicationStatus,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        applicationStatus: RecruitmentApplicationProgressStatus?,
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

    private fun RecruitmentApplicationSortType.toOrder() = when (this) {
        RecruitmentApplicationSortType.LATEST -> arrayOf(
            recruitmentPostApplication.firstClickedAt.desc(),
            recruitmentPostApplication.id.desc(),
        )
    }
}

data class RecruitmentPostApplicationRow(
    val applicationId: Long,
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: com.ogonggo.core.community.domain.ProgressMethod =
        com.ogonggo.core.community.domain.ProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentApplicationProgressStatus = RecruitmentApplicationProgressStatus.PREPARING,
    val lastClickedAt: LocalDateTime,
    val authorUserId: Long,
)
