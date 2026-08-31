package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.userapi.bootcamp.business.UserBootcampService
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class UserBootcampController(
    private val userBootcampService: UserBootcampService,
) : UserBootcampApi {

    override fun getBootcamps(
        page: Int,
        size: Int,
        sortType: BootcampSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>> {
        val result = userBootcampService.getBootcamps(page - 1, size, sortType)
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

    override fun getBootcamp(
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<UserBootcampDetailResponse>> =
        SuccessResponse.ok(UserBootcampDetailResponse.from(userBootcampService.getBootcamp(bootcampId)))
}
