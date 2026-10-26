package com.ogonggo.adminapi.notification.intake.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.notification.intake.presentation.response.AdminWelcomeEmailResendResponse
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
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 알림")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminWelcomeEmailApi {

    @Operation(
        operationId = "createWelcomeEmailResend",
        summary = "가입 환영 이메일 재발송",
        description = """
            오공고 일반 회원 ID로 현재 프로필의 가입 이메일에 sign_up_welcome 메일을 다시 적재합니다.
            USER·MENTOR·ADMIN 역할의 활성 일반 회원만 대상이며 수신 이메일·제목·본문은 입력받지 않습니다.
            기존 발송 이력이 없어도 요청할 수 있고 기존 결과는 변경하지 않습니다. 요청마다 새 발송이므로 반복 호출하면 중복 수신할 수 있습니다.
            201은 새 발송 기록의 저장 완료입니다. 실제 SMTP 발송은 사용자 API dispatcher가 24시간 안에 처리하며 sentAt은 최근 알림 결과에서 확인합니다.
            응답의 notificationId로 GET /api/v1/admin/notifications/recent-results 항목을 식별할 수 있습니다. batchId는 생성하지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "재발송 적재 완료", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: userId가 양수가 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음. GENERAL_MEMBER_REQUIRED: 기업 회원. USER_SUSPENDED·USER_WITHDRAWN: 정지·탈퇴 회원.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "USER_NOT_FOUND: 회원이 없습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "WELCOME_EMAIL_RECIPIENT_UNAVAILABLE: 프로필 또는 가입 이메일이 없습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun createWelcomeEmailResend(
        @Parameter(hidden = true) adminUserId: Long,
        @Parameter(description = "수신 회원 ID(recipientUserId). 발송 기록 notificationId와 다릅니다.") @Min(1) userId: Long,
    ): ResponseEntity<SuccessResponse<AdminWelcomeEmailResendResponse>>
}
