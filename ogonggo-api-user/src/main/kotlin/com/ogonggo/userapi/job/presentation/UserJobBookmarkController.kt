package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmarkSearchCondition
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.userapi.job.business.UserJobBookmarkService
import com.ogonggo.userapi.job.presentation.request.UpdateJobApplicationStatusRequest
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/job-bookmarks")
class UserJobBookmarkController(
    private val userJobBookmarkService: UserJobBookmarkService,
) : UserJobBookmarkApi {

    @GetMapping
    override fun getBookmarks(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "sort", defaultValue = "RECENTLY_SAVED") sortType: BookmarkSortType,
        @RequestParam(name = "employmentType", required = false) employmentType: EmploymentType?,
        @RequestParam(name = "experienceType", required = false) experienceType: ExperienceType?,
        @RequestParam(name = "jobField", required = false) jobField: String?,
        @RequestParam(name = "jobRole", required = false) jobRole: String?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "applicationStatus", required = false) applicationStatus: JobApplicationStatus?,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: JobRecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>> {
        val result = userJobBookmarkService.getBookmarks(
            userId = userId,
            condition = JobSearchCondition(
                employmentType = employmentType,
                experienceType = experienceType,
                jobField = jobField,
                jobRole = jobRole,
                keyword = keyword,
            ),
            page = page - 1,
            size = size,
            bookmarkCondition = JobBookmarkSearchCondition(
                applicationStatus = applicationStatus,
                recruitmentStatus = recruitmentStatus,
                sortType = sortType,
            ),
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(UserJobSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @PostMapping("/{jobId}")
    override fun addBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.addBookmark(userId, jobId)
        return SuccessResponse.created()
    }

    @DeleteMapping("/{jobId}")
    override fun deleteBookmark(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.deleteBookmark(userId, jobId)
        return SuccessResponse.ok()
    }

    @PutMapping("/{jobId}/application-status")
    override fun updateApplicationStatus(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: UpdateJobApplicationStatusRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobBookmarkService.changeApplicationStatus(userId, jobId, checkNotNull(request.applicationStatus))
        return SuccessResponse.ok()
    }
}
