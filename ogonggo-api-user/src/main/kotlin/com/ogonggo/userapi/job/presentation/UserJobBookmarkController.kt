package com.ogonggo.userapi.job.presentation

import com.ogonggo.userapi.job.business.UserJobBookmarkService
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/job-bookmarks")
class UserJobBookmarkController(
    private val userJobBookmarkService: UserJobBookmarkService,
) : UserJobBookmarkApi {

    @GetMapping
    override fun getBookmarks(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>> {
        val result = userJobBookmarkService.getBookmarks(userId, page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(UserJobSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @PostMapping("/{jobId}")
    override fun addBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.addBookmark(userId, jobId)
        return SuccessResponse.created()
    }

    @DeleteMapping("/{jobId}")
    override fun deleteBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.deleteBookmark(userId, jobId)
        return SuccessResponse.ok()
    }
}
