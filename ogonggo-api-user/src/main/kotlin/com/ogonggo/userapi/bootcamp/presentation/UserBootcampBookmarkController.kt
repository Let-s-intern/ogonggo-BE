package com.ogonggo.userapi.bootcamp.presentation

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
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>> {
        val result = userBootcampBookmarkService.getBookmarks(userId, page - 1, size)
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
}
