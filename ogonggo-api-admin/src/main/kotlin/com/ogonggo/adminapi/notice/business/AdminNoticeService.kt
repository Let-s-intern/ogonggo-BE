package com.ogonggo.adminapi.notice.business

import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.notice.domain.NoticeManagementSearchCondition
import com.ogonggo.core.notice.implement.NoticeAppender
import com.ogonggo.core.notice.implement.NoticeManager
import com.ogonggo.core.notice.implement.NoticeReader
import com.ogonggo.core.notice.implement.dto.NoticeAppendDto
import com.ogonggo.core.notice.implement.dto.NoticeUpdateDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminNoticeService(
    private val noticeReader: NoticeReader,
    private val noticeAppender: NoticeAppender,
    private val noticeManager: NoticeManager,
    private val contentValidator: LexicalEditorStateValidator,
    private val clock: Clock,
) {

    fun getNotices(condition: NoticeManagementSearchCondition, page: Int, size: Int): AdminNoticePageResult =
        AdminNoticePageResult.from(noticeReader.readManagementPage(condition, page, size))

    fun getNotice(noticeId: Long): AdminNoticeResult = AdminNoticeResult.from(noticeReader.read(noticeId))

    /** 생성된 공지의 식별자를 돌려준다. */
    @Transactional
    fun createNotice(command: AdminNoticeCreateCommand): Long {
        val notice = noticeAppender.append(
            NoticeAppendDto(
                title = command.title,
                content = contentValidator.validateAndSerialize(command.content),
                pinned = command.pinned,
                published = command.visibility.published,
            ),
        )
        return notice.requiredId()
    }

    @Transactional
    fun updateNotice(noticeId: Long, command: AdminNoticeUpdateCommand) {
        val content = command.content?.let(contentValidator::validateAndSerialize)
        noticeManager.update(
            noticeReader.readForUpdate(noticeId),
            NoticeUpdateDto(
                title = command.title,
                content = content,
                pinned = command.pinned,
                published = command.visibility?.published,
            ),
        )
    }

    @Transactional
    fun deleteNotice(noticeId: Long) {
        noticeManager.delete(noticeReader.readForDelete(noticeId), LocalDateTime.now(clock))
    }
}
