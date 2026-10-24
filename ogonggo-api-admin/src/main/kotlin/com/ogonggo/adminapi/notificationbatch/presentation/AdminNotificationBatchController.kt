package com.ogonggo.adminapi.notificationbatch.presentation

import com.ogonggo.adminapi.notificationbatch.business.AdminNotificationBatchService
import com.ogonggo.adminapi.notificationbatch.presentation.request.CreateNotificationBatchRequest
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchCreatedResponse
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchResultResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/notification-batches")
class AdminNotificationBatchController(private val service: AdminNotificationBatchService) : AdminNotificationBatchApi {
    @PostMapping
    override fun createNotificationBatch(
        @AuthenticationPrincipal adminUserId: Long,
        @RequestBody request: CreateNotificationBatchRequest,
    ): ResponseEntity<SuccessResponse<AdminNotificationBatchCreatedResponse>> {
        val batch = service.create(adminUserId, request.toCommand())
        // Service의 트랜잭션 커밋이 끝난 요청만 기록하며 본문·토큰은 로그에 남기지 않는다.
        log.info("관리자 FCM 발송 적재 완료. batchId={}, adminUserId={}, targetType={}, enqueued={}, skippedNoToken={}, skippedInactive={}",
            batch.batchId, batch.requestedByUserId, batch.targetType, batch.enqueuedCount,
            batch.skippedNoTokenUserCount, batch.skippedInactiveUserCount)
        return SuccessResponse.created(AdminNotificationBatchCreatedResponse.from(batch))
    }

    @GetMapping("/{batchId}")
    override fun getNotificationBatch(@PathVariable("batchId") batchId: UUID): ResponseEntity<SuccessResponse<AdminNotificationBatchResultResponse>> =
        SuccessResponse.ok(AdminNotificationBatchResultResponse.from(service.get(batchId)))

    private companion object { val log = LoggerFactory.getLogger(AdminNotificationBatchController::class.java) }
}
