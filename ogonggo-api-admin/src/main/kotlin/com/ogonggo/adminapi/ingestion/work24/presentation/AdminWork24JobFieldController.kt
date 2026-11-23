package com.ogonggo.adminapi.ingestion.work24.presentation

import com.ogonggo.adminapi.ingestion.work24.business.AdminWork24JobFieldService
import com.ogonggo.adminapi.ingestion.work24.presentation.request.ReplaceAdminWork24JobFieldRequest
import com.ogonggo.adminapi.ingestion.work24.presentation.response.AdminWork24JobFieldResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.job.domain.JobField
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/work24-job-fields")
class AdminWork24JobFieldController(
    private val adminWork24JobFieldService: AdminWork24JobFieldService,
) : AdminWork24JobFieldApi {

    @GetMapping
    override fun getJobFields(): ResponseEntity<SuccessResponse<List<AdminWork24JobFieldResponse>>> =
        SuccessResponse.ok(AdminWork24JobFieldResponse.listOf(adminWork24JobFieldService.findAll()))

    @PutMapping("/{jobField}")
    override fun replaceJobField(
        @PathVariable jobField: JobField,
        @RequestBody request: ReplaceAdminWork24JobFieldRequest,
    ): ResponseEntity<SuccessResponse<AdminWork24JobFieldResponse>> {
        adminWork24JobFieldService.replace(jobField, checkNotNull(request.enabled))
        val enabled = adminWork24JobFieldService.findAll().getValue(jobField)
        return SuccessResponse.ok(AdminWork24JobFieldResponse(jobField, enabled))
    }
}
