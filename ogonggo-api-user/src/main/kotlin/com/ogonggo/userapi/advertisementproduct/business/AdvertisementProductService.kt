package com.ogonggo.userapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.springframework.stereotype.Service

@Service
class AdvertisementProductService(
    private val advertisementProductReader: AdvertisementProductReader,
) {

    fun getProducts(page: Int, size: Int): AdvertisementProductPageResult {
        val result = advertisementProductReader.readPage(page, size)
        return AdvertisementProductPageResult(
            items = result.items.map(AdvertisementProductResult::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
