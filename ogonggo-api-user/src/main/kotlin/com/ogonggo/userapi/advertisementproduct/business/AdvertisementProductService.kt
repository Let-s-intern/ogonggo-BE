package com.ogonggo.userapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.springframework.stereotype.Service

@Service
class AdvertisementProductService(
    private val advertisementProductReader: AdvertisementProductReader,
) {

    fun getProducts(): List<AdvertisementProductResult> =
        advertisementProductReader.readAll().map(AdvertisementProductResult::from)
}
