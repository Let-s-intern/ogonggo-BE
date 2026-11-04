package com.ogonggo.adminapi.companycontentpriority.presentation

import com.ogonggo.adminapi.companycontentpriority.business.AdminCompanyContentPriorityService
import com.ogonggo.adminapi.companycontentpriority.presentation.request.ReplaceAdminCompanyContentPriorityRequest
import com.ogonggo.adminapi.companycontentpriority.presentation.response.AdminCompanyContentPriorityResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/company-content-priority")
class AdminCompanyContentPriorityController(
    private val adminCompanyContentPriorityService: AdminCompanyContentPriorityService,
) : AdminCompanyContentPriorityApi {

    @GetMapping
    override fun getPriority(): ResponseEntity<SuccessResponse<AdminCompanyContentPriorityResponse>> =
        SuccessResponse.ok(AdminCompanyContentPriorityResponse(adminCompanyContentPriorityService.isEnabled()))

    @PutMapping
    override fun replacePriority(
        @RequestBody request: ReplaceAdminCompanyContentPriorityRequest,
    ): ResponseEntity<SuccessResponse<AdminCompanyContentPriorityResponse>> {
        adminCompanyContentPriorityService.replace(checkNotNull(request.enabled))
        return SuccessResponse.ok(AdminCompanyContentPriorityResponse(adminCompanyContentPriorityService.isEnabled()))
    }
}
