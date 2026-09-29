package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.LetsCareerSyncService
import com.ogonggo.userapi.user.presentation.request.LetsCareerJobProfileSyncRequest
import com.ogonggo.userapi.user.presentation.response.LetsCareerJobProfileSyncResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/internal/letscareer-users")
class LetsCareerSyncController(
    private val letsCareerSyncService: LetsCareerSyncService,
) : LetsCareerSyncApi {

    @PutMapping("/{letsCareerUserId}/job-profile")
    override fun replaceJobProfile(
        @PathVariable("letsCareerUserId") letsCareerUserId: Long,
        @RequestBody request: LetsCareerJobProfileSyncRequest,
    ): ResponseEntity<SuccessResponse<LetsCareerJobProfileSyncResponse>> {
        val applied = letsCareerSyncService.applyJobProfile(letsCareerUserId, request.toCommand(), request.updatedAt)
        return SuccessResponse.ok(LetsCareerJobProfileSyncResponse(applied = applied))
    }
}
