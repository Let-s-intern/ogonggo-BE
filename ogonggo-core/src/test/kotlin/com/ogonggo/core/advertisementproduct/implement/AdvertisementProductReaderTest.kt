package com.ogonggo.core.advertisementproduct.implement

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils

internal class AdvertisementProductReaderTest {

    private val repository = Mockito.mock(AdvertisementProductJpaRepository::class.java)
    private val reader = AdvertisementProductReader(repository)

    @Test
    fun `같은 카테고리 상품은 저장소 반환 순서와 관계없이 ID 오름차순으로 제공한다`() {
        // given
        val higherId = product(id = 20L)
        val lowerId = product(id = 10L)
        Mockito.`when`(repository.findAllByDeletedAtIsNull()).thenReturn(listOf(higherId, lowerId))

        // when
        val products = reader.readAll()

        // then
        assertEquals(listOf(10L, 20L), products.map { it.id })
    }

    private fun product(id: Long): AdvertisementProduct = AdvertisementProduct(
        category = AdvertisementProductCategory.BANNER,
        position = null,
        name = "배너 상품 $id",
        price = null,
    ).also { ReflectionTestUtils.setField(it, "id", id) }
}
