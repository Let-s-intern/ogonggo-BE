package com.ogonggo.core.job.persistence

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.domain.QJob.job
import com.ogonggo.core.job.domain.QJobMetric.jobMetric
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 목록 조회의 필터는 선택적이고 앞으로 계속 늘어나므로 정적 JPQL 대신 동적 쿼리로 조립한다.
 * 필터 하나가 조건 함수 하나에 대응하고 null을 반환하면 where에서 무시되므로,
 * 필터를 추가할 때 조회 계약의 시그니처를 바꾸지 않는다.
 */
@Repository
internal class JobQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublishedPage(
        condition: JobSearchCondition,
        sortType: JobSortType,
        pageable: Pageable,
    ): Page<Job> {
        val predicates = publishedPredicates(condition)

        val content = sorted(queryFactory.selectFrom(job).where(*predicates), sortType)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(job.count())
            .from(job)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    /**
     * 조회 수 인덱스를 높은 순으로 읽으며 공고를 기본 키로 붙이고, 모집 중인 공고가 limit건 모이면 멈춘다.
     * 공고에서 출발하면 게시 공고 전체를 읽어 정렬해야 하므로 지표에서 출발한다.
     * 상위 지표만 먼저 고르면 그중 마감된 공고가 빠져 limit건을 채우지 못하므로 한 쿼리에서 거른다.
     * 지표 행은 첫 조회 시점에 생기므로 한 번도 조회되지 않은 공고는 대상이 아니다.
     */
    fun findPopularRecruiting(limit: Int, now: LocalDateTime): List<Job> =
        queryFactory.select(job)
            .from(jobMetric)
            .join(job).on(job.id.eq(jobMetric.jobId))
            .where(*recruitingPredicates(now))
            .orderBy(jobMetric.viewCount.desc(), jobMetric.jobId.desc())
            .limit(limit.toLong())
            .fetch()

    /**
     * 직무·산업이 주어진 값 중 하나와 정확히 같은 모집 중 공고를 조회수순으로 읽는다.
     * 값 목록이 비면 그 조건을 걸지 않으므로, 둘 다 비었는지는 호출하는 쪽이 막는다.
     * 조건으로 먼저 좁힌 뒤 정렬하므로 인기 공고와 달리 한 번도 조회되지 않은 공고도 0으로 포함한다.
     */
    fun findRecruitingMatched(
        jobRoles: Collection<String>,
        industries: Collection<String>,
        excludedJobIds: Collection<Long>,
        limit: Int,
        now: LocalDateTime,
    ): List<Job> =
        queryFactory.selectFrom(job)
            .leftJoin(jobMetric).on(jobMetric.jobId.eq(job.id))
            .where(
                *recruitingPredicates(now),
                jobRoles.takeIf { it.isNotEmpty() }?.let { job.jobRole.`in`(it) },
                industries.takeIf { it.isNotEmpty() }?.let { job.industry.`in`(it) },
                excludedJobIds.takeIf { it.isNotEmpty() }?.let { job.id.notIn(it) },
            )
            .orderBy(VIEW_COUNT_OR_ZERO.desc(), job.id.desc())
            .limit(limit.toLong())
            .fetch()

    /** 마감 처리됐거나 모집 종료 일시가 지난 공고는 지원할 수 없으므로 추천 목록에서 뺀다. */
    private fun recruitingPredicates(now: LocalDateTime): Array<Predicate> = arrayOf(
        job.publicationStatus.eq(JobPublicationStatus.PUBLISHED),
        job.deletedAt.isNull,
        job.closedAt.isNull,
        job.recruitmentEndAt.isNull.or(job.recruitmentEndAt.goe(now)),
    )

    /** 게시 상태와 삭제 여부는 클라이언트가 고를 수 없는 고정 조건이므로 항상 앞에 둔다. */
    private fun publishedPredicates(condition: JobSearchCondition): Array<Predicate?> = arrayOf(
        job.publicationStatus.eq(JobPublicationStatus.PUBLISHED),
        job.deletedAt.isNull,
        employmentTypeEq(condition.employmentType),
        experienceTypeEq(condition.experienceType),
        keywordContains(condition.keyword),
    )

    private fun employmentTypeEq(employmentType: EmploymentType?): BooleanExpression? =
        employmentType?.let(job.employmentType::eq)

    private fun experienceTypeEq(experienceType: ExperienceType?): BooleanExpression? =
        experienceType?.let(job.experienceType::eq)

    /**
     * 검색어는 인덱스로 좁힐 수 없어 다른 조건으로 고른 행을 차례로 확인한다.
     * 대소문자를 가리지 않아 영문 직무명을 어떻게 입력해도 같은 결과를 준다.
     * 검색어의 와일드카드는 QueryDSL이 이스케이프하므로 사용자가 전체 조회를 유발할 수 없다.
     */
    private fun keywordContains(keyword: String?): BooleanExpression? {
        if (keyword.isNullOrBlank()) {
            return null
        }
        return job.title.containsIgnoreCase(keyword)
            .or(job.companyName.containsIgnoreCase(keyword))
    }

    /**
     * 지표 조인은 조회수순에만 필요하므로 그 정렬에서만 건다.
     * 최신순까지 조인하면 정렬을 인덱스로 해결할 수 없다.
     * 지표 행은 첫 지표 발생 시점에 생기므로 아직 없는 공고는 0으로 본다.
     * 조회 수가 같을 때 페이지가 흔들리지 않도록 식별자로 순서를 확정한다.
     */
    private fun sorted(query: JPAQuery<Job>, sortType: JobSortType): JPAQuery<Job> = when (sortType) {
        JobSortType.LATEST -> query.orderBy(job.id.desc())

        JobSortType.VIEW_COUNT -> query
            .leftJoin(jobMetric).on(jobMetric.jobId.eq(job.id))
            .orderBy(VIEW_COUNT_OR_ZERO.desc(), job.id.desc())
    }

    companion object {
        /** 지표 행이 없는 공고를 조회 수 0으로 취급한다. */
        private val VIEW_COUNT_OR_ZERO = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "coalesce({0}, 0)",
            jobMetric.viewCount,
        )
    }
}
