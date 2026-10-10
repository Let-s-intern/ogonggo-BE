package com.ogonggo.adminapi.contentreview.presentation

import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.adminapi.contentreview.business.AdminContentRejectionService
import com.ogonggo.adminapi.contentreview.presentation.request.UpdateRejectionReasonRequest
import com.ogonggo.adminapi.contentreview.presentation.response.AdminContentRejectionResponse
import com.ogonggo.core.contentreview.domain.ContentReviewTargetType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/content-rejections")
class AdminContentRejectionController(
    private val adminRejectionService: AdminContentRejectionService,
) : AdminContentRejectionApi {

    @GetMapping
    override fun getRejections(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "type", required = false) type: ContentReviewTargetType?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminContentRejectionResponse>>> {
        val result = adminRejectionService.getRejections(type, keyword?.takeIf { it.isNotBlank() }, page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.rejections.map(AdminContentRejectionResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    /** 사유 수정이 커밋된 뒤의 기록을 다시 읽어 응답한다. */
    @PatchMapping("/{type}/{id}")
    override fun updateRejection(
        @PathVariable("type") type: String,
        @PathVariable("id") id: Long,
        @RequestBody request: UpdateRejectionReasonRequest,
    ): ResponseEntity<SuccessResponse<AdminContentRejectionResponse>> {
        val contentType = parseContentReviewTargetType(type)
        adminRejectionService.replaceReason(contentType, id, request.reason)
        return SuccessResponse.ok(AdminContentRejectionResponse.from(adminRejectionService.getRejection(contentType, id)))
    }
}
