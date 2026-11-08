package com.ogonggo.core.advertisementproduct.domain

import com.ogonggo.core.error.InvalidValueException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdvertisementProductDomainTest {

    @Test
    fun `노출 자리는 상품 카테고리에 속한 자리만 고른다`() {
        assertThrows(InvalidValueException::class.java) {
            AdvertisementProduct(
                category = AdvertisementProductCategory.INSTAGRAM_1,
                position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                name = "인스타그램 피드",
                price = null,
            )
        }
    }
}
