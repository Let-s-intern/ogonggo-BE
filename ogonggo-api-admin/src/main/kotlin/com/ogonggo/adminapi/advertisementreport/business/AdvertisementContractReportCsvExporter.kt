package com.ogonggo.adminapi.advertisementreport.business

import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import org.springframework.stereotype.Component

@Component
class AdvertisementContractReportCsvExporter {

    fun export(report: AdvertisementContractReportDto): ByteArray {
        val rows = mutableListOf<List<String>>(
            listOf("구분", "날짜", "카테고리", "지면", "지표", "총 건수", "순 사용자", "사용자당"),
            listOf("요약", "${report.from}~${report.to}", "전체", "전체", "노출", report.summary.impressions.total.toString(), report.summary.impressions.uniqueUsers.toString(), report.summary.impressions.perUser.toString()),
            listOf("요약", "${report.from}~${report.to}", "전체", "전체", "클릭", report.summary.clicks.total.toString(), report.summary.clicks.uniqueUsers.toString(), report.summary.clicks.perUser.toString()),
        )

        report.daily.forEach { metric ->
            val category = metric.category.desc
            val position = metric.position?.desc ?: "광고 공고"
            rows += listOf("일별", metric.date.toString(), category, position, "노출", metric.impressions.total.toString(), metric.impressions.uniqueUsers.toString(), metric.impressions.perUser.toString())
            rows += listOf("일별", metric.date.toString(), category, position, "클릭", metric.clicks.total.toString(), metric.clicks.uniqueUsers.toString(), metric.clicks.perUser.toString())
        }

        report.positions.forEach { metric ->
            rows += listOf("지면별", "${report.from}~${report.to}", metric.category.desc, metric.position?.desc ?: "광고 공고", "클릭", metric.clicks.total.toString(), metric.clicks.uniqueUsers.toString(), metric.clicks.perUser.toString())
        }

        report.jobRoleDistribution.forEach { metric ->
            rows += listOf("클릭 회원 직무", "${report.from}~${report.to}", "", metric.label, "고유 클릭 회원", "", metric.uniqueClickers.toString(), "")
        }

        val csv = rows.joinToString("\r\n") { row -> row.joinToString(",") { escape(it) } }
        return ("\uFEFF" + csv + "\r\n").toByteArray(Charsets.UTF_8)
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value
}
