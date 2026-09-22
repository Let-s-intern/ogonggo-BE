package com.ogonggo.core.notice.implement

import com.ogonggo.core.notice.domain.Notice
import com.ogonggo.core.notice.implement.dto.NoticeUpdateDto
import com.ogonggo.core.notice.persistence.NoticeJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class NoticeManager internal constructor(
    private val noticeRepository: NoticeJpaRepository,
) {

    /** 넘어온 값만 바꾼다. */
    fun update(notice: Notice, dto: NoticeUpdateDto) {
        if (dto.title != null || dto.content != null) {
            notice.edit(title = dto.title, content = dto.content)
        }
        dto.pinned?.let(notice::pin)
        when (dto.published) {
            true -> notice.publish()
            false -> notice.hide()
            null -> Unit
        }
        noticeRepository.save(notice)
    }

    fun delete(notice: Notice, now: LocalDateTime) {
        notice.delete(now)
        noticeRepository.save(notice)
    }
}
