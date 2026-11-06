package com.ogonggo.core.advertiser.implement.dto

import com.ogonggo.core.advertiser.domain.AdvertiserContact
import java.time.LocalDateTime

/** 운영자가 직접 더하는 비회원 광고주다. */
data class AdvertiserAppendDto(
    val name: String,
    val contact: AdvertiserContact,
)

/** 비회원 광고주에서 바꿀 칸이다. null이면 그대로 두고 빈 문자열이면 지운다. 이름은 지울 수 없다. */
data class AdvertiserUpdateDto(
    val name: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
)

/** 광고주 하나의 배너·광고 공고 수다. 노출 중(`active`)은 켜져 있고 기간 안인 것만 센다. */
data class AdvertiserAdCountDto(
    val bannerCount: Int,
    val activeBannerCount: Int,
    val adPostingCount: Int,
    val activeAdPostingCount: Int,
    /** 배너·광고 공고 노출 끝 가운데 가장 늦은 것. 끝이 정해진 광고가 없으면 null이다. */
    val latestEndAt: LocalDateTime?,
) {
    companion object {
        val NONE = AdvertiserAdCountDto(0, 0, 0, 0, null)
    }
}

/** 광고 계약 하나에 이어 둔 배너·광고 공고 수다. */
data class AdvertiserContractAdCountDto(
    val bannerCount: Int,
    val adPostingCount: Int,
) {
    companion object {
        val NONE = AdvertiserContractAdCountDto(0, 0)
    }
}
