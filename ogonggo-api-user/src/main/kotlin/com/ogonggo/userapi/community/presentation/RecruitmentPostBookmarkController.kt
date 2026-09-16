package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.business.RecruitmentPostBookmarkService
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostSummaryResponse
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
@RequestMapping("/api/v1/recruitment-post-bookmarks")
class RecruitmentPostBookmarkController(
    private val bookmarkService: RecruitmentPostBookmarkService,
) : RecruitmentPostBookmarkApi {

    @GetMapping
    override fun getBookmarks(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostSummaryResponse>>> {
        val result = bookmarkService.getBookmarks(userId, page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(RecruitmentPostSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @PostMapping("/{postId}")
    override fun addBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        bookmarkService.addBookmark(userId, postId)
        return SuccessResponse.created()
    }

    @DeleteMapping("/{postId}")
    override fun deleteBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        bookmarkService.deleteBookmark(userId, postId)
        return SuccessResponse.ok()
    }
}
