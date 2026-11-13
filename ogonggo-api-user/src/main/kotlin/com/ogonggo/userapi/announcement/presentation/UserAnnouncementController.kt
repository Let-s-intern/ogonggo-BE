package com.ogonggo.userapi.announcement.presentation

import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementPublicSearchCondition
import com.ogonggo.userapi.announcement.business.UserAnnouncementService
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementCategoryResponse
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementDetailResponse
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementSummaryResponse
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
@RequestMapping("/api/v1/announcements")
class UserAnnouncementController(
    private val userAnnouncementService: UserAnnouncementService,
) : UserAnnouncementApi {

    @GetMapping
    override fun getAnnouncements(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "category", required = false) category: AnnouncementCategory?,
        @RequestParam(name = "pinned", required = false) pinned: Boolean?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserAnnouncementSummaryResponse>>> {
        val result = userAnnouncementService.getAnnouncements(
            condition = AnnouncementPublicSearchCondition(category = category, pinned = pinned, keyword = keyword),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(UserAnnouncementSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/categories")
    override fun getAnnouncementCategories(): ResponseEntity<SuccessResponse<List<UserAnnouncementCategoryResponse>>> =
        SuccessResponse.ok(userAnnouncementService.getCategories().map(UserAnnouncementCategoryResponse::from))

    @GetMapping("/{announcementId}")
    override fun getAnnouncement(
        @PathVariable("announcementId") announcementId: Long,
    ): ResponseEntity<SuccessResponse<UserAnnouncementDetailResponse>> =
        SuccessResponse.ok(UserAnnouncementDetailResponse.from(userAnnouncementService.getAnnouncement(announcementId)))
}
