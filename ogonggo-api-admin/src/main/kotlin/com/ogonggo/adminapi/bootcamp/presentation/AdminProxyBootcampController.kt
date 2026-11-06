package com.ogonggo.adminapi.bootcamp.presentation

import com.ogonggo.adminapi.bootcamp.business.AdminBootcampService
import com.ogonggo.adminapi.bootcamp.business.AdminProxyBootcampService
import com.ogonggo.adminapi.bootcamp.presentation.request.CreateProxyBootcampRequest
import com.ogonggo.adminapi.bootcamp.presentation.response.AdminBootcampDetailResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/company-members/{userId}/bootcamps")
class AdminProxyBootcampController(
    private val adminProxyBootcampService: AdminProxyBootcampService,
    private val adminBootcampService: AdminBootcampService,
) : AdminProxyBootcampApi {

    /** 등록이 커밋된 뒤의 과정을 다시 읽어 응답한다. */
    @PostMapping
    override fun createCompanyMemberBootcamp(
        @PathVariable("userId") userId: Long,
        @RequestBody request: CreateProxyBootcampRequest,
        @AuthenticationPrincipal adminUserId: Long,
    ): ResponseEntity<SuccessResponse<AdminBootcampDetailResponse>> {
        val bootcampId = adminProxyBootcampService.create(userId, request.toCommand(), adminUserId)
        return SuccessResponse.created(AdminBootcampDetailResponse.from(adminBootcampService.getBootcamp(bootcampId)))
    }
}
