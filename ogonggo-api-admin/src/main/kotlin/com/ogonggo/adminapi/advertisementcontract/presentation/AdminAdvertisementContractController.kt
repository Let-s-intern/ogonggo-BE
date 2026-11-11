package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractService
import com.ogonggo.adminapi.advertisementcontract.business.AdminBusinessAccountService
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminBusinessAccountIssueResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.request.AddAdminAdvertisementContractProductRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.request.ChangeAdminAdvertisementContractPaymentAmountRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.request.ChangeAdminAdvertisementContractStatusRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementContractDetailResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementContractSummaryResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.CreateAdminAdvertisementContractProductResponse
import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/admin/advertisement-contracts")
class AdminAdvertisementContractController(
    private val adminAdvertisementContractService: AdminAdvertisementContractService,
    private val adminBusinessAccountService: AdminBusinessAccountService,
) : AdminAdvertisementContractApi {

    @GetMapping
    override fun getContracts(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "source", required = false) source: AdvertisementContractSource?,
        @RequestParam(name = "status", required = false) statuses: List<AdvertisementContractStatus>?,
        @RequestParam(name = "promotionTarget", required = false) promotionTarget: AdvertisementContractPromotionTarget?,
        @RequestParam(name = "category", required = false) category: AdvertisementProductCategory?,
        @RequestParam(name = "companyName", required = false) companyName: String?,
        @RequestParam(name = "createdFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        createdFrom: LocalDate?,
        @RequestParam(name = "createdTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        createdTo: LocalDate?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAdvertisementContractSummaryResponse>>> {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw InvalidRequestParameterException("createdFrom", "생성 기간 시작일은 종료일보다 늦을 수 없습니다.")
        }

        val result = adminAdvertisementContractService.getContracts(
            condition = AdvertisementContractSearchCondition(
                source = source,
                statuses = statuses.orEmpty().toSet(),
                promotionTarget = promotionTarget,
                category = category,
                companyName = companyName,
                createdFrom = createdFrom,
                createdTo = createdTo,
            ),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminAdvertisementContractSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{contractId}")
    override fun getContract(
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementContractDetailResponse>> =
        SuccessResponse.ok(
            AdminAdvertisementContractDetailResponse.from(adminAdvertisementContractService.getContract(contractId)),
        )

    @PatchMapping("/{contractId}/status")
    override fun changeStatus(
        @PathVariable("contractId") contractId: Long,
        @RequestBody request: ChangeAdminAdvertisementContractStatusRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementContractService.changeStatus(contractId, request.status)
        return SuccessResponse.ok()
    }

    @PatchMapping("/{contractId}/payment-amount")
    override fun changePaymentAmount(
        @PathVariable("contractId") contractId: Long,
        @RequestBody request: ChangeAdminAdvertisementContractPaymentAmountRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementContractService.changePaymentAmount(contractId, request.paymentAmount)
        return SuccessResponse.ok()
    }

    @PostMapping("/{contractId}/products")
    override fun addProduct(
        @PathVariable("contractId") contractId: Long,
        @RequestBody request: AddAdminAdvertisementContractProductRequest,
    ): ResponseEntity<SuccessResponse<CreateAdminAdvertisementContractProductResponse>> =
        SuccessResponse.created(
            CreateAdminAdvertisementContractProductResponse(
                adminAdvertisementContractService.addProduct(contractId, request.productId),
            ),
        )

    @DeleteMapping("/{contractId}/products/{contractProductId}")
    override fun removeProduct(
        @PathVariable("contractId") contractId: Long,
        @PathVariable("contractProductId") contractProductId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementContractService.removeProduct(contractId, contractProductId)
        return SuccessResponse.ok()
    }

    @PostMapping("/{contractId}/business-account")
    override fun issueBusinessAccount(
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<AdminBusinessAccountIssueResponse>> =
        SuccessResponse.ok(AdminBusinessAccountIssueResponse.from(adminBusinessAccountService.issue(contractId)))
}
