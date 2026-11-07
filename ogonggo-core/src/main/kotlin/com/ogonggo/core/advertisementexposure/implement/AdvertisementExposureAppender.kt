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

    /** 그 자리의 맨 뒤에 둔다. 광고주 계약을 고르면 있는 계약이어야 한다. */
    fun append(dto: AdvertisementExposureAppendDto): Long {
        val product = advertisementProductReader.read(dto.productId)
        dto.contractId?.let(advertisementContractReader::read)
        val lastOrder = advertisementExposureRepository
            .findAllByProductIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(dto.productId)
            .maxOfOrNull { it.displayOrder } ?: 0
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
