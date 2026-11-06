package com.ogonggo.core.advertisercontract.implement

import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.error.AdvertiserContractErrorCode
import com.ogonggo.core.advertisercontract.persistence.AdvertiserContractJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class AdvertiserContractReader internal constructor(
    private val advertiserContractRepository: AdvertiserContractJpaRepository,
) {

    fun read(contractId: Long): AdvertiserContract =
        advertiserContractRepository.findByIdAndDeletedAtIsNull(contractId)
            ?: throw EntityNotFoundException(AdvertiserContractErrorCode.ADVERTISER_CONTRACT_NOT_FOUND)

    /** 광고주 하나의 계약이다. 시작이 늦은 것이 먼저이고 같으면 나중에 등록한 것이 먼저다. */
    fun readAllByAdvertiser(advertiserId: Long): List<AdvertiserContract> =
        advertiserContractRepository.findAllByAdvertiserIdAndDeletedAtIsNullOrderByStartAtDescIdDesc(advertiserId)

    /** 광고주마다 계약을 묶는다. 계약이 없는 광고주는 결과에 없다. 순서는 정하지 않는다. */
    fun readAllByAdvertisers(advertiserIds: Collection<Long>): Map<Long, List<AdvertiserContract>> =
        if (advertiserIds.isEmpty()) {
            emptyMap()
        } else {
            advertiserContractRepository.findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds).groupBy { it.advertiserId }
        }

    /** 수정할 계약을 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(contractId: Long): AdvertiserContract =
        advertiserContractRepository.findForUpdate(contractId)
            ?: throw EntityNotFoundException(AdvertiserContractErrorCode.ADVERTISER_CONTRACT_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 계약도 잠가 찾는다. */
    fun readForDelete(contractId: Long): AdvertiserContract =
        advertiserContractRepository.findIncludingDeletedForUpdate(contractId)
            ?: throw EntityNotFoundException(AdvertiserContractErrorCode.ADVERTISER_CONTRACT_NOT_FOUND)

    /** 식별자로 계약을 읽는다. 없거나 지운 계약은 결과에서 빠진다. */
    fun readAllByIds(contractIds: Collection<Long>): List<AdvertiserContract> =
        if (contractIds.isEmpty()) {
            emptyList()
        } else {
            advertiserContractRepository.findAllById(contractIds.toSet()).filter { it.deletedAt == null }
        }
}
