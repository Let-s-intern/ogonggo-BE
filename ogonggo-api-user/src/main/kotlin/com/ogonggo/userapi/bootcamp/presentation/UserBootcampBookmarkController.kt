package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bookmark.domain.ApplicationStatus
import com.ogonggo.core.bookmark.domain.BookmarkListCondition
import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.business.UserBootcampBookmarkService
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
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
@RequestMapping("/api/v1/bootcamp-bookmarks")
class UserBootcampBookmarkController(
    private val userBootcampBookmarkService: UserBootcampBookmarkService,
) : UserBootcampBookmarkApi {

    @GetMapping
    override fun getBookmarks(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "sort", defaultValue = "RECENTLY_SAVED") sortType: BookmarkSortType,
        @RequestParam(name = "tuitionType", required = false) tuitionType: TuitionType?,
        @RequestParam(name = "status", required = false) status: BootcampStatus?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: ApplicationStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>> {
        validatePublicStatus(status)
        val result = userBootcampBookmarkService.getBookmarks(
            userId = userId,
            condition = BootcampSearchCondition(
                tuitionType = tuitionType,
                status = status,
                keyword = keyword,
            ),
            page = page - 1,
            size = size,
            bookmarkCondition = BookmarkListCondition(applicationStatus = applicationStatus, sortType = sortType),
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

    @PostMapping("/{bootcampId}")
    override fun addBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampBookmarkService.addBookmark(userId, bootcampId)
        return SuccessResponse.created()
    }

    @DeleteMapping("/{bootcampId}")
    override fun deleteBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampBookmarkService.deleteBookmark(userId, bootcampId)
        return SuccessResponse.ok()
    }

    @PostMapping("/{bootcampId}/prepare")
    override fun prepare(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampBookmarkService.prepare(userId, bootcampId)
        return SuccessResponse.ok()
    }

    @PostMapping("/{bootcampId}/cancel-preparation")
    override fun cancelPreparation(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampBookmarkService.cancelPreparation(userId, bootcampId)
        return SuccessResponse.ok()
    }
}
