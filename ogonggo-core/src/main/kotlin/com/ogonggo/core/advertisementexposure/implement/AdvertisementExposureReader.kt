package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
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

    /** 고치거나 켜고 끄거나 지울 노출을 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(exposureId: Long): AdvertisementExposure =
        advertisementExposureRepository.findForUpdate(exposureId)
            ?: throw EntityNotFoundException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_NOT_FOUND)

    /**
     * 세팅 화면의 한 탭(상품 자리)이다. 꺼졌거나 기간이 지난 것도 모두 순서대로 준다.
     * 광고주로 거르면 그 계약의 노출만 준다.
     */
    fun readByProduct(productId: Long, contractId: Long? = null): List<AdvertisementExposure> =
        advertisementExposureRepository.findAllByProductIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(productId)
            .filter { contractId == null || it.contractId == contractId }

    /**
     * 사용자 화면의 한 자리에서 지금 보이는 노출을 순서대로 준다. 지운 상품의 노출은 주지 않는다.
     * 광고 공고 노출이 가리키는 공고가 지워졌거나 게시 중이 아닌지는 공고를 붙이는 쪽이 거른다.
     */
    fun readShown(position: AdvertisementProductPosition, now: LocalDateTime): List<AdvertisementExposure> {
        val productIds = advertisementProductReader.readByPosition(position).map { checkNotNull(it.id) }
        return advertisementExposureQueryRepository.findShown(productIds, now)
    }
}
