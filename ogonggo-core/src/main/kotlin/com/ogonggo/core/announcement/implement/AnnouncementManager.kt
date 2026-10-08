package com.ogonggo.core.announcement.implement

import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.implement.dto.AnnouncementUpdateDto
import com.ogonggo.core.announcement.persistence.AnnouncementJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class AnnouncementManager internal constructor(
    private val announcementRepository: AnnouncementJpaRepository,
) {

    /** 넘어온 값만 바꾼다. */
    fun update(announcement: Announcement, dto: AnnouncementUpdateDto) {
        if (dto.title != null || dto.content != null) {
            announcement.edit(title = dto.title, content = dto.content)
        }
        dto.pinned?.let(announcement::pin)
        when (dto.published) {
            true -> announcement.publish()
            false -> announcement.hide()
            null -> Unit
        }
        announcementRepository.save(announcement)
    }

    fun delete(announcement: Announcement, now: LocalDateTime) {
        announcement.delete(now)
        announcementRepository.save(announcement)
    }
}
