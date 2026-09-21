package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.community.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1")
class RecruitmentPostBookmarkController(
    private val bookmarkService: RecruitmentPostBookmarkService,
) : RecruitmentPostBookmarkApi {

    @GetMapping("/recruitment-post-bookmarks")
    override fun getBookmarks(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentStatus?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "sort", defaultValue = "RECENTLY_SAVED") sortType: BookmarkSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostSummaryResponse>>> {
        val result = bookmarkService.getBookmarks(
            userId = userId,
            page = page - 1,
            size = size,
            condition = RecruitmentPostBookmarkSearchCondition(
                recruitmentStatus = recruitmentStatus,
                recruitmentType = recruitmentType,
                keyword = normalizeRecruitmentPostKeyword(keyword),
                sortType = sortType,
            ),
        )
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

    @PutMapping("/recruitment-posts/{postId}/bookmarks/me")
    override fun addBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        bookmarkService.addBookmark(userId, postId)
        return SuccessResponse.created()
    }

    @DeleteMapping("/recruitment-posts/{postId}/bookmarks/me")
    override fun deleteBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        bookmarkService.deleteBookmark(userId, postId)
        return SuccessResponse.ok()
    }

    @PostMapping("/recruitment-post-bookmarks/{postId}/prepare")
    override fun prepare(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        bookmarkService.prepare(userId, postId)
        return SuccessResponse.ok()
    }

    @PostMapping("/recruitment-post-bookmarks/{postId}/cancel-preparation")
    override fun cancelPreparation(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        bookmarkService.cancelPreparation(userId, postId)
        return SuccessResponse.ok()
    }
}
