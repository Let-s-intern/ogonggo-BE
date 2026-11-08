package com.ogonggo.adminapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductManager
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdminAdvertisementProductServiceTest {

    private val productReader = Mockito.mock(AdvertisementProductReader::class.java)
    private val productAppender = Mockito.mock(AdvertisementProductAppender::class.java)
    private val productManager = Mockito.mock(AdvertisementProductManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminAdvertisementProductService(productReader, productAppender, productManager, clock)

    @Test
    fun `상품 수정은 삭제와 같은 잠금으로 읽은 상품을 교체한다`() {
        // given
        val product = Mockito.mock(AdvertisementProduct::class.java)
        val command = AdminAdvertisementProductSaveCommand(
            category = AdvertisementProductCategory.JOB_POSTING,
            position = null,
            name = "수정한 광고 상품",
            price = 120_000,
        )
        Mockito.`when`(productReader.readForUpdate(PRODUCT_ID)).thenReturn(product)

        // when
        service.replaceProduct(PRODUCT_ID, command)

        // then
        Mockito.verify(productReader).readForUpdate(PRODUCT_ID)
        Mockito.verify(productManager).update(
            product,
            AdvertisementProductSaveDto(
                category = command.category,
                position = command.position,
                name = command.name,
                price = command.price,
            ),
        )
        Mockito.verifyNoMoreInteractions(productReader, productManager)
    }

    @Test
    fun `상품 삭제는 잠가 읽은 상품을 소프트 삭제한다`() {
        // given
        val product = Mockito.mock(AdvertisementProduct::class.java)
        Mockito.`when`(productReader.readForUpdate(PRODUCT_ID)).thenReturn(product)

        // when
        service.deleteProduct(PRODUCT_ID)

        // then
        Mockito.verify(productReader).readForUpdate(PRODUCT_ID)
        Mockito.verify(productManager).delete(product, NOW)
        Mockito.verifyNoMoreInteractions(productReader, productManager)
    }

    companion object {
        private const val PRODUCT_ID = 11L
        private val NOW = LocalDateTime.of(2026, 11, 8, 12, 0)
    }
}
