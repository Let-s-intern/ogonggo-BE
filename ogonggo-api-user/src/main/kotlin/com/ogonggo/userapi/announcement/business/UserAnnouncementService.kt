package com.ogonggo.userapi.announcement.business

import com.ogonggo.core.announcement.domain.AnnouncementPublicSearchCondition
import com.ogonggo.core.announcement.implement.AnnouncementReader
import org.springframework.stereotype.Service

@Service
class UserAnnouncementService(
    private val announcementReader: AnnouncementReader,
) {

    fun getAnnouncements(condition: AnnouncementPublicSearchCondition, page: Int, size: Int): UserAnnouncementPageResult =
        UserAnnouncementPageResult.from(announcementReader.readPublicPage(condition, page, size))

    fun getAnnouncement(announcementId: Long): UserAnnouncementResult = UserAnnouncementResult.from(announcementReader.readPublic(announcementId))

    /** 공지사항 화면의 `전체 카테고리` 칸들. 칸마다 최근 공지 [RECENT_LIMIT]건을 싣는다. */
    fun getCategories(): List<UserAnnouncementCategoryResult> =
        announcementReader.readPublicCategoryDigests(RECENT_LIMIT).map(UserAnnouncementCategoryResult::from)

    companion object {
        const val RECENT_LIMIT = 3
    }
}
