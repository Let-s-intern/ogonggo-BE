package com.ogonggo.adminapi.announcement.business

import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.implement.AnnouncementAppender
import com.ogonggo.core.announcement.implement.AnnouncementManager
import com.ogonggo.core.announcement.implement.AnnouncementReader
import com.ogonggo.core.announcement.implement.dto.AnnouncementAppendDto
import com.ogonggo.core.announcement.implement.dto.AnnouncementUpdateDto
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAnnouncementService(
    private val announcementReader: AnnouncementReader,
    private val announcementAppender: AnnouncementAppender,
    private val announcementManager: AnnouncementManager,
    private val contentValidator: LexicalEditorStateValidator,
    private val clock: Clock,
) {

    fun getAnnouncements(condition: AnnouncementManagementSearchCondition, page: Int, size: Int): AdminAnnouncementPageResult =
        AdminAnnouncementPageResult.from(announcementReader.readManagementPage(condition, page, size))

    fun getAnnouncement(announcementId: Long): AdminAnnouncementResult = AdminAnnouncementResult.from(announcementReader.read(announcementId))

    /** 생성된 공지의 식별자를 돌려준다. */
    @Transactional
    fun createAnnouncement(command: AdminAnnouncementCreateCommand): Long {
        val announcement = announcementAppender.append(
            AnnouncementAppendDto(
                title = command.title,
                content = validContent(command.content),
                pinned = command.pinned,
                published = command.visibility.published,
            ),
        )
        return announcement.requiredId()
    }

    @Transactional
    fun updateAnnouncement(announcementId: Long, command: AdminAnnouncementUpdateCommand) {
        val content = command.content?.let(::validContent)
        announcementManager.update(
            announcementReader.readForUpdate(announcementId),
            AnnouncementUpdateDto(
                title = command.title,
                content = content,
                pinned = command.pinned,
                published = command.visibility?.published,
            ),
        )
    }

    @Transactional
    fun deleteAnnouncement(announcementId: Long) {
        announcementManager.delete(announcementReader.readForDelete(announcementId), LocalDateTime.now(clock))
    }
    /** 본문 형식 오류는 요청 필드 검증 오류로 알린다. */
    private fun validContent(content: String): String =
        contentValidator.validateAndSerialize(content) { reason -> throw InvalidRequestFieldException("content", reason) }

}
