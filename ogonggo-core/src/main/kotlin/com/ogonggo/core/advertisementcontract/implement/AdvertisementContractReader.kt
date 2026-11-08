package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractPageDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractProductDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractJpaRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractProductJpaRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractQueryRepository
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.paging.validatePageRequest
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class AdvertisementContractReader internal constructor(
    private val advertisementContractRepository: AdvertisementContractJpaRepository,
    private val advertisementContractQueryRepository: AdvertisementContractQueryRepository,
    private val advertisementContractProductRepository: AdvertisementContractProductJpaRepository,
    private val advertisementProductReader: AdvertisementProductReader,
) {

    fun read(contractId: Long): AdvertisementContract =
        advertisementContractRepository.findById(contractId).orElse(null)
            ?: throw EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND)

    /** 상태·결제 금액을 바꾸거나 상품을 붙일 계약을 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(contractId: Long): AdvertisementContract =
        advertisementContractRepository.findForUpdate(contractId)
            ?: throw EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND)

    /** 비즈니스 공고의 계약이다. 이관 전에 등록된 공고는 계약이 없을 수 있다. */
    fun readOfCompanyPosting(promotionTarget: AdvertisementContractPromotionTarget, targetId: Long): AdvertisementContract? =
        advertisementContractRepository.findBySourceAndPromotionTargetAndTargetId(
            AdvertisementContractSource.COMPANY_POSTING,
            promotionTarget,
            targetId,
        )

    /** 노출 목록의 광고주처럼 여러 계약을 한 번에 붙일 때 쓴다. */
    fun readAllByIds(contractIds: Collection<Long>): Map<Long, AdvertisementContract> =
        if (contractIds.isEmpty()) emptyMap()
        else advertisementContractRepository.findAllByIdIn(contractIds.toSet()).associateBy { checkNotNull(it.id) }

    /** 최근에 생긴 계약부터 준다. */
    fun readPage(condition: AdvertisementContractSearchCondition, page: Int, size: Int): AdvertisementContractPageDto {
        validatePageRequest(page, size)
        val result = advertisementContractQueryRepository.findPage(condition, PageRequest.of(page, size))
        advertisementContractQueryRepository.initializeCategories(result.content.map { checkNotNull(it.id) })
        return AdvertisementContractPageDto(
            advertisementContracts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /** 계약에 붙인 상품을 붙인 순서로 준다. 계약에서 뺀 상품은 주지 않는다. */
    fun readProducts(contractId: Long): List<AdvertisementContractProductDto> {
        val contractProducts = advertisementContractProductRepository.findAllByContractIdAndDeletedAtIsNullOrderByIdAsc(contractId)
        val products = advertisementProductReader.readAllByIds(contractProducts.map { it.productId })
        return contractProducts.map {
            AdvertisementContractProductDto.of(
                contractProductId = checkNotNull(it.id),
                product = checkNotNull(products[it.productId]) { "계약에 붙인 광고 상품이 없습니다." },
                addedAt = it.createdAt,
            )
        }
    }
}
