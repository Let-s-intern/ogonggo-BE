package com.ogonggo.adminapi.notificationbatch.business

import com.ogonggo.adminapi.notificationbatch.implement.NotificationBatchCreator
import com.ogonggo.core.notificationbatch.implement.NotificationBatchReader
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchResultDto
import com.ogonggo.core.user.domain.LetsCareerWishJobCatalog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdminNotificationBatchService(
    private val creator: NotificationBatchCreator,
    private val batchReader: NotificationBatchReader,
    private val clock: Clock,
) {
    /** 알림함·발송 적재·요청 기록을 함께 커밋한다. provider 호출은 사용자 API의 기존 dispatcher가 맡는다. */
    @Transactional
    fun create(adminUserId: Long, command: CreateNotificationBatchCommand): NotificationBatchDto =
        creator.create(adminUserId, command.toDto())

    /** 관리자 화면이 고를 희망 직무 선택지. 렛츠커리어 커리어 정보 화면과 같은 목록이다. */
    fun jobRoleOptions(): Map<String, List<String>> = LetsCareerWishJobCatalog.rolesByField

    fun get(batchId: UUID): NotificationBatchResultDto = batchReader.readResult(batchId.toString(), LocalDateTime.now(clock))
}
