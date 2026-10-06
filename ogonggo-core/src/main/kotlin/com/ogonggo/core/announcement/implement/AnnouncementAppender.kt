package com.ogonggo.core.announcement.implement

import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.implement.dto.AnnouncementAppendDto
import com.ogonggo.core.announcement.persistence.AnnouncementJpaRepository
import org.springframework.stereotype.Component

@Component
class AnnouncementAppender internal constructor(
    private val announcementRepository: AnnouncementJpaRepository,
) {

    fun append(dto: AnnouncementAppendDto): Announcement =
        announcementRepository.save(
            Announcement(
                title = dto.title,
                content = dto.content,
                pinned = dto.pinned,
                published = dto.published,
            ),
        )
}
