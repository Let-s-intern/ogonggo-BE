package com.ogonggo.core.advertisernote.persistence

import com.ogonggo.core.advertisernote.domain.AdvertiserNote
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface AdvertiserNoteJpaRepository : JpaRepository<AdvertiserNote, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdvertiserNote?

    fun findAllByAdvertiserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(advertiserId: Long): List<AdvertiserNote>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from AdvertiserNote n where n.id = :noteId and n.deletedAt is null")
    fun findForUpdate(@Param("noteId") noteId: Long): AdvertiserNote?

    /** 삭제는 멱등해야 하므로 이미 삭제된 메모도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from AdvertiserNote n where n.id = :noteId")
    fun findIncludingDeletedForUpdate(@Param("noteId") noteId: Long): AdvertiserNote?
}
