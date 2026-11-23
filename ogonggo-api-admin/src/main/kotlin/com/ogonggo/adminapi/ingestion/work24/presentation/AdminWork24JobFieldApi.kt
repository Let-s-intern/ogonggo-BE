package com.ogonggo.adminapi.ingestion.work24.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.ingestion.work24.presentation.request.ReplaceAdminWork24JobFieldRequest
import com.ogonggo.adminapi.ingestion.work24.presentation.response.AdminWork24JobFieldResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.job.domain.JobField
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

@Tag(name = "관리자 고용24 수집 직군")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminWork24JobFieldApi {

    @Operation(
        operationId = "getWork24JobFields",
        summary = "고용24 수집 직군 조회",
        description = """
            고용24 채용정보를 매일 받을 때 직군마다 공고를 받는지 줍니다. 모든 직군을 직군 순서대로 줍니다.
            바꾼 적이 없는 직군은 이 설정이 생기기 전에 받던 다섯 직군(IT·개발, 기획·전략, 마케팅·광고, 영업, HR·총무)만 켜짐입니다.
            일학습병행 훈련과정과 부트캠프 수집에는 쓰지 않습니다.
        """,
    )
    fun getJobFields(): ResponseEntity<SuccessResponse<List<AdminWork24JobFieldResponse>>>

    @Operation(
        operationId = "replaceWork24JobField",
        summary = "고용24 수집 직군 수정",
        description = """
            직군 하나의 수집을 켜거나 끄고 바뀐 값을 반환합니다. 다음 수집 회차부터 씁니다. 같은 요청을 반복해도 결과가 같습니다.
            끄면 그 직군의 공고를 더 받지 않고, 이미 등록한 공고는 그대로 둡니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 없는 직군이거나, enabled가 빠졌거나 참·거짓이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceJobField(
        @Parameter(description = "직군 enum 이름. 예: SALES") jobField: JobField,
        @Valid request: ReplaceAdminWork24JobFieldRequest,
    ): ResponseEntity<SuccessResponse<AdminWork24JobFieldResponse>>
}
