package com.ogonggo.userapi.notice.business

import com.ogonggo.core.notice.implement.NoticeReader
import org.springframework.stereotype.Service

@Service
class UserNoticeService(
    private val noticeReader: NoticeReader,
) {

    fun getNotices(page: Int, size: Int): UserNoticePageResult =
        UserNoticePageResult.from(noticeReader.readPublicPage(page, size))

    fun getNotice(noticeId: Long): UserNoticeResult = UserNoticeResult.from(noticeReader.readPublic(noticeId))
}
