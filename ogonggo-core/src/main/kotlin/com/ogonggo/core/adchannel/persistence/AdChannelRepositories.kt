package com.ogonggo.core.adchannel.persistence

import com.ogonggo.core.adchannel.domain.AdChannel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface AdChannelJpaRepository : JpaRepository<AdChannel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdChannel?

    fun findAllByDeletedAtIsNullOrderByIdAsc(): List<AdChannel>

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<AdChannel>

    @Query(
        "select case when count(c) > 0 then true else false end from AdChannel c " +
            "where lower(c.handle) = lower(:handle) and c.deletedAt is null and (:exceptId is null or c.id <> :exceptId)",
    )
    fun existsHandle(@Param("handle") handle: String, @Param("exceptId") exceptId: Long?): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdChannel c where c.id = :channelId and c.deletedAt is null")
    fun findForUpdate(@Param("channelId") channelId: Long): AdChannel?

    /** 삭제는 멱등해야 하므로 이미 삭제된 채널도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdChannel c where c.id = :channelId")
    fun findIncludingDeletedForUpdate(@Param("channelId") channelId: Long): AdChannel?
}
