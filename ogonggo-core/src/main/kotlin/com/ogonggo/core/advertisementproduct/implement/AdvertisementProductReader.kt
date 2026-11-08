package com.ogonggo.core.advertisementproduct.implement

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class AdvertisementProductReader internal constructor(
    private val advertisementProductRepository: AdvertisementProductJpaRepository,
) {

    /** 지우지 않은 상품을 읽는다. 지운 상품은 고치거나 계약·노출에 새로 붙일 수 없어 없는 것으로 본다. */
    fun read(productId: Long): AdvertisementProduct =
        advertisementProductRepository.findByIdAndDeletedAtIsNull(productId)
            ?: throw EntityNotFoundException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND)

    /** 상품 삭제·연결이나 같은 상품 자리의 노출 등록을 직렬화할 때 잠가 읽는다. 호출한 서비스 트랜잭션 안에서 부른다. */
    fun readForUpdate(productId: Long): AdvertisementProduct =
        advertisementProductRepository.findForUpdate(productId)
            ?: throw EntityNotFoundException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND)

    /** 기존 노출 순서 변경을 직렬화하기 위한 잠금이다. 지운 상품의 기존 노출도 관리할 수 있어 삭제 여부로 거르지 않는다. */
    fun readForOrderUpdate(productId: Long): AdvertisementProduct =
        advertisementProductRepository.findForOrderUpdate(productId)
            ?: throw EntityNotFoundException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND)

    /** 지우지 않은 상품을 카테고리 선언 순서, 등록 순서로 준다. 신청 폼과 어드민 상품 목록이 쓴다. */
    fun readAll(): List<AdvertisementProduct> =
        advertisementProductRepository.findAllByDeletedAtIsNull()
            .sortedWith(compareBy<AdvertisementProduct>({ it.category.ordinal }, { it.id }))

    /** 이미 맺은 계약·노출이 가리키는 상품은 지웠어도 이름을 보여 줘야 하므로 지운 상품도 준다. */
    fun readAllByIds(productIds: Collection<Long>): Map<Long, AdvertisementProduct> =
        if (productIds.isEmpty()) emptyMap()
        else advertisementProductRepository.findAllByIdIn(productIds.toSet()).associateBy { checkNotNull(it.id) }

    /** 노출 자리의 지우지 않은 상품이다. 보통 하나지만 막지 않는다. */
    fun readByPosition(position: AdvertisementProductPosition): List<AdvertisementProduct> =
        advertisementProductRepository.findAllByPositionAndDeletedAtIsNull(position)
}
