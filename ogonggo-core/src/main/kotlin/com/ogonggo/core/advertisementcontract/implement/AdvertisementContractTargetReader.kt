package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractTargetDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractTargetJpaRepository
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.implement.JobReader
import org.springframework.stereotype.Component

@Component
class AdvertisementContractTargetReader internal constructor(
    private val advertisementContractTargetRepository: AdvertisementContractTargetJpaRepository,
    private val jobReader: JobReader,
    private val bootcampReader: BootcampReader,
) {

    /** 계약의 리포트 대상이다. 비즈니스 공고 계약의 공고가 맨 앞이고, 그 뒤는 연결한 순서다. */
    fun readTargets(contract: AdvertisementContract): List<AdvertisementContractTargetDto> {
        val fixed = fixedTargetOf(contract)
        val linked = advertisementContractTargetRepository
            .findAllByContractIdAndDeletedAtIsNullOrderByIdAsc(checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." })
            .map { it.targetType to it.targetId }
            .filter { it != fixed }
        return (listOfNotNull(fixed) + linked).map { (type, id) -> describe(contract, type, id, fixed = (type to id) == fixed) }
    }

    /** 비즈니스 공고 계약이 가리키는 공고다. 신청 폼 계약이면 null이다. */
    fun fixedTargetOf(contract: AdvertisementContract): Pair<AdvertisementContractPromotionTarget, Long>? =
        if (contract.source == AdvertisementContractSource.COMPANY_POSTING) {
            contract.targetId?.let { contract.promotionTarget to it }
        } else {
            null
        }

    /** 지금 지워지지 않은 공고·부트캠프인지 본다. 없으면 JOB_NOT_FOUND·BOOTCAMP_NOT_FOUND(404)다. */
    fun checkExists(targetType: AdvertisementContractPromotionTarget, targetId: Long) {
        when (targetType) {
            AdvertisementContractPromotionTarget.JOB -> jobReader.read(targetId)
            AdvertisementContractPromotionTarget.BOOTCAMP -> bootcampReader.read(targetId)
        }
    }

    private fun describe(
        contract: AdvertisementContract,
        targetType: AdvertisementContractPromotionTarget,
        targetId: Long,
        fixed: Boolean,
    ): AdvertisementContractTargetDto = try {
        when (targetType) {
            AdvertisementContractPromotionTarget.JOB -> jobReader.readIncludingDeleted(targetId).let {
                AdvertisementContractTargetDto(targetType, targetId, it.title, it.companyName, it.coverImageUrl, it.logoUrl, fixed)
            }
            AdvertisementContractPromotionTarget.BOOTCAMP -> bootcampReader.readIncludingDeleted(targetId).let {
                AdvertisementContractTargetDto(targetType, targetId, it.title, it.companyName, it.representativeImageUrl, it.logoUrl, fixed)
            }
        }
    } catch (exception: EntityNotFoundException) {
        // 공고 행이 아예 없으면(다른 환경에서 옮긴 계약 등) 계약에 남은 이름으로 보인다.
        AdvertisementContractTargetDto(targetType, targetId, contract.recruitmentTitle, contract.companyName, null, null, fixed)
    }
}
