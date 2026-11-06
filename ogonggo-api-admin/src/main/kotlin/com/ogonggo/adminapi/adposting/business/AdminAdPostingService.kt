package com.ogonggo.adminapi.adposting.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPeriod
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.adposting.implement.AdPostingAppender
import com.ogonggo.core.adposting.implement.AdPostingManager
import com.ogonggo.core.adposting.implement.AdPostingReader
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.adposting.implement.dto.AdPostingAppendDto
import com.ogonggo.core.adposting.implement.dto.AdPostingTargetDto
import com.ogonggo.core.adposting.implement.dto.AdPostingUpdateDto
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 관리자 콘솔의 광고 공고 세팅이다. 400 문구는 화면에 그대로 보이는 `필드: 설명`이고 프런트 목과 같은 순서로 본다.
 *
 * 사용자 API는 자리별 결과를 짧게 캐시하고 이 API와 다른 프로세스라 쓰기 때 그 캐시를 비울 수 없다. 바꾼 값은 캐시 시간이
 * 지나면 사용자에게 나간다.
 */
@Service
class AdminAdPostingService(
    private val adPostingReader: AdPostingReader,
    private val adPostingAppender: AdPostingAppender,
    private val adPostingManager: AdPostingManager,
    private val adPostingTargetReader: AdPostingTargetReader,
    private val advertiserReader: AdvertiserReader,
    private val advertiserContractReader: AdvertiserContractReader,
    private val clock: Clock,
) {

    /**
     * [advertiserId]가 없으면 자리 하나의 광고 공고 전부(꺼진 것·기간이 지난 것·대상이 비공개인 것 포함)를 순서대로 준다.
     * 있으면 그 광고주의 것만 주고, [placement]가 없으면 모든 자리에서 자리 선언 순서·자리 안 순서대로 준다.
     */
    fun getAdPostings(placement: AdPostingPlacement?, advertiserId: Long?): List<AdminAdPostingResult> {
        val adPostings = when {
            advertiserId != null -> adPostingReader.readAllByAdvertiser(advertiserId, placement)
            placement != null -> adPostingReader.readAllByPlacement(placement)
            else -> throw placementError()
        }
        return toResults(adPostings)
    }

    fun getAdPosting(adPostingId: Long): AdminAdPostingResult = toResults(listOf(adPostingReader.read(adPostingId))).single()

    /** 그 자리 맨 뒤에 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createAdPosting(command: AdminAdPostingCreateCommand): Long {
        val placement = command.placement ?: throw placementError()
        val targetId = command.targetId
        if (targetId == null || targetId <= 0 || !adPostingTargetReader.exists(placement.targetType, targetId)) {
            throw AdminConsoleBadRequestException.field("targetId", "있는 ${placement.targetType.label} id 를 보내 주세요.")
        }
        if (adPostingReader.existsInPlacement(placement, targetId)) {
            throw AdminConsoleBadRequestException.field("targetId", "이 자리에 이미 등록된 대상입니다.")
        }
        val advertiserId = command.advertiserId?.takeIf(::advertiserExists) ?: throw advertiserError()
        validateContract(command.contractId, advertiserId)
        val enabled = command.enabled ?: throw AdminConsoleBadRequestException.field("enabled", "참·거짓 값을 보내 주세요.")
        val period = period(command.startAt, command.endAt)
        return adPostingAppender.append(
            AdPostingAppendDto(
                placement = placement,
                targetId = targetId,
                advertiserId = advertiserId,
                contractId = command.contractId,
                period = period,
                enabled = enabled,
            ),
        ).requiredId()
    }

    /** 보낸 칸만 바꾼다. 계약과 기간은 저장된 값과 합친 결과로 본다. */
    @Transactional
    fun updateAdPosting(adPostingId: Long, command: AdminAdPostingUpdateCommand) {
        val adPosting = adPostingReader.readForUpdate(adPostingId)
        if (command.advertiserId != null && !advertiserExists(command.advertiserId)) {
            throw advertiserError()
        }
        val advertiserId = command.advertiserId ?: adPosting.advertiserId
        val contractId = if (command.contractId == null) adPosting.contractId else command.contractId.orElse(null)
        validateContract(contractId, advertiserId)
        val period = period(
            startAt = if (command.startAt == null) adPosting.startAt else command.startAt.orElse(null),
            endAt = if (command.endAt == null) adPosting.endAt else command.endAt.orElse(null),
        )
        adPostingManager.change(
            adPosting,
            AdPostingUpdateDto(
                advertiserId = advertiserId,
                contractId = contractId,
                period = period,
                enabled = command.enabled ?: adPosting.enabled,
            ),
        )
    }

    /** 지우고 남은 광고 공고의 순서를 1부터 다시 매긴다. 이미 지운 광고 공고를 다시 지워도 성공한다. */
    @Transactional
    fun deleteAdPosting(adPostingId: Long) {
        adPostingManager.delete(adPostingReader.readForDelete(adPostingId), LocalDateTime.now(clock))
    }

    /** 자리 안 순서를 통째로 바꾸고 그 자리를 돌려준다. 그 자리의 광고 공고 식별자를 빠짐없이 한 번씩 받는다. */
    @Transactional
    fun replaceOrder(command: AdminAdPostingOrderCommand): AdPostingPlacement {
        val placement = command.placement ?: throw placementError()
        val current = adPostingReader.readAllByPlacementForUpdate(placement).map { it.requiredId() }
        val ids = command.adPostingIds.orEmpty()
        if (ids.any { it == null } || ids.size != current.size || ids.toSet() != current.toSet()) {
            throw AdminConsoleBadRequestException.field("adPostingIds", "그 자리의 광고 id 를 빠짐없이 한 번씩 보내 주세요.")
        }
        adPostingManager.reorder(placement, ids.filterNotNull())
        return placement
    }

    private fun toResults(adPostings: List<AdPosting>): List<AdminAdPostingResult> {
        if (adPostings.isEmpty()) {
            return emptyList()
        }
        val targets = adPostingTargetReader.read(adPostings, LocalDateTime.now(clock))
        val advertisers = advertiserReader.readAllByIds(adPostings.map { it.advertiserId })
        val contracts = advertiserContractReader.readAllByIds(adPostings.mapNotNull { it.contractId })
        return adPostings.map { adPosting ->
            AdminAdPostingResult.of(
                adPosting = adPosting,
                target = targets[adPosting.requiredId()] ?: AdPostingTargetDto.MISSING,
                advertiser = advertisers[adPosting.advertiserId],
                contract = adPosting.contractId?.let(contracts::get),
            )
        }
    }

    private fun advertiserExists(advertiserId: Long): Boolean =
        advertiserId > 0 && advertiserReader.readAllByIds(listOf(advertiserId)).isNotEmpty()

    /** 계약은 고른 광고주의 것만 잇는다. 노출 기간이 계약 기간 밖이어도 받는다(관리자 화면이 경고만 한다). */
    private fun validateContract(contractId: Long?, advertiserId: Long) {
        if (contractId == null) {
            return
        }
        val contract = advertiserContractReader.readAllByIds(listOf(contractId))[contractId]
            ?: throw AdminConsoleBadRequestException.field("contractId", "있는 계약을 골라 주세요.")
        if (contract.advertiserId != advertiserId) {
            throw AdminConsoleBadRequestException.field("contractId", "고른 광고주의 계약만 이을 수 있습니다.")
        }
    }

    private fun period(startAt: LocalDateTime?, endAt: LocalDateTime?): AdPostingPeriod {
        if (!AdPostingPeriod.isValid(startAt, endAt)) {
            throw AdminConsoleBadRequestException.field("endAt", "노출 끝은 시작보다 뒤여야 합니다.")
        }
        return AdPostingPeriod(startAt, endAt)
    }

    private fun advertiserError() = AdminConsoleBadRequestException.field("advertiserId", "광고주를 골라 주세요.")

    companion object {
        internal fun placementError(): AdminConsoleBadRequestException = AdminConsoleBadRequestException.field(
            "placement",
            "${AdPostingPlacement.entries.joinToString(", ") { it.name }} 중 하나를 보내 주세요.",
        )
    }
}

/** 화면 문구에 쓰는 대상 이름이다. */
private val AdPostingTargetType.label: String
    get() = when (this) {
        AdPostingTargetType.JOB -> "채용공고"
        AdPostingTargetType.BOOTCAMP -> "부트캠프"
    }
