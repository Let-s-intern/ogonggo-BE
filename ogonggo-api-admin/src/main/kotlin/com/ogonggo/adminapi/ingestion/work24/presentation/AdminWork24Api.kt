package com.ogonggo.adminapi.ingestion.work24.presentation

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
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
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 고용24 Open API")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminWork24Api {

    @Operation(
        operationId = "getWork24ApiResponse",
        summary = "고용24 Open API 조회",
        description = """
            고용24 Open API를 서버의 인증키로 호출하고 응답을 JSON으로 바꿔 돌려줍니다. 저장하지 않습니다.

            요청 파라미터는 고용24 개발명세(고용24 > 고객센터 > OPEN-API > 서비스 소개 및 신청)의 이름 그대로 query로 보냅니다.
            인증키(authKey)와 응답 형식(returnType), 명세가 값을 고정한 파라미터(채용정보 callTp·infoSvc, 훈련과정 outType, 직업정보 target·jobGb)는
            서버가 채우므로 보내도 무시합니다. 값이 빈 파라미터는 보내지 않은 것으로 봅니다.

            XML 응답은 최상위 요소를 벗겨 JSON 객체로 바꿉니다. 같은 이름의 요소가 여러 개면 배열이 되지만
            한 개뿐이면 배열이 아닌 객체이므로, 목록을 읽을 때 두 경우를 모두 처리해야 합니다.
            직무정보는 고용24가 준 JSON을 그대로 돌려줍니다.

            | apiName | 고용24 API |
            | --- | --- |
            | recruitments | 채용정보 목록 |
            | recruitment-detail | 채용정보 상세 (wantedAuthNo) |
            | tomorrow-learning-card-courses | 국민내일배움카드 훈련과정 목록 |
            | tomorrow-learning-card-course-detail | 국민내일배움카드 훈련과정 과정·기관정보 |
            | tomorrow-learning-card-course-schedules | 국민내일배움카드 훈련과정 훈련일정 |
            | work-study-courses | 일학습병행 훈련과정 목록 |
            | work-study-course-detail | 일학습병행 훈련과정 과정·기관정보 |
            | work-study-course-schedules | 일학습병행 훈련과정 훈련일정 |
            | government-job-recruitments | 정부지원일자리 참여자모집정보 |
            | government-job-recruitment-detail | 정부지원일자리 참여자모집상세정보 |
            | government-job-programs | 정부지원일자리 일자리사업정보 |
            | government-job-program-detail | 정부지원일자리 일자리사업상세정보 |
            | government-job-institutions | 정부지원일자리 기관기본정보 |
            | government-job-participant-statistics | 정부지원일자리 참여자통계 |
            | job-seeker-programs | 구직자취업역량 강화프로그램 |
            | occupations | 직업정보 목록 |
            | occupation-detail | 직업정보 상세 |
            | occupation-dictionary | 직업사전 |
            | standard-job-descriptions | 표준직무기술서 |
            | duty-data-dictionary | 직무데이터사전 |
            | small-giant-companies | 강소기업 |
            | small-giant-company-visits | 강소기업 현장탐방기 |
            | youth-small-giant-company-experiences | 청년강소기업체험 |
            | youth-friendly-small-giant-companies | 청년친화강소기업 |
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 지원하지 않는 apiName입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = """
                    WORK24_REQUEST_REJECTED: 고용24가 파라미터나 인증키 오류로 요청을 거절했습니다. 사유는 서버 로그에 남습니다.
                    WORK24_UNAVAILABLE: 고용24 호출이나 응답 해석에 실패했습니다.
                """,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "WORK24_AUTH_KEY_NOT_CONFIGURED: 해당 서비스의 고용24 인증키가 설정되지 않았습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getWork24ApiResponse(
        @Parameter(
            description = "호출할 고용24 API",
            schema = Schema(
                type = "string",
                allowableValues = [
                    "recruitments",
                    "recruitment-detail",
                    "tomorrow-learning-card-courses",
                    "tomorrow-learning-card-course-detail",
                    "tomorrow-learning-card-course-schedules",
                    "work-study-courses",
                    "work-study-course-detail",
                    "work-study-course-schedules",
                    "government-job-recruitments",
                    "government-job-recruitment-detail",
                    "government-job-programs",
                    "government-job-program-detail",
                    "government-job-institutions",
                    "government-job-participant-statistics",
                    "job-seeker-programs",
                    "occupations",
                    "occupation-detail",
                    "occupation-dictionary",
                    "standard-job-descriptions",
                    "duty-data-dictionary",
                    "small-giant-companies",
                    "small-giant-company-visits",
                    "youth-small-giant-company-experiences",
                    "youth-friendly-small-giant-companies",
                ],
            ),
        )
        apiName: String,
        @Parameter(
            description = "고용24 명세의 요청 파라미터. 예: {\"startPage\": \"1\", \"display\": \"10\"}",
            required = false,
            schema = Schema(type = "object"),
        )
        parameters: Map<String, String>,
    ): ResponseEntity<SuccessResponse<JsonNode>>
}
