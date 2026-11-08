package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdvertisementExposureService(
    private val advertisementExposureReader: AdvertisementExposureReader,
    private val clock: Clock,
) {

    fun getShown(position: AdvertisementProductPosition): List<AdvertisementExposureResult> =
        advertisementExposureReader
            .readShown(position, LocalDateTime.now(clock))
            .map(AdvertisementExposureResult::from)
}
