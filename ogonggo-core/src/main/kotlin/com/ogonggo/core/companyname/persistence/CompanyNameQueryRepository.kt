package com.ogonggo.core.companyname.persistence

import com.ogonggo.core.bootcamp.domain.QBootcamp.bootcamp
import com.ogonggo.core.job.domain.QJob.job
import com.ogonggo.core.user.domain.QCompanyProfile.companyProfile
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.EntityPathBase
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * 회사명 검색 대상인 세 칼럼에서 검색어를 포함하는 이름을 테이블마다 따로 찾는다.
 *
 * 테이블마다 [CompanyNameReader]와 같은 순서로 앞에서 [limit]개만 가져온다. 합친 뒤의 앞 [limit]개는
 * 반드시 어느 한 테이블의 앞 [limit]개 안에 있으므로 이것으로 충분하다.
 */
@Repository
internal class CompanyNameQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 삭제되지 않은 채용공고 전부다. 마감·비노출 공고도 포함한다. */
    fun findJobCompanyNames(keyword: String, limit: Int): List<String> =
        find(job, job.companyName.trim(), job.deletedAt.isNull, keyword, limit)

    /** 삭제되지 않은 부트캠프 전부다. 마감 부트캠프도 포함한다. */
    fun findBootcampCompanyNames(keyword: String, limit: Int): List<String> =
        find(bootcamp, bootcamp.companyName.trim(), bootcamp.deletedAt.isNull, keyword, limit)

    /** 기업 회원의 기관명 전부다. */
    fun findCompanyProfileOrganizationNames(keyword: String, limit: Int): List<String> =
        find(companyProfile, companyProfile.organizationName.trim(), null, keyword, limit)

    private fun find(
        from: EntityPathBase<*>,
        name: StringExpression,
        condition: Predicate?,
        keyword: String,
        limit: Int,
    ): List<String> {
        val prefixFirst = CaseBuilder().`when`(name.startsWithIgnoreCase(keyword)).then(0).otherwise(1)
        // distinct는 select에 없는 식으로 정렬할 수 없어(H2·MySQL) 이름으로 묶고 정렬 식은 집계로 감싼다.
        return queryFactory.select(name)
            .from(from)
            .where(condition, name.containsIgnoreCase(keyword))
            .groupBy(name)
            .orderBy(prefixFirst.min().asc(), name.length().min().asc(), name.asc())
            .limit(limit.toLong())
            .fetch()
    }
}
