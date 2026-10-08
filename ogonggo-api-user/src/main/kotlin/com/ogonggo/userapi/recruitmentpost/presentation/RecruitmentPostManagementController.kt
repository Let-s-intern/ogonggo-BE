package com.ogonggo.userapi.recruitmentpost.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicantPresence
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostManagementService
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostService
import com.ogonggo.userapi.recruitmentpost.presentation.request.CreateRecruitmentPostDraftRequest
import com.ogonggo.userapi.recruitmentpost.presentation.request.PublishRecruitmentPostRequest
import com.ogonggo.userapi.recruitmentpost.presentation.response.CreateRecruitmentPostResponse
import com.ogonggo.userapi.recruitmentpost.presentation.response.RecruitmentPostFormResponse
import com.ogonggo.userapi.recruitmentpost.presentation.response.RecruitmentPostManagementItemResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me/recruitment-posts")
class RecruitmentPostManagementController(
    private val managementService: RecruitmentPostManagementService,
    private val recruitmentPostService: RecruitmentPostService,
    private val objectMapper: ObjectMapper,
) : RecruitmentPostManagementApi {

    override fun createMyRecruitmentPostDraft(
        userId: Long,
        request: CreateRecruitmentPostDraftRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostResponse>> =
        SuccessResponse.created(
            CreateRecruitmentPostResponse(
                recruitmentPostService.createDraft(userId, request.toCommand(userId)),
            ),
        )

    override fun copyMyRecruitmentPost(
        userId: Long,
        postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostFormResponse>> =
        SuccessResponse.created(
            RecruitmentPostFormResponse.from(managementService.copy(userId, postId), objectMapper),
        )

    override fun publishMyRecruitmentPost(
        userId: Long,
        postId: Long,
        request: PublishRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        managementService.publish(userId, postId)
        return SuccessResponse.ok()
    }

    override fun getMyRecruitmentPostForm(
        userId: Long,
        postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostFormResponse>> =
        SuccessResponse.ok(
            RecruitmentPostFormResponse.from(managementService.getPostForm(userId, postId), objectMapper),
        )

    @GetMapping
    override fun getMyRecruitmentPosts(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "status", defaultValue = "ALL") status: RecruitmentPostManagementStatus,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: RecruitmentPostApplicantPresence?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentPostType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "sort", defaultValue = "LATEST_SAVED") sort: RecruitmentPostManagementSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostManagementItemResponse>>> {
        val result = managementService.getPosts(
            userId = userId,
            status = status,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = normalizeRecruitmentPostKeyword(keyword),
            page = page - 1,
            size = size,
            sort = sort,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(RecruitmentPostManagementItemResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
