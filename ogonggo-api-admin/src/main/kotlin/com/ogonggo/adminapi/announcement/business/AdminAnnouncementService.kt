package com.ogonggo.adminapi.announcement.business

import com.ogonggo.core.image.implement.ImageAssetManager
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
    private val imageAssetManager: ImageAssetManager,
    private val contentValidator: LexicalEditorStateValidator,
    private val clock: Clock,
) {

    fun getAnnouncements(condition: AnnouncementManagementSearchCondition, page: Int, size: Int): AdminAnnouncementPageResult =
        AdminAnnouncementPageResult.from(announcementReader.readManagementPage(condition, page, size))

    fun getAnnouncement(announcementId: Long): AdminAnnouncementResult = AdminAnnouncementResult.from(announcementReader.read(announcementId))

    /**
     * 생성된 공지의 식별자를 돌려준다. 썸네일로 올린 이미지는 첨부해 정리 작업이 지우지 않게 한다. 관리자 콘솔은 사용자 API 로
     * 이미지를 올리므로 올린 사람은 관리자 본인이다(부트캠프 수정과 같다).
     */
    @Transactional
    fun createAnnouncement(command: AdminAnnouncementCreateCommand, adminUserId: Long): Long {
        val announcement = announcementAppender.append(
            AnnouncementAppendDto(
                title = command.title,
                content = validContent(command.content),
                pinned = command.pinned,
                published = command.visibility.published,
                category = command.category,
                thumbnailUrl = command.thumbnailUrl,
                summary = command.summary,
            ),
        )
        imageAssetManager.attachImageUrls(adminUserId, listOf(command.thumbnailUrl))
        return announcement.requiredId()
    }

    @Transactional
    fun updateAnnouncement(announcementId: Long, command: AdminAnnouncementUpdateCommand, adminUserId: Long) {
        val content = command.content?.let(::validContent)
        announcementManager.update(
            announcementReader.readForUpdate(announcementId),
            AnnouncementUpdateDto(
                title = command.title,
                content = content,
                pinned = command.pinned,
                published = command.visibility?.published,
                category = command.category,
                thumbnailUrl = command.thumbnailUrl,
                summary = command.summary,
            ),
        )
        imageAssetManager.attachImageUrls(adminUserId, listOf(command.thumbnailUrl))
    }

    @Transactional
    fun deleteAnnouncement(announcementId: Long) {
        announcementManager.delete(announcementReader.readForDelete(announcementId), LocalDateTime.now(clock))
    }
    /** 본문 형식 오류는 요청 필드 검증 오류로 알린다. */
    private fun validContent(content: String): String =
        contentValidator.validateAndSerialize(content) { reason -> throw InvalidRequestFieldException("content", reason) }

}
