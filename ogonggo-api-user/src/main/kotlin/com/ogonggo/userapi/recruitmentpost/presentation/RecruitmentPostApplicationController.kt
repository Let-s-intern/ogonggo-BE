package com.ogonggo.userapi.recruitmentpost.presentation

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostApplicationService
import com.ogonggo.userapi.recruitmentpost.presentation.request.UpdateRecruitmentPostApplicationStatusRequest
import com.ogonggo.userapi.recruitmentpost.presentation.response.CreateRecruitmentPostApplicationResponse
import com.ogonggo.userapi.recruitmentpost.presentation.response.RecruitmentPostApplicationItemResponse
import com.ogonggo.userapi.recruitmentpost.presentation.response.RecruitmentPostApplicationPageResponse
import com.ogonggo.userapi.response.PageInfo
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class RecruitmentPostApplicationController(
    private val applicationService: RecruitmentPostApplicationService,
) : RecruitmentPostApplicationApi {

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
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentPostType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "sort", defaultValue = "LATEST") sort: RecruitmentPostApplicationSortType,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: RecruitmentPostApplicationProgressStatus?,
    ): ResponseEntity<SuccessResponse<RecruitmentPostApplicationPageResponse>> {
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
            RecruitmentPostApplicationPageResponse(
                items = result.items.map(RecruitmentPostApplicationItemResponse::from),
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
        request: UpdateRecruitmentPostApplicationStatusRequest,
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
