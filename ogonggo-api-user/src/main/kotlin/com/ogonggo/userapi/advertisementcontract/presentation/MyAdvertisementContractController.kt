package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractService
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementContractDetailResponse
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementContractSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me/advertisement-contracts")
class MyAdvertisementContractController(
    private val myAdvertisementContractService: MyAdvertisementContractService,
) : MyAdvertisementContractApi {

    @GetMapping
    override fun getContracts(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "status", required = false) statuses: List<AdvertisementContractStatus>?,
    ): ResponseEntity<SuccessResponse<PageResponse<MyAdvertisementContractSummaryResponse>>> {
        val result = myAdvertisementContractService.getContracts(
            userId = userId,
            statuses = statuses.orEmpty().toSet(),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(MyAdvertisementContractSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{contractId}")
    override fun getContract(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<MyAdvertisementContractDetailResponse>> =
        SuccessResponse.ok(MyAdvertisementContractDetailResponse.from(myAdvertisementContractService.getContract(userId, contractId)))
}
