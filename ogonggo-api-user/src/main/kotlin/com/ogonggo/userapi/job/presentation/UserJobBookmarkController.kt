package com.ogonggo.userapi.job.presentation

import com.ogonggo.userapi.job.business.UserJobBookmarkService
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class UserJobBookmarkController(
    private val userJobBookmarkService: UserJobBookmarkService,
) : UserJobBookmarkApi {

    override fun getBookmarks(
        userId: Long,
        page: Int,
        size: Int,
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

    override fun addBookmark(userId: Long, jobId: Long): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.addBookmark(userId, jobId)
        return SuccessResponse.created()
    }

    override fun deleteBookmark(userId: Long, jobId: Long): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.deleteBookmark(userId, jobId)
        return SuccessResponse.ok()
    }
}
