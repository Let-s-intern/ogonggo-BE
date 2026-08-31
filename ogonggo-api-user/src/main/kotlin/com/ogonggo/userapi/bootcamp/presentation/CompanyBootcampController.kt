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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class CompanyBootcampController(
    private val companyBootcampService: CompanyBootcampService,
) : CompanyBootcampApi {

    override fun createBootcamp(
        userId: Long,
        request: CreateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyBootcampResponse>> =
        SuccessResponse.created(CreateCompanyBootcampResponse(companyBootcampService.create(userId, request.toCommand())))

    override fun getBootcamps(
        userId: Long,
        page: Int,
        size: Int,
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

    override fun getBootcamp(
        userId: Long,
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<CompanyBootcampDetailResponse>> =
        SuccessResponse.ok(CompanyBootcampDetailResponse.from(companyBootcampService.getBootcamp(userId, bootcampId)))

    override fun updateBootcamp(
        userId: Long,
        bootcampId: Long,
        request: UpdateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.update(userId, bootcampId, request.toCommand())
        return SuccessResponse.ok()
    }

    override fun startRecruitment(userId: Long, bootcampId: Long): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.startRecruitment(userId, bootcampId)
        return SuccessResponse.ok()
    }

    override fun closeBootcamp(userId: Long, bootcampId: Long): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.close(userId, bootcampId)
        return SuccessResponse.ok()
    }

    override fun deleteBootcamp(userId: Long, bootcampId: Long): ResponseEntity<SuccessResponse<Unit>> {
        companyBootcampService.delete(userId, bootcampId)
        return SuccessResponse.ok()
    }
}
