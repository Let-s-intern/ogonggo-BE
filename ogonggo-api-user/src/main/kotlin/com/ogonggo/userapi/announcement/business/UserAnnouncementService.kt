package com.ogonggo.userapi.announcement.business

import com.ogonggo.core.announcement.implement.AnnouncementReader
import org.springframework.stereotype.Service

@Service
class UserAnnouncementService(
    private val announcementReader: AnnouncementReader,
) {

    fun getAnnouncements(page: Int, size: Int): UserAnnouncementPageResult =
        UserAnnouncementPageResult.from(announcementReader.readPublicPage(page, size))

    fun getAnnouncement(announcementId: Long): UserAnnouncementResult = UserAnnouncementResult.from(announcementReader.readPublic(announcementId))
}
