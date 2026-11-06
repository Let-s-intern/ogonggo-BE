package com.ogonggo.core.advertisernote.implement

import com.ogonggo.core.advertisernote.domain.AdvertiserNote
import com.ogonggo.core.advertisernote.error.AdvertiserNoteErrorCode
import com.ogonggo.core.advertisernote.persistence.AdvertiserNoteJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class AdvertiserNoteReader internal constructor(
    private val advertiserNoteRepository: AdvertiserNoteJpaRepository,
) {

    fun read(noteId: Long): AdvertiserNote =
        advertiserNoteRepository.findByIdAndDeletedAtIsNull(noteId)
            ?: throw EntityNotFoundException(AdvertiserNoteErrorCode.ADVERTISER_NOTE_NOT_FOUND)

    /** 광고주 하나의 메모다. 나중에 쓴 것이 먼저다. */
    fun readAllByAdvertiser(advertiserId: Long): List<AdvertiserNote> =
        advertiserNoteRepository.findAllByAdvertiserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(advertiserId)

    /** 수정할 메모를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(noteId: Long): AdvertiserNote =
        advertiserNoteRepository.findForUpdate(noteId)
            ?: throw EntityNotFoundException(AdvertiserNoteErrorCode.ADVERTISER_NOTE_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 메모도 잠가 찾는다. */
    fun readForDelete(noteId: Long): AdvertiserNote =
        advertiserNoteRepository.findIncludingDeletedForUpdate(noteId)
            ?: throw EntityNotFoundException(AdvertiserNoteErrorCode.ADVERTISER_NOTE_NOT_FOUND)
}
