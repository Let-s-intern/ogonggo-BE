package com.ogonggo.adminapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductManager
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdvertisementProductService(
    private val advertisementProductReader: AdvertisementProductReader,
    private val advertisementProductAppender: AdvertisementProductAppender,
    private val advertisementProductManager: AdvertisementProductManager,
    private val clock: Clock,
) {

    fun getProducts(page: Int, size: Int): AdminAdvertisementProductPageResult {
        val result = advertisementProductReader.readPage(page, size)
        return AdminAdvertisementProductPageResult(
            items = result.items.map(AdminAdvertisementProductResult::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun createProduct(command: AdminAdvertisementProductSaveCommand): Long =
        advertisementProductAppender.append(command.toCoreDto())

    @Transactional
    fun replaceProduct(productId: Long, command: AdminAdvertisementProductSaveCommand) {
        advertisementProductManager.update(advertisementProductReader.readForUpdate(productId), command.toCoreDto())
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        advertisementProductManager.delete(advertisementProductReader.readForUpdate(productId), LocalDateTime.now(clock))
    }
}
