package com.ogonggo.adminapi.advertisementreport.presentation

import com.ogonggo.adminapi.advertisementreport.business.AdminAdvertisementContractReportService
import com.ogonggo.adminapi.advertisementreport.business.AdvertisementContractReportCsvExporter
import com.ogonggo.adminapi.advertisementreport.presentation.response.AdminAdvertisementContractReportResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/admin/advertisement-contracts")
class AdminAdvertisementContractReportController(
    private val reportService: AdminAdvertisementContractReportService,
    private val csvExporter: AdvertisementContractReportCsvExporter,
) : AdminAdvertisementContractReportApi {

    @GetMapping("/{contractId}/report")
    override fun getReport(
        @PathVariable("contractId") contractId: Long,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementContractReportResponse>> =
        SuccessResponse.ok(
            AdminAdvertisementContractReportResponse.from(reportService.getReport(contractId, from, to)),
        )

    @GetMapping("/{contractId}/report.csv", produces = ["text/csv"])
    override fun exportReportCsv(
        @PathVariable("contractId") contractId: Long,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<ByteArray> {
        val report = reportService.getReport(contractId, from, to)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("advertisement-report-$contractId.csv", StandardCharsets.UTF_8)
                    .build()
                    .toString(),
            )
            .body(csvExporter.export(report))
    }
}
