package com.ogonggo.userapi.recruitmentpost.presentation

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicantPresence
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.userapi.recruitmentpost.presentation.request.CreateRecruitmentPostDraftRequest
import com.ogonggo.userapi.recruitmentpost.presentation.request.PublishRecruitmentPostRequest
import com.ogonggo.userapi.recruitmentpost.presentation.request.UpdateRecruitmentPostApplicationStatusRequest
import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 모집글 경로를 다른 자원과 맞추기 전의 경로다. 서버를 먼저 배포하므로 프런트가 새 경로로 옮길 때까지 같은 동작으로 받는다.
 * 명세에는 새 경로만 보이도록 숨기고, 동작은 새 경로의 컨트롤러에 맡긴다. 프런트 배포 후 서버 2차 배포에서 이 파일을 지운다.
 *
 * - `/api/v1/me/recruitment-posts` 이하 → `/api/v1/users/me/recruitment-posts` 이하
 * - `/api/v1/me/recruitment-applications` 이하 → `/api/v1/users/me/recruitment-post-applications` 이하
 * - `PUT·DELETE /api/v1/recruitment-posts/{postId}/bookmarks/me` → `POST·DELETE /api/v1/recruitment-post-bookmarks/{postId}`
 */
@Hidden
@RestController
class RecruitmentPostLegacyPathController(
    private val managementController: RecruitmentPostManagementController,
    private val applicationController: RecruitmentPostApplicationController,
    private val bookmarkController: RecruitmentPostBookmarkController,
) {

    @GetMapping("/api/v1/me/recruitment-posts")
    fun getMyRecruitmentPosts(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "status", defaultValue = "ALL") status: RecruitmentPostManagementStatus,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: RecruitmentPostApplicantPresence?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentPostType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "sort", defaultValue = "LATEST_SAVED") sort: RecruitmentPostManagementSortType,
    ) = managementController.getMyRecruitmentPosts(
        userId, page, size, status, recruitmentStatus, applicationStatus, recruitmentType, keyword, sort,
    )

    @GetMapping("/api/v1/me/recruitment-posts/{postId}")
    fun getMyRecruitmentPostForm(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ) = managementController.getMyRecruitmentPostForm(userId, postId)

    @PostMapping("/api/v1/me/recruitment-posts/drafts")
    fun createMyRecruitmentPostDraft(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: CreateRecruitmentPostDraftRequest,
    ) = managementController.createMyRecruitmentPostDraft(userId, request)

    @PostMapping("/api/v1/me/recruitment-posts/{postId}/copies")
    fun copyMyRecruitmentPost(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ) = managementController.copyMyRecruitmentPost(userId, postId)

    @PostMapping("/api/v1/me/recruitment-posts/{postId}/publish")
    fun publishMyRecruitmentPost(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
        @RequestBody @Valid request: PublishRecruitmentPostRequest,
    ) = managementController.publishMyRecruitmentPost(userId, postId, request)

    @GetMapping("/api/v1/me/recruitment-applications")
    fun getApplications(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentPostType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "sort", defaultValue = "LATEST") sort: RecruitmentPostApplicationSortType,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: RecruitmentPostApplicationProgressStatus?,
    ) = applicationController.getApplications(
        userId, page, size, recruitmentStatus, recruitmentType, keyword, sort, applicationStatus,
    )

    @PatchMapping("/api/v1/me/recruitment-applications/{postId}")
    fun updateApplicationStatus(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
        @RequestBody @Valid request: UpdateRecruitmentPostApplicationStatusRequest,
    ) = applicationController.updateApplicationStatus(userId, postId, request)

    @DeleteMapping("/api/v1/me/recruitment-applications/{postId}")
    fun deleteApplication(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ) = applicationController.deleteApplication(userId, postId)

    @PutMapping("/api/v1/recruitment-posts/{postId}/bookmarks/me")
    fun addBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ) = bookmarkController.addBookmark(userId, postId)

    @DeleteMapping("/api/v1/recruitment-posts/{postId}/bookmarks/me")
    fun deleteBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") postId: Long,
    ) = bookmarkController.deleteBookmark(userId, postId)
}
