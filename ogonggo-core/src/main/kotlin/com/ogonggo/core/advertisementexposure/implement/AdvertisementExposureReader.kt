package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateTargetDto
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureQueryRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertisementExposureReader internal constructor(
    private val advertisementExposureRepository: AdvertisementExposureJpaRepository,
    private val advertisementExposureQueryRepository: AdvertisementExposureQueryRepository,
    private val advertisementProductReader: AdvertisementProductReader,
) {

    /**
     * 상품이 삭제된 뒤에도 기존 노출을 켜거나 끄거나 이동·삭제할 수 있다. 노출을 영속성 컨텍스트에
     * 올리지 않고 불변 productId만 읽은 뒤 삭제 여부를 가리지 않고 상품 행, 대상 노출 행을 차례로 잠근다.
     */
    fun readForUpdate(exposureId: Long): AdvertisementExposure {
        val productId = readActiveProductId(exposureId)
        advertisementProductReader.readForOrderUpdate(productId)
        return lockExposure(exposureId)
    }

    /**
     * A-13은 삭제된 상품을 수정할 수 없다. 영속성 컨텍스트에 노출을 올리지 않고 불변 productId만 읽은 뒤
     * 활성 상품을 잠근다. 잠금 획득 시점에 상품이 이미 삭제됐다면 ADVERTISEMENT_PRODUCT_NOT_FOUND를 던진다.
     */
    fun readForUpdateWithActiveProduct(exposureId: Long): AdvertisementExposureUpdateTargetDto {
        val productId = readActiveProductId(exposureId)
        val product = advertisementProductReader.readForUpdate(productId)
        return AdvertisementExposureUpdateTargetDto(
            exposure = lockExposure(exposureId),
            product = product,
        )
    }

    /**
     * A-15도 다른 노출 변경과 같은 상품 → 대상 노출 잠금 순서를 사용한다.
     */
    fun readForMove(exposureId: Long): AdvertisementExposure = readForUpdate(exposureId)

    /**
     * 세팅 화면의 한 탭(상품 자리)이다. 꺼졌거나 기간이 지난 것도 모두 순서대로 준다.
     * 광고주로 거르면 그 계약의 노출만 준다.
     */
    fun readByProduct(productId: Long, contractId: Long? = null): List<AdvertisementExposure> =
        advertisementExposureQueryRepository.findByProduct(productId, contractId)

    /**
     * 사용자 화면의 한 자리에서 지금 보이는 노출을 순서대로 준다. 지운 상품의 노출은 주지 않는다.
     * 광고 공고 노출이 가리키는 공고가 지워졌거나 게시 중이 아닌지는 공고를 붙이는 쪽이 거른다.
     */
    fun readShown(position: AdvertisementProductPosition, now: LocalDateTime): List<AdvertisementExposure> {
        val productIds = advertisementProductReader.readByPosition(position).map { checkNotNull(it.id) }
        return advertisementExposureQueryRepository.findShown(productIds, now)
    }

    private fun readActiveProductId(exposureId: Long): Long =
        advertisementExposureRepository.findProductIdByIdAndDeletedAtIsNull(exposureId)
            ?: throw EntityNotFoundException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_NOT_FOUND)

    private fun lockExposure(exposureId: Long): AdvertisementExposure =
        advertisementExposureRepository.findForUpdate(exposureId)
            ?: throw EntityNotFoundException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_NOT_FOUND)
}
