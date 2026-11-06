package com.ogonggo.adminapi.adchannel.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.implement.AdChannelAppender
import com.ogonggo.core.adchannel.implement.AdChannelManager
import com.ogonggo.core.adchannel.implement.AdChannelReader
import com.ogonggo.core.adchannel.implement.dto.AdChannelAppendDto
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatManager
import com.ogonggo.core.channelad.implement.ChannelAdReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdChannelService(
    private val adChannelReader: AdChannelReader,
    private val adChannelAppender: AdChannelAppender,
    private val adChannelManager: AdChannelManager,
    private val adChannelFormatManager: AdChannelFormatManager,
    private val channelAdReader: ChannelAdReader,
    private val clock: Clock,
) {

    /** 모든 플랫폼의 채널이다. 사용 안 함도 넣고 등록 순으로 둔다. */
    fun getChannels(): List<AdminAdChannelResult> = toResults(adChannelReader.readAll())

    fun getChannel(channelId: Long): AdminAdChannelResult = toResults(listOf(adChannelReader.read(channelId))).single()

    /** 채널을 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createChannel(command: AdminAdChannelCreateCommand): Long {
        val (platform, profile) = command.toDraft().toProfile { adChannelReader.existsHandle(it) }
        return adChannelAppender.append(AdChannelAppendDto(platform = platform, profile = profile)).requiredId()
    }

    /** 보낸 칸만 바꾼다. 검증은 저장된 값과 합친 결과로 한다. */
    @Transactional
    fun updateChannel(channelId: Long, command: AdminAdChannelUpdateCommand) {
        val channel = adChannelReader.readForUpdate(channelId)
        val (_, profile) = command.mergeInto(channel).toProfile { adChannelReader.existsHandle(it, exceptChannelId = channelId) }
        adChannelManager.change(channel, profile)
    }

    /** 채널 광고가 잡힌 채널은 지울 수 없다. 지우면 게재 유형의 올릴 수 있는 채널에서도 뺀다. 이미 지운 채널을 다시 지워도 성공한다. */
    @Transactional
    fun deleteChannel(channelId: Long) {
        val channel = adChannelReader.readForDelete(channelId)
        if (channel.deletedAt != null) {
            return
        }
        if (channelAdReader.existsByChannel(channelId)) {
            throw AdminConsoleBadRequestException("이 채널에 잡힌 채널 광고가 있어 지울 수 없습니다. 사용 안 함으로 바꿔 주세요.")
        }
        adChannelManager.delete(channel, LocalDateTime.now(clock))
        adChannelFormatManager.removeChannel(channelId)
    }

    private fun toResults(channels: List<AdChannel>): List<AdminAdChannelResult> {
        val adCounts = channelAdReader.countByChannels(channels.map { it.requiredId() })
        return channels.map { AdminAdChannelResult.of(it, adCounts.getValue(it.requiredId())) }
    }
}
