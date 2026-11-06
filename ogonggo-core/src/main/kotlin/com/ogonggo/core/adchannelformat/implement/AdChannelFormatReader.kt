package com.ogonggo.core.adchannelformat.implement

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.adchannelformat.error.AdChannelFormatErrorCode
import com.ogonggo.core.adchannelformat.persistence.AdChannelFormatJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class AdChannelFormatReader internal constructor(
    private val adChannelFormatRepository: AdChannelFormatJpaRepository,
) {

    fun read(formatId: Long): AdChannelFormat =
        adChannelFormatRepository.findByIdAndDeletedAtIsNull(formatId)
            ?: throw EntityNotFoundException(AdChannelFormatErrorCode.AD_CHANNEL_FORMAT_NOT_FOUND)

    /** 없거나 지운 유형이면 null이다. */
    fun readOrNull(formatId: Long): AdChannelFormat? = adChannelFormatRepository.findByIdAndDeletedAtIsNull(formatId)

    /** 모든 플랫폼의 유형이다. 사용 안 함도 넣고 플랫폼 순(선언 순서), 그 안에서 순서대로 둔다. */
    fun readAll(): List<AdChannelFormat> =
        adChannelFormatRepository.findAllByDeletedAtIsNull().sortedWith(DISPLAY_ORDER)

    /** 식별자마다 유형을 둔다. 없거나 지운 유형은 결과에 없다. */
    fun readAllByIds(formatIds: Collection<Long>): Map<Long, AdChannelFormat> =
        if (formatIds.isEmpty()) {
            emptyMap()
        } else {
            adChannelFormatRepository.findAllByIdInAndDeletedAtIsNull(formatIds).associateBy { checkNotNull(it.id) }
        }

    /** 같은 플랫폼에 같은 이름(앞뒤 공백을 뺀 그대로)의 유형이 있는지 본다. 지운 유형은 보지 않는다. */
    fun existsName(platform: AdChannelPlatform, name: String, exceptFormatId: Long? = null): Boolean =
        adChannelFormatRepository.existsByPlatformAndNameAndDeletedAtIsNullAndIdNot(platform, name.trim(), exceptFormatId ?: 0)

    /** 한 플랫폼의 유형을 순서대로 잠가 읽는다. 순서를 바꾸거나 맨 뒤에 더할 때 서비스의 트랜잭션 안에서 부른다. */
    fun readAllByPlatformForUpdate(platform: AdChannelPlatform): List<AdChannelFormat> =
        adChannelFormatRepository.findAllByPlatformForUpdate(platform)

    /** 수정할 유형을 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(formatId: Long): AdChannelFormat =
        adChannelFormatRepository.findForUpdate(formatId)
            ?: throw EntityNotFoundException(AdChannelFormatErrorCode.AD_CHANNEL_FORMAT_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 유형도 잠가 찾는다. */
    fun readForDelete(formatId: Long): AdChannelFormat =
        adChannelFormatRepository.findIncludingDeletedForUpdate(formatId)
            ?: throw EntityNotFoundException(AdChannelFormatErrorCode.AD_CHANNEL_FORMAT_NOT_FOUND)

    private companion object {
        val DISPLAY_ORDER: Comparator<AdChannelFormat> =
            compareBy<AdChannelFormat>({ it.platform.ordinal }, { it.displayOrder }, { it.id })
    }
}
