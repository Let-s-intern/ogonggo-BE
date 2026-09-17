package com.ogonggo.core.job.persistence

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.domain.QJob.job
import com.ogonggo.core.job.domain.QJobBookmark.jobBookmark
import com.ogonggo.core.job.domain.QJobMetric.jobMetric
import com.ogonggo.core.review.domain.ContentSource
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
    ): Page<Job> = findPage(publishedPredicates(condition), sortType, pageable)

    /**
     * 북마크한 공고 중 게시된 것만 최근 북마크 순으로 읽는다. 선택 필터는 공개 목록과 같다.
     * 공고와 북마크는 연관관계가 없으므로 명시적으로 조인한다.
     */
    fun findBookmarkedPublishedPage(
        userId: Long,
        condition: JobSearchCondition,
        pageable: Pageable,
    ): Page<Job> {
        val predicates = arrayOf(
            jobBookmark.userId.eq(userId),
            jobBookmark.deletedAt.isNull,
            *publishedPredicates(condition),
        )
        val content = queryFactory.select(job)
            .from(job)
            .join(jobBookmark).on(jobBookmark.jobId.eq(job.id))
            .where(*predicates)
            .orderBy(jobBookmark.updatedAt.desc(), jobBookmark.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(job.count())
            .from(job)
            .join(jobBookmark).on(jobBookmark.jobId.eq(job.id))
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    /**
     * 관리 목록은 게시 상태를 고정하지 않으므로 게시 인덱스를 타지 못하고 식별자 역순으로 훑는다.
     * 관리자만 쓰는 목록이라 사용자 목록처럼 인덱스를 필터마다 두지 않는다.
     */
    fun findManagementPage(
        condition: JobManagementSearchCondition,
        sortType: JobSortType,
        now: LocalDateTime,
        pageable: Pageable,
    ): Page<Job> = findPage(managementPredicates(condition, now), sortType, pageable)

    private fun findPage(predicates: Array<Predicate?>, sortType: JobSortType, pageable: Pageable): Page<Job> {
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
        recruiting(now),
    )

    /** 게시 상태와 삭제 여부는 클라이언트가 고를 수 없는 고정 조건이므로 항상 앞에 둔다. */
    private fun publishedPredicates(condition: JobSearchCondition): Array<Predicate?> = arrayOf(
        job.publicationStatus.eq(JobPublicationStatus.PUBLISHED),
        job.deletedAt.isNull,
        employmentTypeEq(condition.employmentType),
        experienceTypeEq(condition.experienceType),
        jobFieldEq(condition.jobField),
        jobRoleEq(condition.jobRole),
        keywordContains(condition.keyword),
    )

    private fun managementPredicates(condition: JobManagementSearchCondition, now: LocalDateTime): Array<Predicate?> =
        arrayOf(
            job.deletedAt.isNull,
            publishedEq(condition.published),
            sourceEq(condition.source),
            condition.reviewStatus?.let(job.reviewStatus::eq),
            recruitmentStatusEq(condition.recruitmentStatus, now),
            keywordContains(condition.keyword),
        )

    private fun employmentTypeEq(employmentType: EmploymentType?): BooleanExpression? =
        employmentType?.let(job.employmentType::eq)

    private fun experienceTypeEq(experienceType: ExperienceType?): BooleanExpression? =
        experienceType?.let(job.experienceType::eq)

    private fun jobFieldEq(jobField: String?): BooleanExpression? =
        jobField?.takeIf(String::isNotBlank)?.let(job.jobField::eq)

    private fun jobRoleEq(jobRole: String?): BooleanExpression? =
        jobRole?.takeIf(String::isNotBlank)?.let(job.jobRole::eq)

    private fun publishedEq(published: Boolean?): BooleanExpression? = when (published) {
        null -> null
        true -> job.publicationStatus.eq(JobPublicationStatus.PUBLISHED)
        false -> job.publicationStatus.ne(JobPublicationStatus.PUBLISHED)
    }

    /** 등록 경로는 저장하지 않으므로 소유자 유무로 거른다. */
    private fun sourceEq(source: ContentSource?): BooleanExpression? = when (source) {
        null -> null
        ContentSource.CRAWLER -> job.ownerUserId.isNull
        ContentSource.COMPANY -> job.ownerUserId.isNotNull
    }

    /** `Job.recruitmentStatus`와 같은 경계를 쓴다. 종료 일시와 같은 시각까지는 모집 중이다. */
    private fun recruitmentStatusEq(status: JobRecruitmentStatus?, now: LocalDateTime): BooleanExpression? =
        when (status) {
            null -> null
            JobRecruitmentStatus.RECRUITING -> recruiting(now)
            JobRecruitmentStatus.CLOSED -> job.closedAt.isNotNull.or(job.recruitmentEndAt.lt(now))
        }

    private fun recruiting(now: LocalDateTime): BooleanExpression =
        job.closedAt.isNull.and(job.recruitmentEndAt.isNull.or(job.recruitmentEndAt.goe(now)))

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
