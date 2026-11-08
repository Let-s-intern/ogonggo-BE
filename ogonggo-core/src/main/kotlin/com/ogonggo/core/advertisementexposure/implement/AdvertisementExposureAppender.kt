package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.springframework.stereotype.Component

@Component
class AdvertisementExposureAppender internal constructor(
    private val advertisementExposureRepository: AdvertisementExposureJpaRepository,
    private val advertisementProductReader: AdvertisementProductReader,
    private val advertisementContractReader: AdvertisementContractReader,
) {

    /** 그 자리의 맨 뒤에 둔다. 상품별 순서 잠금을 쓰므로 호출자가 DB 트랜잭션을 열어야 한다. */
    fun append(dto: AdvertisementExposureAppendDto): Long {
        // 상품 행을 순서 잠금 기준으로 삼는다. 반드시 같은 DB 트랜잭션에서
        // 잠금을 얻은 뒤 형제 노출을 조회해야 concurrent append가 같은 순서를 고르지 않는다.
        val product = advertisementProductReader.readForUpdate(dto.productId)
        val lastOrder = advertisementExposureRepository
            .findAllForOrderUpdate(dto.productId)
            .maxOfOrNull { it.displayOrder } ?: 0
        dto.contractId?.let(advertisementContractReader::read)
        val saved = advertisementExposureRepository.save(
            AdvertisementExposure(
                product = product,
                contractId = dto.contractId,
                content = dto.content,
                adLabel = dto.adLabel,
                period = dto.period,
                exposed = dto.exposed,
                displayOrder = lastOrder + 1,
            ),
        )
        return checkNotNull(saved.id) { "광고 노출 식별자가 없습니다." }
    }
}
