package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementReportService
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyActiveAdvertisementReportResponse
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementReportResponse
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementReportTotalResponse
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
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/users/me")
class MyAdvertisementReportController(
    private val myAdvertisementReportService: MyAdvertisementReportService,
) : MyAdvertisementReportApi {

    @GetMapping("/advertisement-contracts/{contractId}/products/{contractProductId}/report")
    override fun getReport(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("contractId") contractId: Long,
        @PathVariable("contractProductId") contractProductId: Long,
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<SuccessResponse<MyAdvertisementReportResponse>> =
        SuccessResponse.ok(MyAdvertisementReportResponse.from(myAdvertisementReportService.getReport(userId, contractId, contractProductId, from, to)))

    @GetMapping("/advertisement-contracts/{contractId}/report-summary")
    override fun getReportSummary(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<MyAdvertisementReportTotalResponse>> =
        SuccessResponse.ok(MyAdvertisementReportTotalResponse.from(myAdvertisementReportService.getTotal(userId, contractId)))

    @GetMapping("/advertisement-reports/active")
    override fun getActiveReports(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SuccessResponse<List<MyActiveAdvertisementReportResponse>>> =
        SuccessResponse.ok(myAdvertisementReportService.getActive(userId).map(MyActiveAdvertisementReportResponse::from))

    @GetMapping("/advertisement-contracts/{contractId}/products/{contractProductId}/report.csv", produces = ["text/csv;charset=UTF-8"])
    override fun getReportCsv(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("contractId") contractId: Long,
        @PathVariable("contractProductId") contractProductId: Long,
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<String> =
        AdvertisementReportCsv.response(myAdvertisementReportService.getReport(userId, contractId, contractProductId, from, to))
}
