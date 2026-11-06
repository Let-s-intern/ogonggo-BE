package com.ogonggo.core.adchannel.implement

import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.error.AdChannelErrorCode
import com.ogonggo.core.adchannel.persistence.AdChannelJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class AdChannelReader internal constructor(
    private val adChannelRepository: AdChannelJpaRepository,
) {

    fun read(channelId: Long): AdChannel =
        adChannelRepository.findByIdAndDeletedAtIsNull(channelId)
            ?: throw EntityNotFoundException(AdChannelErrorCode.AD_CHANNEL_NOT_FOUND)

    /** 없거나 지운 채널이면 null이다. */
    fun readOrNull(channelId: Long): AdChannel? = adChannelRepository.findByIdAndDeletedAtIsNull(channelId)

    /** 모든 플랫폼의 채널이다. 사용 안 함도 넣고 등록 순으로 둔다. */
    fun readAll(): List<AdChannel> = adChannelRepository.findAllByDeletedAtIsNullOrderByIdAsc()

    /** 식별자마다 채널을 둔다. 없거나 지운 채널은 결과에 없다. */
    fun readAllByIds(channelIds: Collection<Long>): Map<Long, AdChannel> =
        if (channelIds.isEmpty()) {
            emptyMap()
        } else {
            adChannelRepository.findAllByIdInAndDeletedAtIsNull(channelIds).associateBy { checkNotNull(it.id) }
        }

    /** 대소문자를 무시하고 같은 계정 이름의 채널이 있는지 본다. 지운 채널은 보지 않는다. */
    fun existsHandle(handle: String, exceptChannelId: Long? = null): Boolean =
        adChannelRepository.existsHandle(handle, exceptChannelId)

    /** 수정할 채널을 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(channelId: Long): AdChannel =
        adChannelRepository.findForUpdate(channelId)
            ?: throw EntityNotFoundException(AdChannelErrorCode.AD_CHANNEL_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 채널도 잠가 찾는다. */
    fun readForDelete(channelId: Long): AdChannel =
        adChannelRepository.findIncludingDeletedForUpdate(channelId)
            ?: throw EntityNotFoundException(AdChannelErrorCode.AD_CHANNEL_NOT_FOUND)
}
