package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.advertiser.implement.dto.AdvertiserAdCountDto
import com.ogonggo.core.advertiser.implement.dto.AdvertiserContractAdCountDto
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 광고주·광고 계약마다 배너와 광고 공고를 센다. 광고 수를 싣는 응답은 모두 여기서 읽는다.
 *
 * 배너(LC-3576)와 광고 공고(LC-3577)가 아직 없어 지금은 모두 0이고 노출 끝은 null이다. 두 엔티티가 생기면 이 클래스만
 * 광고주·계약 식별자로 묶어 세도록 바꾼다.
 */
@Component
class AdvertiserAdCountReader {

    /** [now] 기준으로 노출 중인 것을 함께 센다. 모든 광고주 식별자가 결과에 들어간다. */
    @Suppress("UNUSED_PARAMETER")
    fun readByAdvertisers(advertiserIds: Collection<Long>, now: LocalDateTime): Map<Long, AdvertiserAdCountDto> =
        advertiserIds.associateWith { AdvertiserAdCountDto.NONE }

    /** 모든 계약 식별자가 결과에 들어간다. */
    fun readByContracts(contractIds: Collection<Long>): Map<Long, AdvertiserContractAdCountDto> =
        contractIds.associateWith { AdvertiserContractAdCountDto.NONE }
}
