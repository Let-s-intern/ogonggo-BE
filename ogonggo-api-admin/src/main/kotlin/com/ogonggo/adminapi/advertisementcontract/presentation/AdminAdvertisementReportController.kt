package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementReportService
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementReportResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementReportTotalResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/admin/advertisement-contracts/{contractId}")
class AdminAdvertisementReportController(
    private val adminAdvertisementReportService: AdminAdvertisementReportService,
) : AdminAdvertisementReportApi {

    @GetMapping("/products/{contractProductId}/report")
    override fun getReport(
        @PathVariable("contractId") contractId: Long,
        @PathVariable("contractProductId") contractProductId: Long,
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementReportResponse>> =
        SuccessResponse.ok(AdminAdvertisementReportResponse.from(adminAdvertisementReportService.getReport(contractId, contractProductId, from, to)))

    @GetMapping("/report-summary")
    override fun getReportSummary(
        @PathVariable("contractId") contractId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementReportTotalResponse>> =
        SuccessResponse.ok(AdminAdvertisementReportTotalResponse.from(adminAdvertisementReportService.getTotal(contractId)))

    @GetMapping("/products/{contractProductId}/report.csv", produces = ["text/csv;charset=UTF-8"])
    override fun getReportCsv(
        @PathVariable("contractId") contractId: Long,
        @PathVariable("contractProductId") contractProductId: Long,
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<String> =
        AdvertisementReportCsv.response(adminAdvertisementReportService.getReport(contractId, contractProductId, from, to))
}
