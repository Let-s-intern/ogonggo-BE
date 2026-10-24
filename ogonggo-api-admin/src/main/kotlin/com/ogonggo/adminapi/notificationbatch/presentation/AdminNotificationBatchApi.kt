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
        USER_IDS는 최대 1000개 ID를 중복 제거하고 없는 회원이 있으면 전체 요청을 거부합니다. ALL에는 userIds를 보내지 않습니다.
        전체 토픽 전환 설정이 켜진 ALL은 공통 공지와 토픽 작업 한 건을 함께 저장합니다. 회원별 알림함·토큰별 발송 행을 생성하지 않습니다.
        전환 전 ALL과 USER_IDS는 활성 회원별 알림함·활성 토큰별 발송을 유지합니다. deliveryTargetType으로 DIRECT/TOPIC을 구분합니다.
        enqueuedCount는 발송 작업 수입니다. TOPIC에서는 1이며 회원별 성공 수가 아닙니다. noTokenUserCount와 회원 집계는 요청 시점 DB 참고 값입니다.
        skippedNoTokenUserCount는 호환용 별칭이며 새 클라이언트는 noTokenUserCount를 사용합니다. data.url은 http(s) URL이 필요합니다.
        제목 100자·본문 1000자 원문을 보관하고 TOPIC 푸시는 본문 미리보기 최대 100자를 보냅니다. 시스템 식별자 포함 TOPIC payload는 2048바이트, DIRECT는 4096바이트입니다.
        실제 발송은 기존 dispatcher가 24시간 안에 처리합니다. POST마다 새 발송이며 자동 재발송·중복 클릭 방지는 제공하지 않습니다.
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
        생성 후 30일 동안 결과와 resultCode별 실패·미확정 건수를 조회합니다. DIRECT는 기기별, TOPIC은 토픽 요청 한 건 단위입니다. sentCount는 FCM 접수이며 단말 도착을 뜻하지 않습니다.
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
