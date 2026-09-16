package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostApplication
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
        pageable: Pageable,
    ): Page<RecruitmentPostApplicationRow> {
        val predicates = predicates(userId, publicationStatus, recruitmentStatus, recruitmentType, keyword)
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
                    recruitmentPostApplication.lastClickedAt,
                    recruitmentPost.authorUserId,
                ),
            )
            .from(recruitmentPostApplication)
            .join(recruitmentPost).on(recruitmentPost.id.eq(recruitmentPostApplication.postId))
            .where(*predicates)
            .orderBy(recruitmentPostApplication.lastClickedAt.desc(), recruitmentPostApplication.id.desc())
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
            .groupBy(recruitmentPostApplication.postId)
            .fetch()
            .associate { row: Tuple ->
                row.get(recruitmentPostApplication.postId)!! to
                    (row.get(recruitmentPostApplication.count()) ?: 0L)
            }
    }

    private fun predicates(
        userId: Long,
        publicationStatus: PublicationStatus,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
    ): Array<Predicate?> = arrayOf(
        recruitmentPostApplication.userId.eq(userId),
        recruitmentPost.publicationStatus.eq(publicationStatus),
        recruitmentPost.deletedAt.isNull,
        recruitmentStatus?.let(recruitmentPost.recruitmentStatus::eq),
        recruitmentType?.let(recruitmentPost.recruitmentType::eq),
        keywordContains(keyword),
    )

    private fun keywordContains(keyword: String?): BooleanExpression? =
        keyword?.takeIf(String::isNotBlank)?.let(recruitmentPost.title::containsIgnoreCase)
}

data class RecruitmentPostApplicationRow(
    val applicationId: Long,
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val lastClickedAt: LocalDateTime,
    val authorUserId: Long,
)
