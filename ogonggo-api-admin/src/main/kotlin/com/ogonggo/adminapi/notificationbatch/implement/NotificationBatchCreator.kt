package com.ogonggo.adminapi.notificationbatch.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.notificationbatch.implement.dto.NotificationBatchCreateDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.NotificationBatchAppender
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchAppendDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchDto
import com.ogonggo.core.user.domain.LetsCareerWishJobCatalog
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.GeneralMemberAudienceReader
import com.ogonggo.core.user.implement.dto.GeneralMemberAudienceDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenRecipientReader
import com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.UserNotificationAppender
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import java.net.URI
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * 관리자 FCM 발송 요청을 대상 회원·토큰 또는 토픽으로 펼쳐 적재한다.
 *
 * 관리자 알림 발송(`AdminNotificationBatchService`)과 공지 노출 푸시(`AdminAnnouncementService`)가 같은 규칙으로 보내도록
 * 한곳에 둔다. 트랜잭션은 부르는 Business Service가 연다.
 */
@Component
class NotificationBatchCreator(
    private val recipients: UserFcmTokenRecipientReader,
    private val notifications: NotificationAppender,
    private val batches: NotificationBatchAppender,
    private val inbox: UserNotificationAppender,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
    private val broadcast: NotificationBroadcastProperties,
    private val generalMembers: GeneralMemberAudienceReader,
    private val marketingAgreements: LetsCareerMarketingAgreementClient,
    private val images: ImageAssetManager,
) {
    /**
     * 요청 하나를 발송 묶음으로 적재한다. 알림함·발송 적재·요청 기록을 부르는 쪽의 트랜잭션에서 함께 커밋한다.
     * provider 호출은 사용자 API의 기존 dispatcher가 맡는다.
     */
    fun create(adminUserId: Long, command: NotificationBatchCreateDto): NotificationBatchDto {
        val requestedUserIds = validateTargets(command)
        validateData(command.data)
        val imageUrl = command.imageId?.let { images.replaceImage(adminUserId, null, it, LocalDateTime.now(clock), ImageAssetUsage.NOTIFICATION) }
        val withImage = command.copy(imageUrl = imageUrl)
        // 토픽 발송 게이트가 켜진 대상(전체·일반회원·기업회원)은 FCM 토픽 한 건으로 보낸다. 꺼져 있으면 기기마다 보낸다.
        // 광고성은 동의자만 골라야 하는데 토픽은 받는 사람을 거르지 못하므로 항상 기기마다 보낸다.
        if (!command.advertisement) {
            broadcast.sendingTopicFor(command.targetType)?.let { topic -> return createBroadcast(adminUserId, withImage, topic) }
        }
        val audience = resolveAudience(command, requestedUserIds)
        notificationPayload(withImage, command.data)
        val previewLength = minOf(100, command.body.codePointCount(0, command.body.length))
        val preview = command.body.substring(0, command.body.offsetByCodePoints(0, previewLength))

        val now = LocalDateTime.now(clock)
        val expiresAt = now.plusHours(24)
        val batchId = UUID.randomUUID().toString()
        val throughUserId = recipients.readUpperUserId()
        var afterUserId = 0L
        var targetCount = 0L
        var recipientCount = 0L
        var enqueuedCount = 0L
        var noTokenCount = 0L
        var inactiveCount = 0L
        // 고른 회원이 한 명도 없으면(희망 직무가 맞는 회원 없음, 동의자 없음) 발송 없이 기록만 남긴다.
        while (audience.userIds?.isEmpty() != true) {
            val page = recipients.readPage(afterUserId, throughUserId, audience.userIds, 500, audience.roles)
            if (page.isEmpty()) break
            targetCount += page.size
            val pending = page.flatMap { recipient ->
                when {
                    recipient.status != UserStatus.ACTIVE -> { inactiveCount++; emptyList() }
                    else -> {
                        val inboxId = checkNotNull(inbox.append(UserNotificationAppendDto(
                            recipientUserId = recipient.userId,
                            type = UserNotificationType.ADMIN_NOTIFICATION,
                            actorUserId = adminUserId, postId = null, commentId = null,
                            actorNickname = "오공고", actorProfileImageUrl = null,
                            postTitle = command.title, preview = preview,
                            pushSkipReason = if (recipient.tokens.isEmpty()) UserNotificationPushSkipReason.NO_ACTIVE_TOKEN else null,
                            batchId = batchId, url = command.data.getValue("url"),
                        )))
                        // 요청 data가 시스템의 알림함 ID·종류를 덮어쓰지 못하게 한다.
                        val data = (command.data - setOf("source", "batchId")) + mapOf("notificationId" to inboxId.toString(), "type" to UserNotificationType.ADMIN_NOTIFICATION.name)
                        val payload = notificationPayload(withImage, data)
                        if (recipient.tokens.isEmpty()) noTokenCount++ else recipientCount++
                        recipient.tokens.map { token -> NotificationAppendDto(
                            deduplicationKey = "admin-batch:$batchId:token:${token.id}",
                            channel = NotificationChannel.FCM, templateCode = "admin_notification",
                            recipientUserId = recipient.userId, recipientAddress = token.token,
                            payloadJson = payload, scheduledAt = now, batchId = batchId, dispatchExpiresAt = expiresAt,
                            userNotificationId = inboxId,
                        ) }
                    }
                }
            }
            enqueuedCount += notifications.appendAll(pending)
            afterUserId = page.last().userId
        }
        if (audience.checkAllRequestedExist && requestedUserIds != null && targetCount != requestedUserIds.size.toLong()) {
            // 이전 페이지를 적재했더라도 요청 전체가 롤백되어 부분 발송은 생기지 않는다.
            throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)
        }
        return batches.append(NotificationBatchAppendDto(
            batchId = batchId, requestedByUserId = adminUserId, targetType = command.targetType,
            requestedUserIdsJson = requestedUserIds?.let { objectMapper.writeValueAsString(it.sorted()) },
            title = command.title, body = command.body, dataJson = objectMapper.writeValueAsString(command.data),
            imageId = command.imageId, imageUrl = imageUrl,
            targetUserCount = targetCount + audience.skippedInactiveCount + audience.skippedNoConsentCount,
            recipientUserCount = recipientCount, enqueuedCount = enqueuedCount,
            skippedNoTokenUserCount = noTokenCount, skippedInactiveUserCount = inactiveCount + audience.skippedInactiveCount,
            dispatchExpiresAt = expiresAt,
            targetJobRolesJson = command.jobRoles?.let { objectMapper.writeValueAsString(it.distinct()) },
            advertisement = command.advertisement,
            skippedNoMarketingConsentUserCount = audience.skippedNoConsentCount,
        ))
    }

    /**
     * 기기마다 보낼 회원을 정한다. userIds가 있으면 그 회원만, 없으면 roles 역할의 활성 회원 전체를 읽는다.
     * 광고성은 대상 중 렛츠커리어 마케팅 수신 동의자만 남긴다. 기업 회원은 동의 항목이 없어 동의하지 않은 것으로 센다.
     */
    private fun resolveAudience(command: NotificationBatchCreateDto, requestedUserIds: Set<Long>?): DirectAudience {
        val jobRoles = command.jobRoles.orEmpty()
        if (!command.advertisement) {
            return when (command.targetType) {
                NotificationBatchTargetType.USER_IDS -> DirectAudience(userIds = requestedUserIds, checkAllRequestedExist = true)
                NotificationBatchTargetType.JOB_ROLES -> DirectAudience(userIds = generalMembers.readActive()
                    .filter { LetsCareerWishJobCatalog.matches(it.wishJob, jobRoles) }.mapTo(linkedSetOf()) { it.userId })
                else -> DirectAudience(roles = audienceRoles(command.targetType))
            }
        }
        val activeGeneral = generalMembers.readActive()
        var skippedInactive = 0L
        val candidates: List<GeneralMemberAudienceDto>
        var activeCompanyCount = 0L
        when (command.targetType) {
            NotificationBatchTargetType.USER_IDS -> {
                val statuses = readStatuses(checkNotNull(requestedUserIds))
                skippedInactive = statuses.count { it.value != UserStatus.ACTIVE }.toLong()
                val activeIds = statuses.filterValues { it == UserStatus.ACTIVE }.keys
                candidates = activeGeneral.filter { it.userId in activeIds }
                activeCompanyCount = activeIds.size - candidates.size.toLong()
            }
            NotificationBatchTargetType.JOB_ROLES -> candidates = activeGeneral.filter { LetsCareerWishJobCatalog.matches(it.wishJob, jobRoles) }
            // 전체 발송도 광고성이면 동의 항목이 있는 일반 회원만 대상이다. 기업 회원 수는 동의하지 않은 것으로 센다.
            NotificationBatchTargetType.ALL -> {
                candidates = activeGeneral
                activeCompanyCount = recipients.countActiveAudience(setOf(UserRole.COMPANY)).first
            }
            NotificationBatchTargetType.GENERAL_MEMBERS -> candidates = activeGeneral
            NotificationBatchTargetType.COMPANY_MEMBERS -> error("기업 회원 광고성 발송은 요청 검증에서 막는다.")
        }
        val agreed = marketingAgreements.readAgreedUserIds(candidates.mapNotNull { it.letsCareerUserId })
        val consenting = candidates.filter { it.letsCareerUserId in agreed }.mapTo(linkedSetOf()) { it.userId }
        return DirectAudience(
            userIds = consenting,
            skippedInactiveCount = skippedInactive,
            skippedNoConsentCount = candidates.size - consenting.size + activeCompanyCount,
        )
    }

    /** 지정 회원의 현재 상태. 없는 회원이 있으면 요청 전체를 거부한다. */
    private fun readStatuses(userIds: Set<Long>): Map<Long, UserStatus> {
        val statuses = linkedMapOf<Long, UserStatus>()
        val throughUserId = recipients.readUpperUserId()
        var afterUserId = 0L
        while (true) {
            val page = recipients.readPage(afterUserId, throughUserId, userIds, 500)
            if (page.isEmpty()) break
            page.forEach { statuses[it.userId] = it.status }
            afterUserId = page.last().userId
        }
        if (statuses.size != userIds.size) throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)
        return statuses
    }

    /**
     * 기기마다 보낼 대상. userIds가 빈 집합이면 보낼 회원이 없다는 뜻이다.
     * skipped* 는 발송 전에 이미 걸러 대상 수에만 더하는 회원 수다.
     */
    private data class DirectAudience(
        val userIds: Set<Long>? = null,
        val roles: Set<UserRole>? = null,
        val checkAllRequestedExist: Boolean = false,
        val skippedInactiveCount: Long = 0,
        val skippedNoConsentCount: Long = 0,
    )

    private fun createBroadcast(adminUserId: Long, command: NotificationBatchCreateDto, topic: String): NotificationBatchDto {
        val now = LocalDateTime.now(clock)
        val batchId = UUID.randomUUID().toString()
        val expiresAt = now.plusHours(24)
        val (count, noToken) = recipients.countActiveAudience(audienceRoles(command.targetType))
        val preview = command.body.substring(0, command.body.offsetByCodePoints(0,
            minOf(100, command.body.codePointCount(0, command.body.length))))
        val data = (command.data - "notificationId") + mapOf("batchId" to batchId, "type" to "ADMIN_NOTIFICATION", "source" to "BROADCAST")
        val payload = notificationPayload(command.copy(body = preview), data, 2048)
        val batch = batches.append(NotificationBatchAppendDto(
            batchId = batchId, requestedByUserId = adminUserId, targetType = command.targetType,
            requestedUserIdsJson = null, title = command.title, body = command.body,
            dataJson = objectMapper.writeValueAsString(command.data), imageId = command.imageId, imageUrl = command.imageUrl, targetUserCount = count,
            recipientUserCount = count - noToken, enqueuedCount = 1, skippedNoTokenUserCount = noToken,
            skippedInactiveUserCount = 0, dispatchExpiresAt = expiresAt, topicName = topic,
            url = command.data.getValue("url"),
        ))
        check(notifications.append(NotificationAppendDto(
            deduplicationKey = "admin-batch:$batchId:topic", channel = NotificationChannel.FCM,
            recipientType = NotificationRecipientType.TOPIC, templateCode = "admin_notification",
            recipientAddress = topic, payloadJson = payload, scheduledAt = now,
            batchId = batchId, dispatchExpiresAt = expiresAt,
        )))
        return batch
    }

    private fun notificationPayload(command: NotificationBatchCreateDto, data: Map<String, String>, maxBytes: Int = 4096): String {
        val notification = buildMap<String, String> {
            put("title", command.title); put("body", command.body)
            command.imageUrl?.let { put("imageUrl", it) }
        }
        val messageBytes = objectMapper.writeValueAsBytes(mapOf("notification" to notification, "data" to data))
        if (messageBytes.size > maxBytes) throw InvalidRequestFieldException("data", "시스템 식별자를 포함한 FCM payload는 ${maxBytes}바이트 이하여야 합니다.")
        return objectMapper.writeValueAsString(notification + mapOf("data" to data))
    }

    /** 회원 종류로 대상을 정하는 발송의 역할 조건. 전체·지정 회원 발송은 역할로 거르지 않는다. */
    private fun audienceRoles(target: NotificationBatchTargetType): Set<UserRole>? = when (target) {
        NotificationBatchTargetType.GENERAL_MEMBERS -> UserRole.GENERAL_MEMBER_ROLES
        NotificationBatchTargetType.COMPANY_MEMBERS -> setOf(UserRole.COMPANY)
        NotificationBatchTargetType.ALL, NotificationBatchTargetType.USER_IDS, NotificationBatchTargetType.JOB_ROLES -> null
    }

    private fun validateTargets(command: NotificationBatchCreateDto): Set<Long>? {
        if (command.targetType != NotificationBatchTargetType.JOB_ROLES && command.jobRoles != null) {
            throw InvalidRequestFieldException("jobRoles", "희망 직무 발송에서만 jobRoles를 보냅니다.")
        }
        if (command.advertisement && command.targetType == NotificationBatchTargetType.COMPANY_MEMBERS) {
            throw InvalidRequestFieldException("advertisement", "기업 회원은 마케팅 수신 동의 항목이 없어 광고성 발송 대상이 아닙니다.")
        }
        return when (command.targetType) {
            NotificationBatchTargetType.ALL,
            NotificationBatchTargetType.GENERAL_MEMBERS,
            NotificationBatchTargetType.COMPANY_MEMBERS -> {
                if (command.userIds != null) throw InvalidRequestFieldException("userIds", "전체·회원 종류 발송에서는 userIds를 보내지 않습니다.")
                null
            }
            NotificationBatchTargetType.JOB_ROLES -> {
                if (command.userIds != null) throw InvalidRequestFieldException("userIds", "희망 직무 발송에서는 userIds를 보내지 않습니다.")
                val roles = command.jobRoles
                if (roles.isNullOrEmpty() || roles.size > 100 || roles.any { !LetsCareerWishJobCatalog.isSelectable(it) }) {
                    throw InvalidRequestFieldException("jobRoles", "렛츠커리어 희망 직무 선택지를 1개 이상 100개 이하로 지정해야 합니다.")
                }
                null
            }
            NotificationBatchTargetType.USER_IDS -> {
                val ids = command.userIds
                if (ids.isNullOrEmpty() || ids.size > 1000 || ids.any { it <= 0 }) {
                    throw InvalidRequestFieldException("userIds", "양수 회원 ID를 1개 이상 1000개 이하로 지정해야 합니다.")
                }
                ids.toSet()
            }
        }
    }

    private fun validateData(data: Map<String, String>) {
        if (data.keys.any { it.isBlank() || it in setOf("from", "message_type", "gcm") || it.startsWith("google.") || it.startsWith("gcm.") }) {
            throw InvalidRequestFieldException("data", "빈 키나 FCM 예약 키를 사용할 수 없습니다.")
        }
        val url = data["url"]?.let { runCatching { URI(it) }.getOrNull() }
        if (url == null || url.scheme?.lowercase() !in setOf("http", "https") || url.host.isNullOrBlank()) {
            throw InvalidRequestFieldException("data.url", "이동할 http 또는 https URL이 필요합니다.")
        }
    }
}
