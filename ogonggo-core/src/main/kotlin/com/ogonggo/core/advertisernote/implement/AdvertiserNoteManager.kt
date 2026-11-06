package com.ogonggo.core.advertisernote.implement

import com.ogonggo.core.advertisernote.domain.AdvertiserNote
import com.ogonggo.core.advertisernote.implement.dto.AdvertiserNoteUpdateDto
import com.ogonggo.core.advertisernote.persistence.AdvertiserNoteJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertiserNoteManager internal constructor(
    private val advertiserNoteRepository: AdvertiserNoteJpaRepository,
) {

    fun update(note: AdvertiserNote, dto: AdvertiserNoteUpdateDto) {
        note.edit(title = dto.title, content = dto.content)
        advertiserNoteRepository.save(note)
    }

    fun delete(note: AdvertiserNote, now: LocalDateTime) {
        note.delete(now)
        advertiserNoteRepository.save(note)
    }
}
