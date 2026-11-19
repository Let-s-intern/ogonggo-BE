package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.nio.charset.StandardCharsets

/** 리포트 CSV 다운로드다. 엑셀이 UTF-8로 열도록 BOM을 붙인다. 날짜별·소재별 한 줄이다. */
internal object AdvertisementReportCsv {

    val MEDIA_TYPE: MediaType = MediaType("text", "csv", StandardCharsets.UTF_8)

    private val COLUMNS = listOf(
        "date", "exposureId", "exposureName", "impressions", "adClicks", "views", "uniqueVisitors",
        "memberViews", "ctaClicks", "ctaUniqueVisitors", "avgScrollPercent", "avgDurationSeconds",
    )

    fun response(report: AdvertisementContractReportDto): ResponseEntity<String> {
        val lines = listOf(COLUMNS.joinToString(",")) + report.csvRows.map { row ->
            listOf(
                row.date, row.exposureId, row.exposureName, row.impressions, row.adClicks, row.views, row.uniqueVisitors,
                row.memberViews, row.ctaClicks, row.ctaUniqueVisitors, row.avgScrollPercent, row.avgDurationSeconds,
            ).joinToString(",") { escape(it?.toString() ?: "") }
        }
        val fileName = "report-${report.product.contractProductId}-${report.from}_${report.to}.csv"
        return ResponseEntity.ok()
            .contentType(MEDIA_TYPE)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
            .body("﻿" + lines.joinToString("\n") + "\n")
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
