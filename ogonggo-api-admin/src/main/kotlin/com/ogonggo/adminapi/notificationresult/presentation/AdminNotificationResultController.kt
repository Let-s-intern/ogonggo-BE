package com.ogonggo.adminapi.notificationresult.presentation

import com.ogonggo.adminapi.notificationresult.business.AdminNotificationResultService
import com.ogonggo.adminapi.notificationresult.presentation.response.AdminNotificationRecentResultsResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/notifications")
class AdminNotificationResultController(
    private val service: AdminNotificationResultService,
) : AdminNotificationResultApi {
    @GetMapping("/recent-results")
    override fun getRecentNotificationResults(): ResponseEntity<SuccessResponse<AdminNotificationRecentResultsResponse>> =
        SuccessResponse.ok(AdminNotificationRecentResultsResponse.from(service.getRecent()))
}
