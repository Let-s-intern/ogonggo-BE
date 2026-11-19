package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractTarget
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractTargetJpaRepository
import com.ogonggo.core.error.ConflictException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertisementContractTargetManager internal constructor(
    private val advertisementContractTargetRepository: AdvertisementContractTargetJpaRepository,
    private val advertisementContractTargetReader: AdvertisementContractTargetReader,
) {

    /**
     * 리포트 대상 공고·부트캠프를 연결한다. 지워지지 않은 공고만 연결하고(404), 이미 대상이면 409다.
     * 비즈니스 공고 계약의 공고도 이미 대상이다. 연결을 풀었던 대상이면 그 행을 되살린다.
     */
    fun add(contract: AdvertisementContract, targetType: AdvertisementContractPromotionTarget, targetId: Long) {
        advertisementContractTargetReader.checkExists(targetType, targetId)
        if (advertisementContractTargetReader.fixedTargetOf(contract) == (targetType to targetId)) {
            throw ConflictException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_TARGET_DUPLICATED)
        }
        val contractId = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." }
        val existing = advertisementContractTargetRepository.findByContractIdAndTargetTypeAndTargetId(contractId, targetType, targetId)
        when {
            existing == null -> advertisementContractTargetRepository.save(AdvertisementContractTarget(contractId, targetType, targetId))
            existing.deletedAt == null -> throw ConflictException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_TARGET_DUPLICATED)
            else -> {
                existing.restore()
                advertisementContractTargetRepository.save(existing)
            }
        }
    }

    /** 연결을 푼다. 연결하지 않은 대상이어도 그대로 끝난다. 비즈니스 공고 계약의 공고는 뺄 수 없어 409다. */
    fun remove(contract: AdvertisementContract, targetType: AdvertisementContractPromotionTarget, targetId: Long, now: LocalDateTime) {
        if (advertisementContractTargetReader.fixedTargetOf(contract) == (targetType to targetId)) {
            throw ConflictException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_TARGET_FIXED)
        }
        val existing = advertisementContractTargetRepository.findByContractIdAndTargetTypeAndTargetId(
            checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
            targetType,
            targetId,
        ) ?: return
        existing.delete(now)
        advertisementContractTargetRepository.save(existing)
    }
}
