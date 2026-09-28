package com.ogonggo.adminapi.ingestion.work24.presentation

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.ingestion.work24.business.AdminWork24Service
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/work24")
class AdminWork24Controller(
    private val adminWork24Service: AdminWork24Service,
) : AdminWork24Api {

    /** 고용24 파라미터는 API마다 달라 이름을 정하지 않고 query 전체를 받는다. */
    @GetMapping("/{$WORK24_API_PATH_VARIABLE}")
    override fun getWork24ApiResponse(
        @PathVariable(WORK24_API_PATH_VARIABLE) apiName: String,
        @RequestParam parameters: Map<String, String>,
    ): ResponseEntity<SuccessResponse<JsonNode>> =
        SuccessResponse.ok(adminWork24Service.fetch(parseWork24Api(apiName), parameters))
}
