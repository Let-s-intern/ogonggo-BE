package com.ogonggo.userapi.notice.presentation

import com.ogonggo.userapi.notice.business.UserNoticeService
import com.ogonggo.userapi.notice.presentation.response.UserNoticeDetailResponse
import com.ogonggo.userapi.notice.presentation.response.UserNoticeSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/notices")
class UserNoticeController(
    private val userNoticeService: UserNoticeService,
) : UserNoticeApi {

    @GetMapping
    override fun getNotices(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<UserNoticeSummaryResponse>>> {
        val result = userNoticeService.getNotices(page = page - 1, size = size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(UserNoticeSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{noticeId}")
    override fun getNotice(
        @PathVariable("noticeId") noticeId: Long,
    ): ResponseEntity<SuccessResponse<UserNoticeDetailResponse>> =
        SuccessResponse.ok(UserNoticeDetailResponse.from(userNoticeService.getNotice(noticeId)))
}
