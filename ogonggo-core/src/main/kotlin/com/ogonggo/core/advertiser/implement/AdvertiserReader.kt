package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.error.AdvertiserErrorCode
import com.ogonggo.core.advertiser.persistence.AdvertiserJpaRepository
import com.ogonggo.core.advertiser.persistence.AdvertiserQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AdvertiserReader internal constructor(
    private val advertiserRepository: AdvertiserJpaRepository,
    private val advertiserQueryRepository: AdvertiserQueryRepository,
) {

    /** 탈퇴한 비즈니스 회원의 광고주도 읽는다. 이미 걸린 광고가 그 광고주를 가리킬 수 있다. */
    fun read(advertiserId: Long): Advertiser =
        advertiserRepository.findByIdOrNull(advertiserId)
            ?: throw EntityNotFoundException(AdvertiserErrorCode.ADVERTISER_NOT_FOUND)

    /** 수정할 광고주를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(advertiserId: Long): Advertiser =
        advertiserRepository.findForUpdate(advertiserId)
            ?: throw EntityNotFoundException(AdvertiserErrorCode.ADVERTISER_NOT_FOUND)

    /** 있는 광고주인지 본다. 없으면 찾을 수 없다고 알린다. */
    fun validateExists(advertiserId: Long) {
        if (!advertiserRepository.existsById(advertiserId)) {
            throw EntityNotFoundException(AdvertiserErrorCode.ADVERTISER_NOT_FOUND)
        }
    }

    /** 식별자마다 광고주를 둔다. 없는 광고주는 결과에 없다. 탈퇴한 비즈니스 회원의 광고주도 읽는다. */
    fun readAllByIds(advertiserIds: Collection<Long>): Map<Long, Advertiser> =
        if (advertiserIds.isEmpty()) emptyMap() else advertiserRepository.findAllById(advertiserIds).associateBy { checkNotNull(it.id) }

    /** 조건에 맞는 광고주 전부다. 탈퇴한 비즈니스 회원의 광고주는 뺀다. */
    fun readAll(condition: AdvertiserSearchCondition): List<Advertiser> = advertiserQueryRepository.findAll(condition)

    /**
     * 공백·대소문자를 빼고 같은 이름의 광고주가 있는지 본다. 회원 광고주와 탈퇴한 회원의 광고주까지 본다.
     * [exceptAdvertiserId]는 수정할 때 자기 자신을 빼려고 넘긴다.
     */
    fun existsSameName(name: String, exceptAdvertiserId: Long? = null): Boolean {
        val nameKey = Advertiser.nameKeyOf(name)
        return advertiserRepository.findAll().any { it.id != exceptAdvertiserId && Advertiser.nameKeyOf(it.name) == nameKey }
    }
}
