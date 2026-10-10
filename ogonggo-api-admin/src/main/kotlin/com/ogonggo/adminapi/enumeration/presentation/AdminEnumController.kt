package com.ogonggo.adminapi.enumeration.presentation

import com.ogonggo.adminapi.enumeration.business.AdminEnumService
import com.ogonggo.adminapi.enumeration.presentation.response.AdminEnumOptionResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/enums")
class AdminEnumController(
    private val adminEnumService: AdminEnumService,
) : AdminEnumApi {

    @GetMapping
    override fun getEnums(): ResponseEntity<SuccessResponse<Map<String, List<AdminEnumOptionResponse>>>> =
        SuccessResponse.ok(
            adminEnumService.getEnums().mapValues { (_, options) -> options.map(AdminEnumOptionResponse::from) },
        )
}
