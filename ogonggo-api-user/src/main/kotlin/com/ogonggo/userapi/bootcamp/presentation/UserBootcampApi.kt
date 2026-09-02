package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "부트캠프")
@RequestMapping("/api/v1/bootcamps")
interface UserBootcampApi {

    @Operation(
        summary = "부트캠프 목록 조회",
        description = "sort로 정렬을 고릅니다. LATEST는 최신순, VIEW_COUNT는 조회수순이며 조회 수가 같으면 최신순입니다.",
    )
    @GetMapping
    fun getBootcamps(
        @RequestParam(name = "page", defaultValue = "1")
        @Min(1)
        page: Int,
        @RequestParam(name = "size", defaultValue = "10")
        @Min(1)
        @Max(100)
        size: Int,
        @RequestParam(name = "sort", defaultValue = "LATEST")
        sortType: BootcampSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>>

    @Operation(summary = "부트캠프 상세 조회")
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
    @GetMapping("/{bootcampId}")
    fun getBootcamp(
        @PathVariable("bootcampId")
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<UserBootcampDetailResponse>>
}
