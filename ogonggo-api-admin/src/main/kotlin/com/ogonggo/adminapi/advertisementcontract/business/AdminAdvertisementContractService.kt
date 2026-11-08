package com.ogonggo.adminapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractManager
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdvertisementContractService(
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementContractManager: AdvertisementContractManager,
    private val advertisementProductReader: AdvertisementProductReader,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getContracts(
        condition: AdvertisementContractSearchCondition,
        page: Int,
        size: Int,
    ): AdminAdvertisementContractPageResult {
        val result = advertisementContractReader.readPage(condition, page, size)
        return AdminAdvertisementContractPageResult.from(result)
    }

    @Transactional(readOnly = true)
    fun getContract(contractId: Long): AdminAdvertisementContractDetailResult {
        val contract = advertisementContractReader.read(contractId)
        val products = advertisementContractReader.readProducts(contractId)
        return AdminAdvertisementContractDetailResult.from(contract, products)
    }

    @Transactional
    fun changeStatus(contractId: Long, status: AdvertisementContractStatus) {
        val contract = advertisementContractReader.readForUpdate(contractId)
        advertisementContractManager.changeStatus(contract, status)
    }

    @Transactional
    fun changePaymentAmount(contractId: Long, paymentAmount: Int?) {
        val contract = advertisementContractReader.readForUpdate(contractId)
        advertisementContractManager.changePaymentAmount(contract, paymentAmount)
    }

    @Transactional
    fun addProduct(contractId: Long, productId: Long): Long {
        val contract = advertisementContractReader.readForUpdate(contractId)
        val product = advertisementProductReader.readForUpdate(productId)
        return advertisementContractManager.addProduct(contract, product)
    }

    @Transactional
    fun removeProduct(contractId: Long, contractProductId: Long) {
        val contract = advertisementContractReader.readForUpdate(contractId)
        advertisementContractManager.removeProduct(contract, contractProductId, LocalDateTime.now(clock))
    }
}
