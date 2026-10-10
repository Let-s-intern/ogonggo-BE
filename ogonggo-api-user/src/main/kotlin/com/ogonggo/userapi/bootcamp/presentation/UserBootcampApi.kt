package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampCategory
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
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
            어느 정렬이든 기업과 함께 운영하는 과정이 먼저 오고, 그 안에서 고른 정렬을 따릅니다.

            category로 목록을 좁힙니다. KDT는 고용24에서 수집한 K-디지털 트레이닝 과정, SESAC은 새싹 과정이며
            보내지 않으면 전체입니다.

            keyword는 운영 회사명 또는 프로그램명에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.
            검색도 필터·정렬과 함께 사용할 수 있습니다.

            recruitmentStatus는 RECRUITING(모집 중), CLOSED(모집 마감) 중 하나이며 보내지 않으면 둘 다 반환합니다.
            DRAFT를 보내면 400입니다. 모집 종료 일시가 지난 부트캠프는 매시 정각에 CLOSED로 바뀌므로 그 전까지는 RECRUITING으로 걸릴 수 있습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getBootcamps(
        @Parameter(hidden = true)
        userId: Long?,
        @Min(1)
        page: Int,
        @Min(1)
        @Max(100)
        size: Int,
        sortType: BootcampSortType,
        category: BootcampCategory?,
        @Size(min = 2, max = 100)
        keyword: String?,
        recruitmentStatus: BootcampRecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>>

    @Operation(
        operationId = "createBootcampSourceUrlClick",
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
    fun recordSourceUrlClick(
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
