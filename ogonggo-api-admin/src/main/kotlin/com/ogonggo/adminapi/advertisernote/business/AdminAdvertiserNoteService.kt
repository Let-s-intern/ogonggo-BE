package com.ogonggo.adminapi.advertisernote.business

import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisernote.domain.AdvertiserNote
import com.ogonggo.core.advertisernote.implement.AdvertiserNoteAppender
import com.ogonggo.core.advertisernote.implement.AdvertiserNoteManager
import com.ogonggo.core.advertisernote.implement.AdvertiserNoteReader
import com.ogonggo.core.advertisernote.implement.dto.AdvertiserNoteAppendDto
import com.ogonggo.core.advertisernote.implement.dto.AdvertiserNoteUpdateDto
import com.ogonggo.core.user.implement.UserProfileReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdvertiserNoteService(
    private val advertiserReader: AdvertiserReader,
    private val advertiserNoteReader: AdvertiserNoteReader,
    private val advertiserNoteAppender: AdvertiserNoteAppender,
    private val advertiserNoteManager: AdvertiserNoteManager,
    private val userProfileReader: UserProfileReader,
    private val clock: Clock,
) {

    /** 광고주 하나의 메모 전부다. 나중에 쓴 것이 먼저다. */
    fun getNotes(advertiserId: Long): List<AdminAdvertiserNoteResult> {
        advertiserReader.validateExists(advertiserId)
        return toResults(advertiserNoteReader.readAllByAdvertiser(advertiserId))
    }

    fun getNote(noteId: Long): AdminAdvertiserNoteResult = toResults(listOf(advertiserNoteReader.read(noteId))).single()

    /** 쓴 사람은 요청한 관리자다. 메모 식별자를 돌려준다. */
    @Transactional
    fun createNote(advertiserId: Long, adminUserId: Long, command: AdminAdvertiserNoteCreateCommand): Long {
        advertiserReader.validateExists(advertiserId)
        val title = validNoteTitle(command.title)
        val content = validNoteContent(command.content)
        return advertiserNoteAppender.append(
            AdvertiserNoteAppendDto(advertiserId = advertiserId, title = title, content = content, authorUserId = adminUserId),
        ).requiredId()
    }

    /** 보낸 칸만 바꾼다. 쓴 사람은 바꾸지 않는다. */
    @Transactional
    fun updateNote(noteId: Long, command: AdminAdvertiserNoteUpdateCommand) {
        val note = advertiserNoteReader.readForUpdate(noteId)
        val dto = AdvertiserNoteUpdateDto(
            title = command.title?.let(::validNoteTitle),
            content = command.content?.let(::validNoteContent),
        )
        advertiserNoteManager.update(note, dto)
    }

    /** 이미 지운 메모를 다시 지워도 성공한다. */
    @Transactional
    fun deleteNote(noteId: Long) {
        advertiserNoteManager.delete(advertiserNoteReader.readForDelete(noteId), LocalDateTime.now(clock))
    }

    private fun toResults(notes: List<AdvertiserNote>): List<AdminAdvertiserNoteResult> {
        val profiles = userProfileReader.readAll(notes.mapNotNull { it.authorUserId }.toSet())
        return notes.map { note -> AdminAdvertiserNoteResult.of(note, note.authorUserId?.let(profiles::get)?.name) }
    }
}
