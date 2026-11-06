package com.ogonggo.adminapi.advertisercontract.presentation

import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractService
import com.ogonggo.adminapi.advertisercontract.presentation.request.CreateAdminAdvertiserContractRequest
import com.ogonggo.adminapi.advertisercontract.presentation.request.UpdateAdminAdvertiserContractRequest
import com.ogonggo.adminapi.advertisercontract.presentation.response.AdminAdvertiserContractIdResponse
import com.ogonggo.adminapi.advertisercontract.presentation.response.AdminAdvertiserContractResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 목록·등록은 광고주 아래(`/advertisers/{advertiserId}/contracts`), 수정·삭제는 계약 하나(`/advertiser-contracts/{contractId}`)로 둔다. */
@Validated
@RestController
@RequestMapping("/api/v1/admin")
class AdminAdvertiserContractController(
    private val adminAdvertiserContractService: AdminAdvertiserContractService,
) : AdminAdvertiserContractApi {

    @GetMapping("/advertisers/{advertiserId}/contracts")
    override fun getContracts(
        @PathVariable("advertiserId") advertiserId: Long,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertiserContractResponse>>> =
        SuccessResponse.ok(adminAdvertiserContractService.getContracts(advertiserId).map(AdminAdvertiserContractResponse::from))

    /** 등록이 커밋된 뒤의 계약을 다시 읽어 응답한다. */
    @PostMapping("/advertisers/{advertiserId}/contracts")
    override fun createContract(
        @PathVariable("advertiserId") advertiserId: Long,
        @RequestBody request: CreateAdminAdvertiserContractRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserContractResponse>> {
        val contractId = adminAdvertiserContractService.createContract(advertiserId, request.toCommand())
        return SuccessResponse.created(AdminAdvertiserContractResponse.from(adminAdvertiserContractService.getContract(contractId)))
    }

    /** 수정이 커밋된 뒤의 계약을 다시 읽어 응답한다. */
    @PatchMapping("/advertiser-contracts/{contractId}")
    override fun updateContract(
        @PathVariable("contractId") contractId: Long,
        @RequestBody request: UpdateAdminAdvertiserContractRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserContractResponse>> {
        adminAdvertiserContractService.updateContract(contractId, request.toCommand())
        return SuccessResponse.ok(AdminAdvertiserContractResponse.from(adminAdvertiserContractService.getContract(contractId)))
    }

    @DeleteMapping("/advertiser-contracts/{contractId}")
    override fun deleteContract(
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserContractIdResponse>> {
        adminAdvertiserContractService.deleteContract(contractId)
        return SuccessResponse.ok(AdminAdvertiserContractIdResponse(contractId))
    }
}
