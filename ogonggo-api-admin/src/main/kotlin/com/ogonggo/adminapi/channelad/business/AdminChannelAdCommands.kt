package com.ogonggo.adminapi.channelad.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdSearchCondition
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import com.ogonggo.core.channelad.domain.ChannelAdTerms
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.Optional

/** 채널 광고 목록 조건이다. `month`(`YYYY-MM`)는 그달과 겹치는 광고다. 모두 비우면 전부다. */
data class AdminChannelAdSearchCommand(
    val month: String? = null,
    val advertiserId: Long? = null,
    val contractId: Long? = null,
    val platform: AdChannelPlatform? = null,
    val channelId: Long? = null,
    val formatId: Long? = null,
    val status: ChannelAdStatus? = null,
) {
    internal fun toCondition(): ChannelAdSearchCondition {
        val month = month?.takeIf { it.isNotBlank() }?.let(::parseMonth)
        return ChannelAdSearchCondition(
            from = month?.atDay(1),
            toExclusive = month?.plusMonths(1)?.atDay(1),
            advertiserId = advertiserId,
            contractId = contractId,
            platform = platform,
            channelId = channelId,
            formatId = formatId,
            status = status,
        )
    }

    private fun parseMonth(value: String): YearMonth {
        if (!MONTH.matches(value)) throw monthError()
        return try {
            YearMonth.parse(value)
        } catch (exception: DateTimeParseException) {
            throw monthError()
        }
    }

    private fun monthError() = AdminConsoleBadRequestException.field("month", "YYYY-MM 로 보내 주세요.")

    private companion object {
        val MONTH = Regex("^\\d{4}-\\d{2}$")
    }
}

/** 채널 광고 등록 본문이다. 검증 전 값을 그대로 담는다. 상태를 보내지 않으면 예정이다. */
data class AdminChannelAdCreateCommand(
    val advertiserId: Long?,
    val contractId: Long?,
    val channelId: Long?,
    val formatId: Long?,
    val postDate: LocalDate?,
    val endDate: LocalDate?,
    val status: ChannelAdStatus?,
    val postUrl: String?,
    val creativeMemo: String?,
    val adminMemo: String?,
) {
    internal fun toDraft(): AdminChannelAdDraft = AdminChannelAdDraft(
        advertiserId = advertiserId,
        contractId = contractId,
        channelId = channelId,
        formatId = formatId,
        postDate = postDate,
        endDate = endDate,
        status = status ?: ChannelAdStatus.SCHEDULED,
        postUrl = postUrl,
        creativeMemo = creativeMemo,
        adminMemo = adminMemo,
    )
}

/**
 * 채널 광고 수정 본문이다. null이면 저장된 값을 그대로 둔다. null을 보낼 수 있는 칸(`contractId`, `endDate`, `postUrl`,
 * `creativeMemo`, `adminMemo`)은 보내지 않으면 null, null을 보내면 빈 [Optional]이다.
 */
data class AdminChannelAdUpdateCommand(
    val advertiserId: Long? = null,
    val contractId: Optional<Long>? = null,
    val channelId: Long? = null,
    val formatId: Long? = null,
    val postDate: LocalDate? = null,
    val endDate: Optional<LocalDate>? = null,
    val status: ChannelAdStatus? = null,
    val postUrl: Optional<String>? = null,
    val creativeMemo: Optional<String>? = null,
    val adminMemo: Optional<String>? = null,
) {
    /**
     * 저장된 값 위에 보낸 칸을 얹는다. 광고주를 바꾸면서 계약을 보내지 않으면 다른 광고주의 계약이 남지 않게 계약을 끊는다.
     * 검증은 합친 결과로 한다.
     */
    internal fun mergeInto(ad: ChannelAd): AdminChannelAdDraft {
        val advertiserChanged = advertiserId != null && advertiserId != ad.advertiserId
        return AdminChannelAdDraft(
            advertiserId = advertiserId ?: ad.advertiserId,
            contractId = when {
                contractId != null -> contractId.orElse(null)
                advertiserChanged -> null
                else -> ad.contractId
            },
            channelId = channelId ?: ad.channelId,
            formatId = formatId ?: ad.formatId,
            postDate = postDate ?: ad.postDate,
            endDate = if (endDate == null) ad.endDate else endDate.orElse(null),
            status = status ?: ad.status,
            postUrl = if (postUrl == null) ad.postUrl else postUrl.orElse(null),
            creativeMemo = if (creativeMemo == null) ad.creativeMemo else creativeMemo.orElse(null),
            adminMemo = if (adminMemo == null) ad.adminMemo else adminMemo.orElse(null),
        )
    }
}

/** 등록 본문이나 저장된 값과 합친 수정 본문이다. 날짜·주소·메모 칸만 여기서 보고, 광고주·계약·채널·유형은 서비스가 먼저 본다. */
internal data class AdminChannelAdDraft(
    val advertiserId: Long?,
    val contractId: Long?,
    val channelId: Long?,
    val formatId: Long?,
    val postDate: LocalDate?,
    val endDate: LocalDate?,
    val status: ChannelAdStatus?,
    val postUrl: String?,
    val creativeMemo: String?,
    val adminMemo: String?,
) {
    /** 서비스가 광고주·계약·채널·유형을 본 뒤 부른다. */
    fun toTerms(advertiserId: Long, platform: AdChannelPlatform, channelId: Long, formatId: Long): ChannelAdTerms {
        val postDate = postDate ?: throw AdminConsoleBadRequestException.field("postDate", "YYYY-MM-DD(KST) 로 보내 주세요.")
        if (endDate != null && endDate.isBefore(postDate)) {
            throw AdminConsoleBadRequestException.field("endDate", "null 이거나 게시 예정일과 같거나 뒤인 YYYY-MM-DD 로 보내 주세요.")
        }
        val status = status ?: throw AdminConsoleBadRequestException.field(
            "status",
            "${ChannelAdStatus.entries.joinToString(", ") { it.name }} 가운데 하나로 보내 주세요.",
        )
        val postUrl = postUrl?.trim()?.ifEmpty { null }
        if (postUrl != null && (!ChannelAdTerms.URL.matches(postUrl) || postUrl.length > ChannelAdTerms.MAX_URL_LENGTH)) {
            throw AdminConsoleBadRequestException.field("postUrl", "null 이거나 http:// 또는 https:// 로 시작하는 주소로 보내 주세요.")
        }
        listOf("creativeMemo" to creativeMemo, "adminMemo" to adminMemo).forEach { (fieldName, value) ->
            if ((value?.trim()?.length ?: 0) > ChannelAdTerms.MAX_MEMO_LENGTH) {
                throw AdminConsoleBadRequestException.field(fieldName, "${ChannelAdTerms.MAX_MEMO_LENGTH}자까지 보낼 수 있습니다.")
            }
        }
        return ChannelAdTerms(
            advertiserId = advertiserId,
            contractId = contractId,
            platform = platform,
            channelId = channelId,
            formatId = formatId,
            postDate = postDate,
            endDate = endDate,
            status = status,
            postUrl = postUrl,
            creativeMemo = creativeMemo,
            adminMemo = adminMemo,
        )
    }
}
