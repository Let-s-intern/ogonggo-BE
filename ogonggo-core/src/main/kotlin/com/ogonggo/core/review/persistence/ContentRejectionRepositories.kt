package com.ogonggo.core.review.persistence

import com.ogonggo.core.bootcamp.domain.QBootcamp.bootcamp
import com.ogonggo.core.job.domain.QJob.job
import com.ogonggo.core.review.domain.ContentRejection
import com.ogonggo.core.review.domain.QContentRejection.contentRejection
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.implement.dto.ContentRejectionDto
import com.querydsl.core.Tuple
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

internal interface ContentRejectionJpaRepository : JpaRepository<ContentRejection, Long> {
    /** 콘텐츠마다 한 행뿐이므로 풀린 기록까지 찾아 다시 반려할 때 되살린다. */
    fun findByContentTypeAndContentId(contentType: ReviewContentType, contentId: Long): ContentRejection?

    fun findByContentTypeAndContentIdAndDeletedAtIsNull(
        contentType: ReviewContentType,
        contentId: Long,
    ): ContentRejection?
}

/**
 * 반려 기록에는 제목과 회사명이 없으므로 종류에 맞는 콘텐츠를 붙여 읽는다.
 * 제목을 복사해 두면 콘텐츠를 고칠 때마다 함께 맞춰야 하므로 조회 시점에 조인한다.
 * 콘텐츠는 소프트 삭제되므로 삭제된 뒤에도 제목을 읽을 수 있다.
 */
@Repository
internal class ContentRejectionQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findActivePage(
        contentType: ReviewContentType?,
        keyword: String?,
        pageable: Pageable,
    ): Page<ContentRejectionDto> {
        val predicates = arrayOf(
            contentRejection.deletedAt.isNull,
            contentType?.let(contentRejection.contentType::eq),
            keywordContains(keyword),
        )

        val content = joinedQuery()
            .where(*predicates)
            .orderBy(contentRejection.rejectedAt.desc(), contentRejection.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map(::toDto)

        val total = queryFactory.select(contentRejection.count())
            .from(contentRejection)
            .leftJoin(job).on(*jobJoinConditions())
            .leftJoin(bootcamp).on(*bootcampJoinConditions())
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    fun findActive(contentType: ReviewContentType, contentId: Long): ContentRejectionDto? =
        joinedQuery()
            .where(
                contentRejection.deletedAt.isNull,
                contentRejection.contentType.eq(contentType),
                contentRejection.contentId.eq(contentId),
            )
            .fetchOne()
            ?.let(::toDto)

    private fun joinedQuery(): JPAQuery<Tuple> =
        queryFactory.select(
            contentRejection,
            job.title,
            job.companyName,
            job.deletedAt,
            bootcamp.title,
            bootcamp.companyName,
            bootcamp.deletedAt,
        )
            .from(contentRejection)
            .leftJoin(job).on(*jobJoinConditions())
            .leftJoin(bootcamp).on(*bootcampJoinConditions())

    private fun jobJoinConditions(): Array<Predicate> = arrayOf(
        contentRejection.contentType.eq(ReviewContentType.JOB),
        job.id.eq(contentRejection.contentId),
    )

    private fun bootcampJoinConditions(): Array<Predicate> = arrayOf(
        contentRejection.contentType.eq(ReviewContentType.BOOTCAMP),
        bootcamp.id.eq(contentRejection.contentId),
    )

    /** 같은 이유로 돌려보낸 건을 모아 보도록 사유까지 찾는다. */
    private fun keywordContains(keyword: String?): BooleanExpression? {
        if (keyword.isNullOrBlank()) {
            return null
        }
        return contentRejection.reason.containsIgnoreCase(keyword)
            .or(job.title.containsIgnoreCase(keyword))
            .or(job.companyName.containsIgnoreCase(keyword))
            .or(bootcamp.title.containsIgnoreCase(keyword))
            .or(bootcamp.companyName.containsIgnoreCase(keyword))
    }

    private fun toDto(tuple: Tuple): ContentRejectionDto {
        val rejection = checkNotNull(tuple.get(contentRejection)) { "반려 기록이 없습니다." }
        val isJob = rejection.contentType == ReviewContentType.JOB
        val title = if (isJob) tuple.get(job.title) else tuple.get(bootcamp.title)
        val companyName = if (isJob) tuple.get(job.companyName) else tuple.get(bootcamp.companyName)
        val deletedAt = if (isJob) tuple.get(job.deletedAt) else tuple.get(bootcamp.deletedAt)
        return ContentRejectionDto(
            contentType = rejection.contentType,
            contentId = rejection.contentId,
            title = title.orEmpty(),
            companyName = companyName.orEmpty(),
            reason = rejection.reason,
            rejectedAt = rejection.rejectedAt,
            reasonUpdatedAt = rejection.reasonUpdatedAt,
            // 콘텐츠는 소프트 삭제되므로 조인이 비는 경우는 없지만, 비면 남아 있지 않은 것으로 본다.
            contentExists = title != null && deletedAt == null,
        )
    }
}
