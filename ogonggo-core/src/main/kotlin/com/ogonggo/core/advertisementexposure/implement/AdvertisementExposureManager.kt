package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateDto
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertisementExposureManager internal constructor(
    private val advertisementExposureRepository: AdvertisementExposureJpaRepository,
    private val advertisementProductReader: AdvertisementProductReader,
    private val advertisementContractReader: AdvertisementContractReader,
) {

    /** 지운 상품 자리의 노출은 고칠 수 없다(상품을 찾지 못해 404). */
    fun update(exposure: AdvertisementExposure, dto: AdvertisementExposureUpdateDto) {
        val product = advertisementProductReader.read(exposure.productId)
        dto.contractId?.let(advertisementContractReader::read)
        exposure.update(
            product = product,
            contractId = dto.contractId,
            content = dto.content,
            adLabel = dto.adLabel,
            period = dto.period,
        )
        advertisementExposureRepository.save(exposure)
    }

    fun changeExposed(exposure: AdvertisementExposure, exposed: Boolean) {
        exposure.changeExposed(exposed)
        advertisementExposureRepository.save(exposure)
    }

    /**
     * 같은 자리 안에서 바로 옆 노출과 자리를 바꾼다. 이미 맨 앞(맨 뒤)이면 그대로 둔다.
     * 순서 값이 겹치거나 비어 있어도 바뀐 결과대로 1부터 다시 매긴다.
     */
    fun move(exposure: AdvertisementExposure, direction: AdvertisementExposureMoveDirection) {
        val siblings = advertisementExposureRepository
            .findAllForOrderUpdate(exposure.productId)
            .toMutableList()
        val index = siblings.indexOfFirst { it.id == exposure.id }
        check(index >= 0) { "같은 자리의 노출 목록에 없습니다." }
        val target = when (direction) {
            AdvertisementExposureMoveDirection.UP -> index - 1
            AdvertisementExposureMoveDirection.DOWN -> index + 1
        }
        if (target !in siblings.indices) {
            return
        }
        siblings[index] = siblings[target].also { siblings[target] = siblings[index] }
        siblings.forEachIndexed { order, sibling ->
            if (sibling.displayOrder != order + 1) {
                sibling.changeDisplayOrder(order + 1)
            }
        }
        advertisementExposureRepository.saveAll(siblings)
    }

    fun delete(exposure: AdvertisementExposure, now: LocalDateTime) {
        exposure.delete(now)
        advertisementExposureRepository.save(exposure)
    }
}
