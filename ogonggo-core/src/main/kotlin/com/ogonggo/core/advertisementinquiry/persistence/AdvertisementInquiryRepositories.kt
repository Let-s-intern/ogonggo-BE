package com.ogonggo.core.advertisementinquiry.persistence

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiry
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.domain.QAdvertisementInquiry.advertisementInquiry
import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

internal interface AdvertisementInquiryJpaRepository : JpaRepository<AdvertisementInquiry, Long> {
    /** 상태와 메모를 동시에 바꿔도 한쪽 변경이 다른 쪽의 낡은 값으로 덮이지 않도록 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AdvertisementInquiry a where a.id = :id")
    fun findForUpdate(id: Long): AdvertisementInquiry?
}

/** 관리자 콘솔 광고 문의 목록이다. 접수 순서와 식별자 순서가 같으므로 최신순은 `id DESC`다. */
@Repository
internal class AdvertisementInquiryQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPage(condition: AdvertisementInquirySearchCondition, pageable: Pageable): Page<AdvertisementInquiry> {
        val predicates = arrayOf(
            condition.inquiryType?.let(advertisementInquiry.inquiryType::eq),
            condition.campaignCode?.takeIf { it.isNotBlank() }?.let(advertisementInquiry.campaignCode::eq),
            condition.statuses.takeIf { it.isNotEmpty() }?.let { advertisementInquiry.status.`in`(it) },
            condition.memberType?.let(advertisementInquiry.memberType::eq),
            condition.inflowChannel?.let(advertisementInquiry.inflowChannel::eq),
            condition.companyNameSource?.let(advertisementInquiry.companyNameSource::eq),
            condition.createdFrom?.let { advertisementInquiry.createdAt.goe(it.atStartOfDay()) },
            condition.createdTo?.let { advertisementInquiry.createdAt.lt(it.plusDays(1).atStartOfDay()) },
        )

        val content = queryFactory.selectFrom(advertisementInquiry)
            .where(*predicates)
            .orderBy(advertisementInquiry.id.desc())
            .paged(pageable)
            .fetch()

        val countQuery = queryFactory.select(advertisementInquiry.count())
            .from(advertisementInquiry)
            .where(*predicates)

        return pageOf(content, pageable, countQuery)
    }

    /** `[from, to)`에 들어온 문의 중 [excludedStatus]가 아닌 문의 수다. 문의 유형을 주지 않으면 가리지 않는다. */
    fun countCreatedBetween(
        inquiryType: AdvertisementInquiryType?,
        excludedStatus: AdvertisementInquiryStatus,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long =
        queryFactory.select(advertisementInquiry.count())
            .from(advertisementInquiry)
            .where(
                inquiryType?.let(advertisementInquiry.inquiryType::eq),
                advertisementInquiry.status.ne(excludedStatus),
                advertisementInquiry.createdAt.goe(from),
                advertisementInquiry.createdAt.lt(to),
            )
            .fetchOne() ?: 0L
}
