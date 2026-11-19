package com.ogonggo.userapi.advertisementevent.presentation

import com.ogonggo.userapi.advertisementevent.business.AdvertisementContractReportService
import com.ogonggo.userapi.advertisementevent.presentation.response.AdvertisementContractReportResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/advertisement-contracts")
class AdvertisementContractReportController(
    private val advertisementContractReportService: AdvertisementContractReportService,
    private val clock: Clock,
) : AdvertisementContractReportApi {

    @GetMapping("/{contractId}/report")
    override fun getReport(
        @AuthenticationPrincipal userId: Long,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<AdvertisementContractReportResponse>> =
        SuccessResponse.ok(
            AdvertisementContractReportResponse.from(
                advertisementContractReportService.getForCompanyMember(userId, contractId, from, to),
                Instant.now(clock),
            ),
        )
}
