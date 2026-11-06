package com.ogonggo.core.channelad.implement

import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdSearchCondition
import com.ogonggo.core.channelad.error.ChannelAdErrorCode
import com.ogonggo.core.channelad.persistence.ChannelAdJpaRepository
import com.ogonggo.core.channelad.persistence.ChannelAdQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class ChannelAdReader internal constructor(
    private val channelAdRepository: ChannelAdJpaRepository,
    private val channelAdQueryRepository: ChannelAdQueryRepository,
) {

    fun read(adId: Long): ChannelAd =
        channelAdRepository.findByIdAndDeletedAtIsNull(adId)
            ?: throw EntityNotFoundException(ChannelAdErrorCode.CHANNEL_AD_NOT_FOUND)

    /** 조건에 맞는 광고 전부다. 게시일, 식별자 순이다. */
    fun readAll(condition: ChannelAdSearchCondition): List<ChannelAd> = channelAdQueryRepository.findAll(condition)

    /** 이 채널에 잡힌 광고가 있는가. 지운 광고는 보지 않는다. */
    fun existsByChannel(channelId: Long): Boolean = channelAdRepository.existsByChannelIdAndDeletedAtIsNull(channelId)

    /** 이 유형으로 잡힌 광고가 있는가. 지운 광고는 보지 않는다. */
    fun existsByFormat(formatId: Long): Boolean = channelAdRepository.existsByFormatIdAndDeletedAtIsNull(formatId)

    /** 채널마다 잡힌 광고 수다. 모든 식별자가 결과에 들어간다. */
    fun countByChannels(channelIds: Collection<Long>): Map<Long, Int> = channelAdQueryRepository.countByChannels(channelIds)

    /** 유형마다 잡힌 광고 수다. 모든 식별자가 결과에 들어간다. */
    fun countByFormats(formatIds: Collection<Long>): Map<Long, Int> = channelAdQueryRepository.countByFormats(formatIds)

    /** 계약마다 이어 둔 광고 수다. 모든 식별자가 결과에 들어간다. */
    fun countByContracts(contractIds: Collection<Long>): Map<Long, Int> = channelAdQueryRepository.countByContracts(contractIds)

    /** 수정할 광고를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(adId: Long): ChannelAd =
        channelAdRepository.findForUpdate(adId)
            ?: throw EntityNotFoundException(ChannelAdErrorCode.CHANNEL_AD_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 광고도 잠가 찾는다. */
    fun readForDelete(adId: Long): ChannelAd =
        channelAdRepository.findIncludingDeletedForUpdate(adId)
            ?: throw EntityNotFoundException(ChannelAdErrorCode.CHANNEL_AD_NOT_FOUND)
}
