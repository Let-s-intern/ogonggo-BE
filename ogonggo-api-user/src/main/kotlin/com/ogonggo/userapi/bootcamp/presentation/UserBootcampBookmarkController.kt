package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.userapi.bootcamp.business.UserBootcampBookmarkService
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class UserBootcampBookmarkController(
    private val userBootcampBookmarkService: UserBootcampBookmarkService,
) : UserBootcampBookmarkApi {

    override fun getBookmarks(
        userId: Long,
        page: Int,
        size: Int,
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

    override fun addBookmark(userId: Long, bootcampId: Long): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampBookmarkService.addBookmark(userId, bootcampId)
        return SuccessResponse.created()
    }

    override fun deleteBookmark(userId: Long, bootcampId: Long): ResponseEntity<SuccessResponse<Unit>> {
        userBootcampBookmarkService.deleteBookmark(userId, bootcampId)
        return SuccessResponse.ok()
    }
}
