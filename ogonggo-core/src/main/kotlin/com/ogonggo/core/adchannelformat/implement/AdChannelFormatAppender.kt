package com.ogonggo.core.adchannelformat.implement

import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.adchannelformat.implement.dto.AdChannelFormatAppendDto
import com.ogonggo.core.adchannelformat.persistence.AdChannelFormatJpaRepository
import org.springframework.stereotype.Component

@Component
class AdChannelFormatAppender internal constructor(
    private val adChannelFormatRepository: AdChannelFormatJpaRepository,
) {

    /** 그 플랫폼의 맨 뒤 순서로 더한다. 순서가 겹치지 않도록 서비스의 트랜잭션 안에서 부른다. */
    fun append(dto: AdChannelFormatAppendDto): AdChannelFormat {
        val lastOrder = adChannelFormatRepository.findAllByPlatformForUpdate(dto.platform).maxOfOrNull { it.displayOrder } ?: 0
        return adChannelFormatRepository.save(AdChannelFormat(platform = dto.platform, terms = dto.terms, displayOrder = lastOrder + 1))
    }
}
