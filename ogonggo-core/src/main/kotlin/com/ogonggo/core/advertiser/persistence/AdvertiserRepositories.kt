package com.ogonggo.core.advertiser.persistence

import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.domain.QAdvertiser.advertiser
import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.UserStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

internal interface AdvertiserJpaRepository : JpaRepository<Advertiser, Long> {
    fun findAllByType(type: AdvertiserType): List<Advertiser>

    /** 광고 문의를 어디까지 광고주로 옮겼는지 본다. */
    @Query("select max(a.adInquiryId) from Advertiser a")
    fun findMaxAdInquiryId(): Long?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Advertiser a where a.id = :advertiserId")
    fun findForUpdate(@Param("advertiserId") advertiserId: Long): Advertiser?
}

/** 관리자 콘솔 광고주 목록이다. 탈퇴한 비즈니스 회원의 광고주는 뺀다. 순서는 계약을 보고 관리자 API가 정한다. */
@Repository
internal class AdvertiserQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findAll(condition: AdvertiserSearchCondition): List<Advertiser> =
        queryFactory.select(advertiser)
            .from(advertiser)
            .leftJoin(user).on(user.id.eq(advertiser.companyMemberUserId))
            .where(
                advertiser.type.eq(AdvertiserType.NON_MEMBER).or(user.status.ne(UserStatus.WITHDRAWN)),
                condition.type?.let(advertiser.type::eq),
                condition.keyword?.takeIf { it.isNotBlank() }?.let {
                    advertiser.name.containsIgnoreCase(it)
                        .or(advertiser.contactName.containsIgnoreCase(it))
                        .or(advertiser.contactEmail.containsIgnoreCase(it))
                },
            )
            .orderBy(advertiser.id.asc())
            .fetch()
}
