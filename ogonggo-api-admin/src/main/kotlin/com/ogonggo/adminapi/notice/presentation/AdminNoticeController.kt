package com.ogonggo.adminapi.notice.presentation

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.notice.business.AdminNoticeService
import com.ogonggo.adminapi.notice.presentation.request.CreateAdminNoticeRequest
import com.ogonggo.adminapi.notice.presentation.request.UpdateAdminNoticeRequest
import com.ogonggo.adminapi.notice.presentation.response.AdminNoticeDetailResponse
import com.ogonggo.adminapi.notice.presentation.response.AdminNoticeSummaryResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.notice.domain.NoticeManagementSearchCondition
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
@RequestMapping("/api/v1/admin/notices")
class AdminNoticeController(
    private val adminNoticeService: AdminNoticeService,
) : AdminNoticeApi {

    @GetMapping
    override fun getNotices(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "visibility", required = false) visibility: AdminContentVisibility?,
        @RequestParam(name = "pinned", required = false) pinned: Boolean?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminNoticeSummaryResponse>>> {
        val result = adminNoticeService.getNotices(
            condition = NoticeManagementSearchCondition(
                published = visibility?.published,
                pinned = pinned,
                keyword = keyword?.takeIf { it.isNotBlank() },
            ),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminNoticeSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{noticeId}")
    override fun getNotice(
        @PathVariable("noticeId") noticeId: Long,
    ): ResponseEntity<SuccessResponse<AdminNoticeDetailResponse>> =
        SuccessResponse.ok(AdminNoticeDetailResponse.from(adminNoticeService.getNotice(noticeId)))

    /** 등록이 커밋된 뒤의 공지를 다시 읽어 응답한다. */
    @PostMapping
    override fun createNotice(
        @RequestBody request: CreateAdminNoticeRequest,
    ): ResponseEntity<SuccessResponse<AdminNoticeDetailResponse>> {
        val noticeId = adminNoticeService.createNotice(request.toCommand())
        return SuccessResponse.created(AdminNoticeDetailResponse.from(adminNoticeService.getNotice(noticeId)))
    }

    /** 수정이 커밋된 뒤의 공지를 다시 읽어 응답한다. */
    @PatchMapping("/{noticeId}")
    override fun updateNotice(
        @PathVariable("noticeId") noticeId: Long,
        @RequestBody request: UpdateAdminNoticeRequest,
    ): ResponseEntity<SuccessResponse<AdminNoticeDetailResponse>> {
        adminNoticeService.updateNotice(noticeId, request.toCommand())
        return SuccessResponse.ok(AdminNoticeDetailResponse.from(adminNoticeService.getNotice(noticeId)))
    }

    @DeleteMapping("/{noticeId}")
    override fun deleteNotice(
        @PathVariable("noticeId") noticeId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminNoticeService.deleteNotice(noticeId)
        return SuccessResponse.ok()
    }
}
