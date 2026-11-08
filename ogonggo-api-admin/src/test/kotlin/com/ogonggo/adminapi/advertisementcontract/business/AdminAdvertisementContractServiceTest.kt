package com.ogonggo.adminapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractManager
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class AdminAdvertisementContractServiceTest {

    private val contractReader = Mockito.mock(AdvertisementContractReader::class.java)
    private val contractManager = Mockito.mock(AdvertisementContractManager::class.java)
    private val productReader = Mockito.mock(AdvertisementProductReader::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminAdvertisementContractService(contractReader, contractManager, productReader, clock)

    @Test
    fun `상품 연결은 계약과 상품을 잠가 읽은 뒤 연결 행을 만든다`() {
        // given
        val contract = Mockito.mock(AdvertisementContract::class.java)
        val product = Mockito.mock(AdvertisementProduct::class.java)
        Mockito.`when`(contractReader.readForUpdate(CONTRACT_ID)).thenReturn(contract)
        Mockito.`when`(productReader.readForUpdate(PRODUCT_ID)).thenReturn(product)
        Mockito.`when`(contractManager.addProduct(contract, product)).thenReturn(CONTRACT_PRODUCT_ID)

        // when
        val contractProductId = service.addProduct(CONTRACT_ID, PRODUCT_ID)

        // then
        assertEquals(CONTRACT_PRODUCT_ID, contractProductId)
        val order = Mockito.inOrder(contractReader, productReader, contractManager)
        order.verify(contractReader).readForUpdate(CONTRACT_ID)
        order.verify(productReader).readForUpdate(PRODUCT_ID)
        order.verify(contractManager).addProduct(contract, product)
        Mockito.verifyNoMoreInteractions(contractReader, productReader, contractManager)
    }

    companion object {
        private const val CONTRACT_ID = 1L
        private const val PRODUCT_ID = 11L
        private const val CONTRACT_PRODUCT_ID = 3001L
    }
}
