package com.ogonggo.adminapi.advertisercontract.business

import com.ogonggo.core.adchannelformat.implement.AdChannelFormatReader
import com.ogonggo.core.advertiser.implement.AdvertiserAdCountReader
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractAppender
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractManager
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.advertisercontract.implement.dto.AdvertiserContractAppendDto
import com.ogonggo.core.channelad.implement.ChannelAdReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdvertiserContractService(
    private val advertiserReader: AdvertiserReader,
    private val advertiserContractReader: AdvertiserContractReader,
    private val advertiserContractAppender: AdvertiserContractAppender,
    private val advertiserContractManager: AdvertiserContractManager,
    private val advertiserAdCountReader: AdvertiserAdCountReader,
    private val clock: Clock,
    private val adChannelFormatReader: AdChannelFormatReader,
    private val channelAdReader: ChannelAdReader,
) {

    /** 광고주 하나의 계약 전부다. 시작이 늦은 것이 먼저이고 같으면 나중에 등록한 것이 먼저다. */
    fun getContracts(advertiserId: Long): List<AdminAdvertiserContractResult> {
        advertiserReader.validateExists(advertiserId)
        return toResults(advertiserContractReader.readAllByAdvertiser(advertiserId))
    }

    fun getContract(contractId: Long): AdminAdvertiserContractResult =
        toResults(listOf(advertiserContractReader.read(contractId))).single()

    /** 계약을 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createContract(advertiserId: Long, command: AdminAdvertiserContractCreateCommand): Long {
        advertiserReader.validateExists(advertiserId)
        val terms = command.toDraft().toTerms(channelFormats())
        return advertiserContractAppender.append(AdvertiserContractAppendDto(advertiserId = advertiserId, terms = terms)).requiredId()
    }

    /** 보낸 칸만 바꾼다. 검증은 저장된 값과 합친 결과로 한다. */
    @Transactional
    fun updateContract(contractId: Long, command: AdminAdvertiserContractUpdateCommand) {
        val contract = advertiserContractReader.readForUpdate(contractId)
        advertiserContractManager.change(contract, command.mergeInto(contract).toTerms(channelFormats(contract.channelFormatIds)))
    }

    /** 계약만 지운다. 이미 지운 계약을 다시 지워도 성공한다. */
    @Transactional
    fun deleteContract(contractId: Long) {
        advertiserContractManager.delete(advertiserContractReader.readForDelete(contractId), LocalDateTime.now(clock))
    }

    private fun toResults(contracts: List<AdvertiserContract>): List<AdminAdvertiserContractResult> {
        val adCounts = advertiserAdCountReader.readByContracts(contracts.map { it.requiredId() })
        val channelFormatOrder = adChannelFormatReader.readAll().map { checkNotNull(it.id) }
        val channelAdCounts = channelAdReader.countByContracts(contracts.map { it.requiredId() })
        return contracts.map {
            AdminAdvertiserContractResult.of(
                contract = it,
                adCount = adCounts.getValue(it.requiredId()),
                channelFormatOrder = channelFormatOrder,
                channelAdCount = channelAdCounts.getValue(it.requiredId()),
            )
        }
    }

    private fun channelFormats(previousIds: Collection<Long> = emptyList()): AdminContractChannelFormats {
        val formats = adChannelFormatReader.readAll()
        return AdminContractChannelFormats(
            existingIds = formats.mapNotNull { it.id }.toSet(),
            enabledIds = formats.filter { it.enabled }.mapNotNull { it.id }.toSet(),
            previousIds = previousIds.toSet(),
        )
    }
}
