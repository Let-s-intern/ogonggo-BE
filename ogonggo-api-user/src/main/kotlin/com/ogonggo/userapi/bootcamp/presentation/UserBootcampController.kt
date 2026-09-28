package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.business.UserBootcampService
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/bootcamps")
class UserBootcampController(
    private val userBootcampService: UserBootcampService,
) : UserBootcampApi {

    @GetMapping
    override fun getBootcamps(
        @AuthenticationPrincipal userId: Long?,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "sort", defaultValue = "LATEST") sortType: BootcampSortType,
        @RequestParam(name = "tuitionType", required = false) tuitionType: TuitionType?,
        @RequestParam(name = "status", required = false) status: BootcampStatus?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>> {
        validatePublicStatus(status)
        val result = userBootcampService.getBootcamps(
            userId = userId,
            condition = BootcampSearchCondition(
                tuitionType = tuitionType,
                status = status,
                keyword = keyword,
            ),
            sortType = sortType,
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(UserBootcampSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @PostMapping("/{bootcampId}/application-url-clicks")
    override fun recordApplicationUrlClick(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampService.recordApplicationUrlClick(userId, bootcampId)
        return SuccessResponse.ok()
    }

    @GetMapping("/{bootcampId}")
    override fun getBootcamp(
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<UserBootcampDetailResponse>> =
        SuccessResponse.ok(UserBootcampDetailResponse.from(userBootcampService.getBootcamp(userId, bootcampId)))
}
