package com.ogonggo.core.bootcamp.persistence

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.QBootcamp.bootcamp
import com.ogonggo.core.bootcamp.domain.QBootcampBookmark.bootcampBookmark
import com.ogonggo.core.bootcamp.domain.QBootcampMetric.bootcampMetric
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.review.domain.ContentSource
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 목록 조회의 필터는 선택적이고 앞으로 계속 늘어나므로 정적 JPQL 대신 동적 쿼리로 조립한다.
 * 필터 하나가 조건 함수 하나에 대응하고 null을 반환하면 where에서 무시되므로,
 * 필터를 추가할 때 조회 계약의 시그니처를 바꾸지 않는다.
 */
@Repository
internal class BootcampQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublicPage(
        condition: BootcampSearchCondition,
        sortType: BootcampSortType,
        publicStatuses: Collection<BootcampStatus>,
        now: LocalDateTime,
        pageable: Pageable,
    ): Page<Bootcamp> = findPage(publicPredicates(condition, publicStatuses, now), sortType, pageable)

    /**
     * 북마크한 부트캠프 중 지금 공개된 것만 최근 북마크 순으로 읽는다. 선택 필터는 공개 목록과 같다.
     * 부트캠프와 북마크는 연관관계가 없으므로 명시적으로 조인한다.
     */
    fun findBookmarkedPublicPage(
        userId: Long,
        condition: BootcampSearchCondition,
        publicStatuses: Collection<BootcampStatus>,
        now: LocalDateTime,
        pageable: Pageable,
    ): Page<Bootcamp> {
        val predicates = arrayOf(
            bootcampBookmark.userId.eq(userId),
            bootcampBookmark.deletedAt.isNull,
            *publicPredicates(condition, publicStatuses, now),
        )
        val content = queryFactory.select(bootcamp)
            .from(bootcamp)
            .join(bootcampBookmark).on(bootcampBookmark.bootcampId.eq(bootcamp.id))
            .where(*predicates)
            .orderBy(bootcampBookmark.updatedAt.desc(), bootcampBookmark.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(bootcamp.count())
            .from(bootcamp)
            .join(bootcampBookmark).on(bootcampBookmark.bootcampId.eq(bootcamp.id))
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    /** 관리 목록은 공개 조건을 고정하지 않는다. 관리자만 쓰는 목록이라 필터용 인덱스를 따로 두지 않는다. */
    fun findManagementPage(
        condition: BootcampManagementSearchCondition,
        sortType: BootcampSortType,
        pageable: Pageable,
    ): Page<Bootcamp> = findPage(managementPredicates(condition), sortType, pageable)

    private fun findPage(predicates: Array<Predicate?>, sortType: BootcampSortType, pageable: Pageable): Page<Bootcamp> {
        val content = sorted(queryFactory.selectFrom(bootcamp).where(*predicates), sortType)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(bootcamp.count())
            .from(bootcamp)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    /** 게시 상태와 모집 상태, 공개 기간, 삭제 여부는 클라이언트가 고를 수 없는 고정 조건이므로 항상 앞에 둔다. */
    private fun publicPredicates(
        condition: BootcampSearchCondition,
        publicStatuses: Collection<BootcampStatus>,
        now: LocalDateTime,
    ): Array<Predicate?> = arrayOf(
        bootcamp.publicationStatus.eq(BootcampPublicationStatus.PUBLISHED),
        bootcamp.status.`in`(publicStatuses),
        bootcamp.deletedAt.isNull,
        bootcamp.publicationStartAt.isNull.or(bootcamp.publicationStartAt.loe(now)),
        bootcamp.publicationEndAt.isNull.or(bootcamp.publicationEndAt.goe(now)),
        tuitionTypeEq(condition.tuitionType),
        statusEq(condition.status),
        keywordContains(condition.keyword),
    )

    private fun managementPredicates(condition: BootcampManagementSearchCondition): Array<Predicate?> = arrayOf(
        bootcamp.deletedAt.isNull,
        publishedEq(condition.published),
        sourceEq(condition.source),
        condition.reviewStatus?.let(bootcamp.reviewStatus::eq),
        statusEq(condition.status),
        keywordContains(condition.keyword),
    )

    private fun tuitionTypeEq(tuitionType: TuitionType?): BooleanExpression? =
        tuitionType?.let(bootcamp.tuitionType::eq)

    /** 공개 목록은 고정 조건이 이미 공개 상태로 좁혀 두므로 그 안에서 한 상태만 더 고른다. */
    private fun statusEq(status: BootcampStatus?): BooleanExpression? =
        status?.let(bootcamp.status::eq)

    private fun publishedEq(published: Boolean?): BooleanExpression? = when (published) {
        null -> null
        true -> bootcamp.publicationStatus.eq(BootcampPublicationStatus.PUBLISHED)
        false -> bootcamp.publicationStatus.ne(BootcampPublicationStatus.PUBLISHED)
    }

    /** 등록 경로는 저장하지 않으므로 소유자 유무로 거른다. */
    private fun sourceEq(source: ContentSource?): BooleanExpression? = when (source) {
        null -> null
        ContentSource.CRAWLER -> bootcamp.ownerUserId.isNull
        ContentSource.COMPANY -> bootcamp.ownerUserId.isNotNull
    }

    /**
     * 검색어는 인덱스로 좁힐 수 없어 다른 조건으로 고른 행을 차례로 확인한다.
     * 대소문자를 가리지 않아 영문 프로그램명을 어떻게 입력해도 같은 결과를 준다.
     * 검색어의 와일드카드는 QueryDSL이 이스케이프하므로 사용자가 전체 조회를 유발할 수 없다.
     */
    private fun keywordContains(keyword: String?): BooleanExpression? {
        if (keyword.isNullOrBlank()) {
            return null
        }
        return bootcamp.title.containsIgnoreCase(keyword)
            .or(bootcamp.companyName.containsIgnoreCase(keyword))
    }

    /**
     * 지표 조인은 조회수순에만 필요하므로 그 정렬에서만 건다.
     * 최신순까지 조인하면 정렬을 인덱스로 해결할 수 없다.
     * 지표 행은 첫 지표 발생 시점에 생기므로 아직 없는 부트캠프는 0으로 본다.
     * 조회 수가 같을 때 페이지가 흔들리지 않도록 식별자로 순서를 확정한다.
     */
    private fun sorted(query: JPAQuery<Bootcamp>, sortType: BootcampSortType): JPAQuery<Bootcamp> = when (sortType) {
        BootcampSortType.LATEST -> query.orderBy(bootcamp.id.desc())

        BootcampSortType.VIEW_COUNT -> query
            .leftJoin(bootcampMetric).on(bootcampMetric.bootcampId.eq(bootcamp.id))
            .orderBy(VIEW_COUNT_OR_ZERO.desc(), bootcamp.id.desc())
    }

    companion object {
        /** 지표 행이 없는 부트캠프를 조회 수 0으로 취급한다. */
        private val VIEW_COUNT_OR_ZERO = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "coalesce({0}, 0)",
            bootcampMetric.viewCount,
        )
    }
}
