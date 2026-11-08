package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractJpaRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractProductJpaRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.error.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertisementContractManager internal constructor(
    private val advertisementContractRepository: AdvertisementContractJpaRepository,
    private val advertisementContractProductRepository: AdvertisementContractProductJpaRepository,
) {

    /** 신청 폼 계약만 바꾼다. 비즈니스 공고 계약은 409다. */
    fun changeStatus(contract: AdvertisementContract, status: AdvertisementContractStatus) {
        contract.changeStatus(status)
        advertisementContractRepository.save(contract)
    }

    fun changePaymentAmount(contract: AdvertisementContract, paymentAmount: Int?) {
        contract.changePaymentAmount(paymentAmount)
        advertisementContractRepository.save(contract)
    }

    /** 운영자가 계약 상세에서 상품을 붙인다. 같은 상품을 여러 번 붙여도 막지 않는다(기간을 나눠 파는 경우). */
    fun addProduct(contract: AdvertisementContract, product: AdvertisementProduct): Long {
        product.checkNotDeleted()
        val saved = advertisementContractProductRepository.save(
            AdvertisementContractProduct(
                contractId = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
                productId = checkNotNull(product.id) { "광고 상품 식별자가 없습니다." },
            ),
        )
        return checkNotNull(saved.id) { "계약 상품 식별자가 없습니다." }
    }

    fun removeProduct(contract: AdvertisementContract, contractProductId: Long, now: LocalDateTime) {
        val contractProduct = advertisementContractProductRepository.findByIdAndContractIdAndDeletedAtIsNull(
            contractProductId,
            checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
        ) ?: throw EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND)
        contractProduct.delete(now)
        advertisementContractProductRepository.save(contractProduct)
    }

    /**
     * 비즈니스 공고의 검수 상태가 바뀌면 그 공고의 계약 상태를 맞춘다. 검수 상태를 바꾸는 쪽이 같은 트랜잭션에서 부른다.
     *
     * 계약이 없으면 건너뛴다. 이관 SQL을 적용하기 전에 등록된 공고가 그렇고, 이관이 나중에 현재 검수 상태로 계약을 만든다.
     */
    fun followReview(promotionTarget: AdvertisementContractPromotionTarget, targetId: Long, reviewStatus: ContentReviewStatus) {
        val contract = advertisementContractRepository.findBySourceAndPromotionTargetAndTargetId(
            AdvertisementContractSource.COMPANY_POSTING,
            promotionTarget,
            targetId,
        )
        if (contract == null) {
            log.warn("비즈니스 공고 계약이 없어 검수 상태를 맞추지 않습니다. target={}, targetId={}", promotionTarget, targetId)
            return
        }
        contract.followReview(reviewStatus)
        advertisementContractRepository.save(contract)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdvertisementContractManager::class.java)
    }
}
