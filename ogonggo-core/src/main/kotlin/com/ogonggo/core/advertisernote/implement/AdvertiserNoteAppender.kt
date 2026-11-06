package com.ogonggo.core.advertisernote.implement

import com.ogonggo.core.advertisernote.domain.AdvertiserNote
import com.ogonggo.core.advertisernote.implement.dto.AdvertiserNoteAppendDto
import com.ogonggo.core.advertisernote.persistence.AdvertiserNoteJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertiserNoteAppender internal constructor(
    private val advertiserNoteRepository: AdvertiserNoteJpaRepository,
) {

    fun append(dto: AdvertiserNoteAppendDto): AdvertiserNote =
        advertiserNoteRepository.save(
            AdvertiserNote(
                advertiserId = dto.advertiserId,
                title = dto.title,
                content = dto.content,
                authorUserId = dto.authorUserId,
            ),
        )
}
