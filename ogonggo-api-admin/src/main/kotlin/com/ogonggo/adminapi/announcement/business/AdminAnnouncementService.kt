package com.ogonggo.adminapi.announcement.business

import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.notificationbatch.implement.NotificationBatchCreator
import com.ogonggo.adminapi.notificationbatch.implement.dto.NotificationBatchCreateDto
import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.domain.AnnouncementPushTarget
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.implement.AnnouncementAppender
import com.ogonggo.core.announcement.implement.AnnouncementManager
import com.ogonggo.core.announcement.implement.AnnouncementReader
import com.ogonggo.core.announcement.implement.dto.AnnouncementAppendDto
import com.ogonggo.core.announcement.implement.dto.AnnouncementUpdateDto
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAnnouncementService(
    private val announcementReader: AnnouncementReader,
    private val announcementAppender: AnnouncementAppender,
    private val announcementManager: AnnouncementManager,
    private val imageAssetManager: ImageAssetManager,
    private val contentValidator: LexicalEditorStateValidator,
    private val notificationBatchCreator: NotificationBatchCreator,
    private val clock: Clock,
    /** 푸시를 누르면 열 사용자 웹 주소. 개발 환경은 설정으로 바꾼다. */
    @Value("\${ogonggo.announcement.push.web-base-url:https://www.ogonggo.co.kr}")
    private val webBaseUrl: String,
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
                pushTarget = command.pushTarget,
            ),
        )
        imageAssetManager.attachImageUrls(adminUserId, listOf(command.thumbnailUrl))
        pushIfDue(announcement, adminUserId)
        return announcement.requiredId()
    }

    @Transactional
    fun updateAnnouncement(announcementId: Long, command: AdminAnnouncementUpdateCommand, adminUserId: Long) {
        val content = command.content?.let(::validContent)
        val announcement = announcementReader.readForUpdate(announcementId)
        announcementManager.update(
            announcement,
            AnnouncementUpdateDto(
                title = command.title,
                content = content,
                pinned = command.pinned,
                published = command.visibility?.published,
                category = command.category,
                thumbnailUrl = command.thumbnailUrl,
                summary = command.summary,
                pushTarget = command.pushTarget,
            ),
        )
        imageAssetManager.attachImageUrls(adminUserId, listOf(command.thumbnailUrl))
        pushIfDue(announcement, adminUserId)
    }

    @Transactional
    fun deleteAnnouncement(announcementId: Long) {
        announcementManager.delete(announcementReader.readForDelete(announcementId), LocalDateTime.now(clock))
    }
    /**
     * 노출 중이고 푸시 대상이 정해졌는데 아직 보내지 않은 공지면 관리자 알림 발송과 같은 규칙으로 한 번 보낸다.
     * 발송 적재와 공지의 발송 기록은 같은 트랜잭션이다. 적재가 실패하면 공지 저장도 되돌린다 — 보냈다고 남기고 못 보내는 일을 막는다.
     */
    private fun pushIfDue(announcement: Announcement, adminUserId: Long) {
        if (!announcement.isPushDue()) return
        val announcementId = announcement.requiredId()
        val batch = notificationBatchCreator.create(
            adminUserId,
            NotificationBatchCreateDto(
                targetType = announcement.pushTarget.batchTargetType(),
                userIds = null,
                title = announcement.title.truncateCodePoints(PUSH_TITLE_MAX),
                body = announcement.summary ?: PUSH_DEFAULT_BODY,
                data = mapOf("url" to "${webBaseUrl.trimEnd('/')}/notices/$announcementId"),
            ),
        )
        announcementManager.markPushed(announcement, batch.batchId, LocalDateTime.now(clock))
    }

    /** 본문 형식 오류는 요청 필드 검증 오류로 알린다. */
    private fun validContent(content: String): String =
        contentValidator.validateAndSerialize(content) { reason -> throw InvalidRequestFieldException("content", reason) }

    companion object {
        /** 관리자 알림 발송의 제목 상한과 같다. */
        const val PUSH_TITLE_MAX = 100
        const val PUSH_DEFAULT_BODY = "새 공지사항이 올라왔어요. 눌러서 확인해 보세요."
    }
}

private fun AnnouncementPushTarget.batchTargetType(): NotificationBatchTargetType = when (this) {
    AnnouncementPushTarget.ALL -> NotificationBatchTargetType.ALL
    AnnouncementPushTarget.GENERAL_MEMBERS -> NotificationBatchTargetType.GENERAL_MEMBERS
    AnnouncementPushTarget.COMPANY_MEMBERS -> NotificationBatchTargetType.COMPANY_MEMBERS
    AnnouncementPushTarget.NONE -> error("보내지 않는 공지입니다.")
}

private fun String.truncateCodePoints(max: Int): String =
    if (codePointCount(0, length) <= max) this else substring(0, offsetByCodePoints(0, max))
