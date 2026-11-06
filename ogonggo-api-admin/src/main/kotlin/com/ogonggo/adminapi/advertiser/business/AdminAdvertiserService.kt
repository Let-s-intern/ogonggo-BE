package com.ogonggo.adminapi.advertiser.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.implement.AdvertiserAdCountReader
import com.ogonggo.core.advertiser.implement.AdvertiserAppender
import com.ogonggo.core.advertiser.implement.AdvertiserManager
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertiser.implement.dto.AdvertiserAppendDto
import com.ogonggo.core.advertiser.implement.dto.AdvertiserUpdateDto
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Collator
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale

@Service
class AdminAdvertiserService(
    private val advertiserReader: AdvertiserReader,
    private val advertiserAppender: AdvertiserAppender,
    private val advertiserManager: AdvertiserManager,
    private val advertiserContractReader: AdvertiserContractReader,
    private val advertiserAdCountReader: AdvertiserAdCountReader,
    private val clock: Clock,
) {

    /**
     * 비즈니스 회원·광고 문의 기업·직접 추가한 기업을 합친 목록이다. 없는 회원·문의 광고주는 이때 만들어 고정 식별자를 준다.
     * 진행 중 계약이 있는 곳, 계약만 있는 곳, 계약이 없는 곳 순이고 같은 무리 안에서는 계약 끝이 늦은 곳, 이름(한국어) 순이다.
     * 정렬한 뒤 [LIST_LIMIT]건까지 준다.
     */
    @Transactional
    fun getAdvertisers(condition: AdvertiserSearchCondition): List<AdminAdvertiserResult> {
        advertiserManager.syncSources()
        val now = LocalDateTime.now(clock)
        val advertisers = advertiserReader.readAll(condition)
        val contracts = advertiserContractReader.readAllByAdvertisers(advertisers.map { it.requiredId() })
        val ranked = advertisers
            .sortedWith(contractFirst(contracts, now))
            .take(LIST_LIMIT)
        return toResults(ranked, contracts, now)
    }

    fun getAdvertiser(advertiserId: Long): AdminAdvertiserResult {
        val advertiser = advertiserReader.read(advertiserId)
        val now = LocalDateTime.now(clock)
        return toResults(listOf(advertiser), advertiserContractReader.readAllByAdvertisers(listOf(advertiserId)), now).single()
    }

    /** 비회원 광고주(직접 추가)를 만들고 식별자를 돌려준다. 공백·대소문자를 빼고 같은 이름이 있으면 막는다. */
    @Transactional
    fun createAdvertiser(command: AdminAdvertiserCreateCommand): Long {
        val name = validAdvertiserName(command.name)
        if (advertiserReader.existsSameName(name)) {
            throw AdminConsoleBadRequestException.field("name", "같은 이름의 광고주가 이미 있습니다. 목록에서 골라 주세요.")
        }
        val contact = command.validContact()
        return advertiserAppender.appendManual(AdvertiserAppendDto(name = name, contact = contact)).requiredId()
    }

    /** 비회원 광고주의 보낸 칸만 바꾼다. 칸을 모두 본 뒤 한꺼번에 바꾸므로 하나라도 걸리면 아무것도 바뀌지 않는다. */
    @Transactional
    fun updateAdvertiser(advertiserId: Long, command: AdminAdvertiserUpdateCommand) {
        val advertiser = advertiserReader.readForUpdate(advertiserId)
        if (advertiser.type == AdvertiserType.MEMBER) {
            throw AdminConsoleBadRequestException("회원 광고주는 비즈니스 회원 정보를 따릅니다. 회원 정보에서 바꿔 주세요.")
        }
        val name = command.name?.let(::validAdvertiserName)
        if (name != null && advertiserReader.existsSameName(name, exceptAdvertiserId = advertiserId)) {
            throw AdminConsoleBadRequestException.field("name", "같은 이름의 광고주가 이미 있습니다.")
        }
        val dto = AdvertiserUpdateDto(
            name = name,
            contactName = command.contactName?.let(::validContactName),
            contactEmail = command.contactEmail?.let(::validContactEmail),
            contactPhone = command.contactPhone?.let(::validContactPhone),
        )
        advertiserManager.update(advertiser, dto)
    }

    private fun toResults(
        advertisers: List<Advertiser>,
        contracts: Map<Long, List<AdvertiserContract>>,
        now: LocalDateTime,
    ): List<AdminAdvertiserResult> {
        val adCounts = advertiserAdCountReader.readByAdvertisers(advertisers.map { it.requiredId() }, now)
        return advertisers.map { advertiser ->
            val advertiserId = advertiser.requiredId()
            AdminAdvertiserResult.of(
                advertiser = advertiser,
                adCount = adCounts.getValue(advertiserId),
                contractTotals = AdminAdvertiserContractTotals.of(contracts[advertiserId].orEmpty(), now),
            )
        }
    }

    private fun contractFirst(contracts: Map<Long, List<AdvertiserContract>>, now: LocalDateTime): Comparator<Advertiser> {
        val collator = Collator.getInstance(Locale.KOREAN)
        val group = { advertiser: Advertiser ->
            val owned = contracts[advertiser.requiredId()].orEmpty()
            when {
                owned.any { it.isActive(now) } -> 0
                owned.isNotEmpty() -> 1
                else -> 2
            }
        }
        val latestEndAt = { advertiser: Advertiser -> contracts[advertiser.requiredId()].orEmpty().maxOfOrNull { it.endAt } }
        return compareBy(group)
            .thenComparing(latestEndAt, nullsLast(reverseOrder()))
            .thenComparing({ it.name }, collator)
            .thenBy { it.requiredId() }
    }

    companion object {
        const val LIST_LIMIT = 50
    }
}
