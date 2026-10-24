package com.ogonggo.adminapi.notificationbatch.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.notificationbatch.presentation.request.CreateNotificationBatchRequest
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchCreatedResponse
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchResultResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import java.util.UUID

@Tag(name = "관리자 FCM 발송")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminNotificationBatchApi {
    @Operation(operationId = "createNotificationBatch", summary = "FCM 발송 요청", description = """
        USER_IDS는 최대 1000개 ID의 중복을 제거하며 없는 회원이 있으면 전체 요청을 거부합니다. ALL은 전체 활성 회원을 대상으로 하며 userIds를 보내지 않습니다.
        활성 토큰마다 발송 한 건을 적재합니다. enqueuedCount는 토큰별 발송 건수이고 나머지 건수는 회원 수입니다. data.url은 http(s) URL이어야 합니다.
        활성 회원마다 토큰 유무와 관계없이 사용자 알림함 한 건을 생성합니다. 알림함·발송 적재·요청 기록을 함께 커밋한 뒤 응답합니다.
        알림함은 ADMIN_NOTIFICATION으로 제목·본문 미리보기 최대 100자·data.url을 보관합니다. 실제 발송은 사용자 API의 기존 발송 작업이 24시간 안에 처리합니다.
        푸시 data의 notificationId·type은 서버가 설정하며 payload 크기 제한에 포함합니다. POST마다 새로운 알림을 생성하며 자동 재시도·중복 클릭 방지는 제공하지 않습니다.
    """)
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "적재 및 운영 기록 저장 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "BAD_REQUEST: 대상·길이·data·URL·payload 크기 오류", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "USER_NOT_FOUND: 지정 회원 중 존재하지 않는 ID", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun createNotificationBatch(@Parameter(hidden = true) adminUserId: Long, @Valid request: CreateNotificationBatchRequest): ResponseEntity<SuccessResponse<AdminNotificationBatchCreatedResponse>>

    @Operation(operationId = "getNotificationBatch", summary = "FCM 발송 결과 조회", description = """
        생성 후 30일 동안 토큰별 결과와 resultCode별 실패·미확정 건수를 조회합니다. sentCount는 FCM 접수이며 단말 도착을 뜻하지 않습니다.
        24시간이 지난 PENDING은 expiredCount로 구분합니다. completed는 더 이상 발송 대기가 없음을 뜻하며 UNKNOWN·기한 초과도 따로 확인해야 합니다.
        기한 직전에 시작한 provider 요청은 조회 뒤 결과를 기록할 수 있습니다. 토큰과 provider 응답 원문은 반환하지 않습니다.
    """)
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "BAD_REQUEST: UUID 형식 오류", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "NOTIFICATION_BATCH_NOT_FOUND: 없거나 조회 기간 만료", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun getNotificationBatch(batchId: UUID): ResponseEntity<SuccessResponse<AdminNotificationBatchResultResponse>>
}
