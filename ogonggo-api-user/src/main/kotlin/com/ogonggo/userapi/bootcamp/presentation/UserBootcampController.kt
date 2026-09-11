package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.business.UserBootcampService
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.error.InvalidRequestParameterException
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
        validateStatus(status)
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

    /**
     * 공개 목록은 모집중과 모집 마감만 다루므로 임시저장은 고를 수 없다.
     * 그대로 넘기면 항상 빈 목록이 나가 클라이언트가 잘못 보냈다는 사실을 알 수 없다.
     */
    private fun validateStatus(status: BootcampStatus?) {
        if (status != null && status !in SELECTABLE_STATUSES) {
            throw InvalidRequestParameterException(
                "status",
                "고를 수 있는 모집 상태는 ${SELECTABLE_STATUSES.joinToString()}입니다.",
            )
        }
    }

    companion object {
        private val SELECTABLE_STATUSES = listOf(BootcampStatus.RECRUITING, BootcampStatus.CLOSED)
    }
}
