package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.RecruitmentApplicationSortType
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationService
import com.ogonggo.userapi.community.presentation.request.UpdateRecruitmentApplicationStatusRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostApplicationResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentApplicationItemResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentApplicationPageResponse
import com.ogonggo.userapi.response.PageInfo
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class RecruitmentApplicationController(
    private val applicationService: RecruitmentPostApplicationService,
) : RecruitmentApplicationApi {

    override fun createApplication(
        @AuthenticationPrincipal userId: Long,
        postId: Long,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostApplicationResponse>> =
        SuccessResponse.ok(
            CreateRecruitmentPostApplicationResponse.from(
                applicationService.createApplication(userId, postId),
            ),
        )

    override fun getApplications(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentStatus?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "sort", defaultValue = "LATEST") sort: RecruitmentApplicationSortType,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: RecruitmentApplicationProgressStatus?,
    ): ResponseEntity<SuccessResponse<RecruitmentApplicationPageResponse>> {
        val result = applicationService.getApplications(
            userId = userId,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = normalizeRecruitmentPostKeyword(keyword),
            page = page - 1,
            size = size,
            sort = sort,
        )
        return SuccessResponse.ok(
            RecruitmentApplicationPageResponse(
                items = result.items.map(RecruitmentApplicationItemResponse::from),
                pageInfo = PageInfo(
                    pageNum = result.page + 1,
                    pageSize = result.size,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                ),
                countsByRecruitmentType = result.countsByRecruitmentType,
            ),
        )
    }

    override fun updateApplicationStatus(
        @AuthenticationPrincipal userId: Long,
        postId: Long,
        request: UpdateRecruitmentApplicationStatusRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        applicationService.changeApplicationStatus(
            userId = userId,
            postId = postId,
            status = checkNotNull(request.applicationStatus),
        )
        return SuccessResponse.ok()
    }

    override fun deleteApplication(
        @AuthenticationPrincipal userId: Long,
        postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        applicationService.deleteApplication(userId, postId)
        return SuccessResponse.ok()
    }
}
