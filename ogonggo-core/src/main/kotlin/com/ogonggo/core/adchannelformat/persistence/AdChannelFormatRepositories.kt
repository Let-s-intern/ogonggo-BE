package com.ogonggo.core.adchannelformat.persistence

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface AdChannelFormatJpaRepository : JpaRepository<AdChannelFormat, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdChannelFormat?

    fun findAllByDeletedAtIsNull(): List<AdChannelFormat>

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<AdChannelFormat>

    fun existsByPlatformAndNameAndDeletedAtIsNullAndIdNot(platform: AdChannelPlatform, name: String, id: Long): Boolean

    /** 지운 채널을 올릴 수 있는 채널로 둔 게재 유형이다. */
    @Query("select distinct f from AdChannelFormat f join f.channelIds c where c = :channelId and f.deletedAt is null")
    fun findAllByChannelId(@Param("channelId") channelId: Long): List<AdChannelFormat>

    /** 순서를 다시 매기려고 한 플랫폼의 유형을 잠가 읽는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from AdChannelFormat f where f.platform = :platform and f.deletedAt is null order by f.displayOrder asc, f.id asc")
    fun findAllByPlatformForUpdate(@Param("platform") platform: AdChannelPlatform): List<AdChannelFormat>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from AdChannelFormat f where f.id = :formatId and f.deletedAt is null")
    fun findForUpdate(@Param("formatId") formatId: Long): AdChannelFormat?

    /** 삭제는 멱등해야 하므로 이미 삭제된 유형도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from AdChannelFormat f where f.id = :formatId")
    fun findIncludingDeletedForUpdate(@Param("formatId") formatId: Long): AdChannelFormat?
}
