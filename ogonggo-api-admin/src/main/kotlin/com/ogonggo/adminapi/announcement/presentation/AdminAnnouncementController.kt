package com.ogonggo.adminapi.announcement.presentation

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementService
import com.ogonggo.adminapi.announcement.presentation.request.CreateAdminAnnouncementRequest
import com.ogonggo.adminapi.announcement.presentation.request.UpdateAdminAnnouncementRequest
import com.ogonggo.adminapi.announcement.presentation.response.AdminAnnouncementDetailResponse
import com.ogonggo.adminapi.announcement.presentation.response.AdminAnnouncementSummaryResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/announcements")
class AdminAnnouncementController(
    private val adminAnnouncementService: AdminAnnouncementService,
) : AdminAnnouncementApi {

    @GetMapping
    override fun getAnnouncements(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "visibility", required = false) visibility: AdminContentVisibility?,
        @RequestParam(name = "pinned", required = false) pinned: Boolean?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAnnouncementSummaryResponse>>> {
        val result = adminAnnouncementService.getAnnouncements(
            condition = AnnouncementManagementSearchCondition(
                published = visibility?.published,
                pinned = pinned,
                keyword = keyword?.takeIf { it.isNotBlank() },
            ),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminAnnouncementSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{announcementId}")
    override fun getAnnouncement(
        @PathVariable("announcementId") announcementId: Long,
    ): ResponseEntity<SuccessResponse<AdminAnnouncementDetailResponse>> =
        SuccessResponse.ok(AdminAnnouncementDetailResponse.from(adminAnnouncementService.getAnnouncement(announcementId)))

    /** 등록이 커밋된 뒤의 공지를 다시 읽어 응답한다. */
    @PostMapping
    override fun createAnnouncement(
        @RequestBody request: CreateAdminAnnouncementRequest,
    ): ResponseEntity<SuccessResponse<AdminAnnouncementDetailResponse>> {
        val announcementId = adminAnnouncementService.createAnnouncement(request.toCommand())
        return SuccessResponse.created(AdminAnnouncementDetailResponse.from(adminAnnouncementService.getAnnouncement(announcementId)))
    }

    /** 수정이 커밋된 뒤의 공지를 다시 읽어 응답한다. */
    @PatchMapping("/{announcementId}")
    override fun updateAnnouncement(
        @PathVariable("announcementId") announcementId: Long,
        @RequestBody request: UpdateAdminAnnouncementRequest,
    ): ResponseEntity<SuccessResponse<AdminAnnouncementDetailResponse>> {
        adminAnnouncementService.updateAnnouncement(announcementId, request.toCommand())
        return SuccessResponse.ok(AdminAnnouncementDetailResponse.from(adminAnnouncementService.getAnnouncement(announcementId)))
    }

    @DeleteMapping("/{announcementId}")
    override fun deleteAnnouncement(
        @PathVariable("announcementId") announcementId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAnnouncementService.deleteAnnouncement(announcementId)
        return SuccessResponse.ok()
    }
}
