package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.userapi.bootcamp.business.CompanyBootcampService
import com.ogonggo.userapi.bootcamp.presentation.request.CreateCompanyBootcampRequest
import com.ogonggo.userapi.bootcamp.presentation.request.UpdateCompanyBootcampRequest
import com.ogonggo.userapi.bootcamp.presentation.response.CompanyBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.CompanyBootcampSummaryResponse
import com.ogonggo.userapi.bootcamp.presentation.response.CreateCompanyBootcampResponse
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
@RequestMapping("/api/v1/users/me/bootcamps")
class CompanyBootcampController(
    private val companyBootcampService: CompanyBootcampService,
) : CompanyBootcampApi {

    @PostMapping
    override fun createBootcamp(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: CreateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyBootcampResponse>> =
        SuccessResponse.created(CreateCompanyBootcampResponse(companyBootcampService.create(userId, request.toCommand())))

    @GetMapping
    override fun getBootcamps(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<CompanyBootcampSummaryResponse>>> {
        val result = companyBootcampService.getBootcamps(userId, page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(CompanyBootcampSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{bootcampId}")
    override fun getBootcamp(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<CompanyBootcampDetailResponse>> =
        SuccessResponse.ok(CompanyBootcampDetailResponse.from(companyBootcampService.getBootcamp(userId, bootcampId)))

    @PutMapping("/{bootcampId}")
    override fun updateBootcamp(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
        @RequestBody request: UpdateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.update(userId, bootcampId, request.toCommand())
        return SuccessResponse.ok()
    }

    @PostMapping("/{bootcampId}/start-recruitment")
    override fun startRecruitment(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.startRecruitment(userId, bootcampId)
        return SuccessResponse.ok()
    }

    @PostMapping("/{bootcampId}/close")
    override fun closeBootcamp(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.close(userId, bootcampId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{bootcampId}")
    override fun deleteBootcamp(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.delete(userId, bootcampId)
        return SuccessResponse.ok()
    }
}
