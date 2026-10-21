package com.ogonggo.core.userfcmtoken.persistence

import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface UserFcmTokenJpaRepository : JpaRepository<UserFcmToken, Long> {
    fun findAllByUserIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(userId: Long): List<UserFcmToken>

    fun findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(userId: Long): UserFcmToken?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select t from UserFcmToken t
        where t.deletedAt is null and (t.activeSlot = :slot or t.tokenHash = :hash)
        order by t.id
        """,
    )
    fun findBindingsForUpdate(@Param("slot") slot: String, @Param("hash") hash: String): List<UserFcmToken>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        update UserFcmToken t
        set t.token = null, t.tokenHash = null, t.activeSlot = null, t.deletedAt = :now, t.updatedAt = :now
        where t.userId = :userId and t.tokenHash = :hash and t.deletedAt is null
        """,
    )
    fun revoke(@Param("userId") userId: Long, @Param("hash") hash: String, @Param("now") now: LocalDateTime): Int

    @Query(
        """
        select count(t) > 0 from UserFcmToken t, User u
        where t.userId = :userId and t.tokenHash = :hash and t.deletedAt is null
          and u.id = t.userId and u.status = com.ogonggo.core.user.domain.UserStatus.ACTIVE
        """,
    )
    fun isDeliverable(@Param("userId") userId: Long, @Param("hash") hash: String): Boolean
}
