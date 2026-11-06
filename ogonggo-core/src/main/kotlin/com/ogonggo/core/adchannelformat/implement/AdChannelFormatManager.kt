package com.ogonggo.core.adchannelformat.implement

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.adchannelformat.domain.AdChannelFormatTerms
import com.ogonggo.core.adchannelformat.persistence.AdChannelFormatJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdChannelFormatManager internal constructor(
    private val adChannelFormatRepository: AdChannelFormatJpaRepository,
) {

    fun change(format: AdChannelFormat, terms: AdChannelFormatTerms) {
        format.change(terms)
        adChannelFormatRepository.save(format)
    }

    /**
     * 한 플랫폼의 순서를 [formatIds] 순서대로 1부터 매긴다. [formatIds]는 그 플랫폼의 유형 전부를 겹치지 않게 담아야 한다.
     * 서비스의 트랜잭션 안에서 부른다.
     */
    fun reorder(platform: AdChannelPlatform, formatIds: List<Long>) {
        val formats = adChannelFormatRepository.findAllByPlatformForUpdate(platform).associateBy { checkNotNull(it.id) }
        require(formatIds.size == formats.size && formatIds.toSet() == formats.keys) { "그 플랫폼의 유형을 빠짐없이 겹치지 않게 넘겨야 합니다." }
        formatIds.forEachIndexed { index, id -> formats.getValue(id).reorder(index + 1) }
        adChannelFormatRepository.saveAll(formats.values)
    }

    /** 유형을 지우고 같은 플랫폼의 남은 순서를 1부터 다시 매긴다. 이 유형을 산 계약에서 빼는 일은 계약 쪽이 한다. */
    fun delete(format: AdChannelFormat, now: LocalDateTime) {
        if (format.deletedAt != null) {
            return
        }
        format.delete(now)
        adChannelFormatRepository.saveAndFlush(format)
        val remaining = adChannelFormatRepository.findAllByPlatformForUpdate(format.platform)
        remaining.forEachIndexed { index, item -> item.reorder(index + 1) }
        adChannelFormatRepository.saveAll(remaining)
    }

    /** 지운 채널을 모든 유형의 올릴 수 있는 채널에서 뺀다. 남은 채널이 없으면 그 플랫폼의 모든 채널이 된다. */
    fun removeChannel(channelId: Long) {
        val formats = adChannelFormatRepository.findAllByChannelId(channelId)
        formats.forEach { it.removeChannel(channelId) }
        adChannelFormatRepository.saveAll(formats)
    }
}
