package com.ogonggo.adminapi.adchannelformat.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.implement.AdChannelReader
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatAppender
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatManager
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatReader
import com.ogonggo.core.adchannelformat.implement.dto.AdChannelFormatAppendDto
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractManager
import com.ogonggo.core.channelad.implement.ChannelAdReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdChannelFormatService(
    private val adChannelFormatReader: AdChannelFormatReader,
    private val adChannelFormatAppender: AdChannelFormatAppender,
    private val adChannelFormatManager: AdChannelFormatManager,
    private val adChannelReader: AdChannelReader,
    private val advertiserContractManager: AdvertiserContractManager,
    private val channelAdReader: ChannelAdReader,
    private val clock: Clock,
) {

    /** 모든 플랫폼의 유형이다. 사용 안 함도 넣고 플랫폼 순, 그 안에서 순서대로 둔다. */
    fun getFormats(): List<AdminAdChannelFormatResult> = toResults(adChannelFormatReader.readAll())

    fun getFormat(formatId: Long): AdminAdChannelFormatResult = toResults(listOf(adChannelFormatReader.read(formatId))).single()

    /** 그 플랫폼의 맨 뒤 순서로 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createFormat(command: AdminAdChannelFormatCreateCommand): Long {
        val (platform, terms) = command.toDraft().toTerms(
            nameTaken = { platform, name -> adChannelFormatReader.existsName(platform, name) },
            channelPlatforms = channelPlatforms(),
        )
        return adChannelFormatAppender.append(AdChannelFormatAppendDto(platform = platform, terms = terms)).requiredId()
    }

    /** 보낸 칸만 바꾼다. 검증은 저장된 값과 합친 결과로 한다. */
    @Transactional
    fun updateFormat(formatId: Long, command: AdminAdChannelFormatUpdateCommand) {
        val format = adChannelFormatReader.readForUpdate(formatId)
        val (_, terms) = command.mergeInto(format).toTerms(
            nameTaken = { platform, name -> adChannelFormatReader.existsName(platform, name, exceptFormatId = formatId) },
            channelPlatforms = channelPlatforms(),
        )
        adChannelFormatManager.change(format, terms)
    }

    /** 한 플랫폼의 순서를 통째로 바꾼다. 그 플랫폼의 유형 id를 빠짐없이, 겹치지 않게 받아야 한다. */
    @Transactional
    fun reorderFormats(command: AdminAdChannelFormatOrderCommand) {
        val platform = command.platform ?: throw platformError()
        val current = adChannelFormatReader.readAllByPlatformForUpdate(platform).map { it.requiredId() }
        adChannelFormatManager.reorder(platform, command.validFormatIds(current))
    }

    /**
     * 채널 광고가 잡힌 유형은 지울 수 없다. 지우면 같은 플랫폼의 남은 순서를 1부터 다시 매기고 이 유형을 산 계약에서도 뺀다.
     * 이미 지운 유형을 다시 지워도 성공한다.
     */
    @Transactional
    fun deleteFormat(formatId: Long) {
        val format = adChannelFormatReader.readForDelete(formatId)
        if (format.deletedAt != null) {
            return
        }
        if (channelAdReader.existsByFormat(formatId)) {
            throw AdminConsoleBadRequestException("이 유형으로 잡힌 채널 광고가 있어 지울 수 없습니다. 사용 안 함으로 바꿔 주세요.")
        }
        adChannelFormatManager.delete(format, LocalDateTime.now(clock))
        advertiserContractManager.removeChannelFormat(formatId)
    }

    private fun channelPlatforms() = adChannelReader.readAll().associate { checkNotNull(it.id) to it.platform }

    private fun toResults(formats: List<AdChannelFormat>): List<AdminAdChannelFormatResult> {
        val adCounts = channelAdReader.countByFormats(formats.map { it.requiredId() })
        return formats.map { AdminAdChannelFormatResult.of(it, adCounts.getValue(it.requiredId())) }
    }
}
