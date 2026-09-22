package com.ogonggo.core.notice.implement

import com.ogonggo.core.notice.domain.Notice
import com.ogonggo.core.notice.implement.dto.NoticeAppendDto
import com.ogonggo.core.notice.persistence.NoticeJpaRepository
import org.springframework.stereotype.Component

@Component
class NoticeAppender internal constructor(
    private val noticeRepository: NoticeJpaRepository,
) {

    fun append(dto: NoticeAppendDto): Notice =
        noticeRepository.save(
            Notice(
                title = dto.title,
                content = dto.content,
                pinned = dto.pinned,
                published = dto.published,
            ),
        )
}
