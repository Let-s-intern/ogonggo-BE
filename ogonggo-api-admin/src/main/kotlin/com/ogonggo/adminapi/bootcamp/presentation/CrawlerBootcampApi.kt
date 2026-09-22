package com.ogonggo.adminapi.bootcamp.presentation

import com.ogonggo.adminapi.bootcamp.presentation.request.CrawlerBootcampRequest
import com.ogonggo.adminapi.bootcamp.presentation.response.CrawlerBootcampLookupResponse
import com.ogonggo.adminapi.bootcamp.presentation.response.CrawlerBootcampRegistrationResponse
import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

private const val BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 요청 값이 올바르지 않거나 모집 기간·교육 기간·지원 방법 칸이 서로 맞지 않습니다. " +
        "모집 상태에 RECRUITING·CLOSED 밖의 값을 보내도 400입니다."
private const val UNAUTHORIZED_DESCRIPTION = "UNAUTHORIZED: 내부 API 키가 없거나 올바르지 않습니다."
private const val CRAWLED_BOOTCAMP_NOT_FOUND_DESCRIPTION =
    "BOOTCAMP_NOT_FOUND: 수집 부트캠프가 없습니다. 기업회원 부트캠프와 원문 URL이 없는 부트캠프는 찾지 않습니다."

@Tag(name = "크롤러 부트캠프")
@SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
interface CrawlerBootcampApi {

    @Operation(
        operationId = "createCrawlerBootcamp",
        summary = "크롤러 부트캠프 등록",
        description = """
            크롤러가 수집한 부트캠프를 게시 상태로 등록합니다. 검수는 기업회원이 올린 부트캠프만 거칩니다.

            모집 상태(status)는 RECRUITING(모집 중) 또는 CLOSED(모집 마감)만 받고, 보내지 않으면 RECRUITING으로 등록합니다.
            CLOSED로 보내면 모집 마감 상태로 게시하며 마감 일시는 등록 시각입니다.

            기간 모집은 모집 시작·종료 일시가 모두 필요하고, 상시 모집에는 모집 종료 일시를 보낼 수 없습니다.
            외부 페이지 지원은 지원 페이지 주소가 필요하고, 이메일 지원에는 보낼 수 없습니다.
            커리큘럼은 보낸 순서대로 노출합니다.
            이미 같은 원문 URL로 등록된 미삭제 부트캠프가 있으면 409로 거절합니다. 이때는 원문 URL로 식별자를 찾아 교체합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "BOOTCAMP_ALREADY_EXISTS: 이미 등록된 원문 URL입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun registerBootcamp(
        @Valid request: CrawlerBootcampRequest,
    ): ResponseEntity<SuccessResponse<CrawlerBootcampRegistrationResponse>>

    @Operation(
        operationId = "getCrawlerBootcamp",
        summary = "원문 URL로 크롤러 부트캠프 식별자 조회",
        description = """
            원문 URL로 미삭제 수집 부트캠프의 식별자를 찾습니다.
            크롤러가 등록 응답의 식별자를 잃은 채 같은 부트캠프를 다시 등록해 409를 받았을 때 씁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CRAWLED_BOOTCAMP_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBootcamp(
        @NotBlank @Size(max = 2048) sourceUrl: String,
    ): ResponseEntity<SuccessResponse<CrawlerBootcampLookupResponse>>

    @Operation(
        operationId = "replaceCrawlerBootcamp",
        summary = "크롤러 부트캠프 교체",
        description = """
            다시 수집한 값으로 수집 부트캠프 전체를 바꾸고, 커리큘럼은 기존 것을 지운 뒤 보낸 목록으로 바꿉니다.
            운영자가 관리자 콘솔에서 고친 내용도 이 값으로 덮어씁니다. 게시 상태는 바꾸지 않습니다.

            모집 상태(status)를 보내면 그 값으로 맞춥니다. RECRUITING에서 CLOSED로 바뀌면 교체 시각으로 마감하고,
            CLOSED에서 RECRUITING으로 바뀌면 마감 일시를 지우고 다시 모집 중으로 둡니다. 지금과 같으면 그대로 둡니다.
            보내지 않으면 모집 상태를 바꾸지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "교체 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CRAWLED_BOOTCAMP_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "BOOTCAMP_ALREADY_EXISTS: 다른 부트캠프가 쓰는 원문 URL로 바꾸려 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceBootcamp(
        @Positive bootcampId: Long,
        @Valid request: CrawlerBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteCrawlerBootcamp",
        summary = "크롤러 부트캠프 삭제",
        description = """
            더는 쓰지 않는 수집 부트캠프를 소프트 삭제합니다.
            이미 삭제한 부트캠프를 다시 삭제해도 성공하며 최초 삭제 일시를 유지합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CRAWLED_BOOTCAMP_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteBootcamp(
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
