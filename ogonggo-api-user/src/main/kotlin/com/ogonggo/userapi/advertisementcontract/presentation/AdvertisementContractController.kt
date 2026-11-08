package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractService
import com.ogonggo.userapi.advertisementcontract.presentation.request.CreateAdvertisementContractRequest
import com.ogonggo.userapi.advertisementcontract.presentation.response.CreateAdvertisementContractResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/advertisement-contracts")
class AdvertisementContractController(
    private val advertisementContractService: AdvertisementContractService,
) : AdvertisementContractApi {

    @PostMapping
    override fun createContract(
        @AuthenticationPrincipal userId: Long?,
        @RequestBody request: CreateAdvertisementContractRequest,
    ): ResponseEntity<SuccessResponse<CreateAdvertisementContractResponse>> =
        SuccessResponse.created(
            CreateAdvertisementContractResponse(
                advertisementContractService.createContract(userId, request.toCommand()),
            ),
        )
}
