package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "부트캠프")
interface UserBootcampApi {

    @Operation(
        operationId = "listPublicBootcamps",
        summary = "부트캠프 목록 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.

            sort로 정렬을 고릅니다. LATEST는 최신순, VIEW_COUNT는 조회수순이며 조회 수가 같으면 최신순입니다.

            tuitionType과 status로 목록을 좁힙니다. 각각 하나씩 고를 수 있고,
            보내지 않으면 해당 조건을 적용하지 않습니다. status는 공개 목록이 다루는
            RECRUITING(모집중)과 CLOSED(모집 마감)만 받으며 그 밖의 값은 400입니다.

            keyword는 운영 회사명 또는 프로그램명에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.
            검색도 필터·정렬과 함께 사용할 수 있습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 공개 목록에서 고를 수 없는 모집 상태입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBootcamps(
        @Parameter(hidden = true)
        userId: Long?,
        @Min(1)
        page: Int,
        @Min(1)
        @Max(100)
        size: Int,
        sortType: BootcampSortType,
        tuitionType: TuitionType?,
        status: BootcampStatus?,
        @Size(min = 2, max = 100)
        keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>>

    @Operation(
        operationId = "createBootcampApplicationUrlClick",
        summary = "부트캠프 지원 페이지 이동 기록",
        description = """
            사용자가 부트캠프의 외부 지원 페이지로 이동하는 버튼을 눌렀다는 사실을 기록합니다.

            누가 눌렀는지를 남기는 기록이므로 목록·상세 조회와 달리 로그인이 필요합니다.
            같은 사용자가 같은 부트캠프를 여러 번 눌러도 최초 기록만 남기고 항상 성공합니다.
            이동할 주소는 상세 조회 응답의 applicationUrl을 사용합니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "기록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_NOT_FOUND: 부트캠프를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun recordApplicationUrlClick(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "getPublicBootcamp",
        summary = "부트캠프 상세 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_NOT_FOUND: 부트캠프를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBootcamp(
        @Parameter(hidden = true)
        userId: Long?,
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<UserBootcampDetailResponse>>
}
