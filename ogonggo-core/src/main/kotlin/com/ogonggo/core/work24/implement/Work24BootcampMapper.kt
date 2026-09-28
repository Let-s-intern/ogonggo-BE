package com.ogonggo.core.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.work24.implement.Work24Values.date
import com.ogonggo.core.work24.implement.Work24Values.limit
import com.ogonggo.core.work24.implement.Work24Values.number
import com.ogonggo.core.work24.implement.Work24Values.section
import com.ogonggo.core.work24.implement.Work24Values.text
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 고용24 훈련과정 목록 항목(`scn_list`)과 과정·기관정보(`inst_base_info`, `inst_detail_info`)를 부트캠프로 옮긴다.
 *
 * 크롤러 부트캠프처럼 소유자 없이 모집 중으로 곧바로 게시한다. 같은 과정인지는 과정 링크(`titleLink`)를
 * 원문 URL로 보고 판단한다. 고용24에 없는 값은 다음처럼 채운다.
 * - 대표 이미지: 고용24가 이미지를 주지 않아 설정의 공통 이미지를 쓴다.
 * - 모집 기간: 목록에 없어 수집한 시각부터 개강일 끝까지로 둔다.
 * - 진행 방식: 과정명·훈련대상에 원격·인터넷이 있으면 온라인, 혼합이 있으면 온·오프라인, 그 밖에는 오프라인으로 본다.
 */
internal object Work24BootcampMapper {

    fun sourceUrl(item: JsonNode): String? = item.text("titleLink")

    fun detailParameters(item: JsonNode): Map<String, String> = mapOf(
        "srchTrprId" to requireNotNull(item.text("trprId")) { "훈련과정 ID가 없습니다." },
        "srchTrprDegr" to requireNotNull(item.text("trprDegr")) { "훈련과정 회차가 없습니다." },
        "srchTorgId" to requireNotNull(item.text("trainstCstId")) { "훈련기관 ID가 없습니다." },
    )

    fun toAppendDto(
        target: Work24CollectionTarget,
        item: JsonNode,
        detail: JsonNode,
        sourceUrl: String,
        representativeImageUrl: String,
        now: LocalDateTime,
    ): BootcampAppendDto {
        val base = detail.path("inst_base_info")
        val detailInfo = detail.path("inst_detail_info")

        val title = requireNotNull(base.text("trprNm") ?: item.text("title")) { "훈련과정명이 없습니다." }
        val programStartDate = requireNotNull(date(item.text("traStartDate"))) { "훈련 시작일이 없습니다." }
        val programEndDate = requireNotNull(date(item.text("traEndDate"))) { "훈련 종료일이 없습니다." }
        val recruitmentEndAt = programStartDate.atTime(LocalTime.of(23, 59, 59))
        val hasPeriod = !now.isAfter(recruitmentEndAt)
        val totalHours = base.text("trtm") ?: detailInfo.text("totTraingTime")

        return BootcampAppendDto(
            companyName = requireNotNull(base.text("inoNm") ?: item.text("subTitle")) { "훈련기관명이 없습니다." }
                .limit(COMPANY_NAME_MAX),
            title = title.limit(TITLE_MAX),
            programType = (base.text("trprTargetNm") ?: target.api.service.desc).limit(PROGRAM_TYPE_MAX),
            operationType = operationType(title, item.text("trainTarget")),
            recruitmentType = if (hasPeriod) BootcampRecruitmentType.PERIOD else BootcampRecruitmentType.ALWAYS_OPEN,
            recruitmentStartAt = if (hasPeriod) now else null,
            recruitmentEndAt = if (hasPeriod) recruitmentEndAt else null,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = number(item.text("yardMan"))?.takeIf { it <= Int.MAX_VALUE }?.toInt(),
            tuitionType = TuitionType.GOVERNMENT_FUNDED,
            tuitionAmount = number(item.text("courseMan") ?: item.text("realMan")),
            representativeImageUrl = representativeImageUrl,
            shortDescription = listOfNotNull(base.text("ncsNm"), totalHours?.let { "총 ${it}시간" })
                .joinToString(" · ")
                .ifBlank { title }
                .limit(SHORT_DESCRIPTION_MAX),
            content = requireNotNull(
                section(
                    "훈련기관" to (base.text("inoNm") ?: item.text("subTitle")),
                    "주소" to listOfNotNull(base.text("addr1"), base.text("addr2")).joinToString(" ").ifBlank { null },
                    "훈련 기간" to "$programStartDate ~ $programEndDate",
                    "총 훈련일수" to (base.text("trDcnt") ?: detailInfo.text("totTraingDyct"))?.let { "${it}일" },
                    "총 훈련시간" to totalHours?.let { "${it}시간" },
                    "훈련 분야" to (base.text("ncsNm") ?: detailInfo.text("govBusiNm")),
                    "훈련 대상" to item.text("trainTarget"),
                    "수강비" to item.text("courseMan")?.let { "${it}원" },
                    "실제 훈련비" to (base.text("instPerTrco") ?: item.text("realMan"))?.let { "${it}원" },
                    "정부 지원금" to base.text("perTrco")?.let { "${it}원" },
                    "본인 부담액" to detailInfo.text("tgcrGnrlTrneOwepAllt")?.let { "${it}원" },
                    "만족도" to item.text("stdgScor"),
                    "3개월 취업률" to item.text("eiEmplRate3")?.let { "$it%" },
                    "담당자" to base.text("trprChap"),
                    "담당자 전화" to (base.text("trprChapTel") ?: item.text("telNo")),
                ),
            ),
            applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
            applicationUrl = sourceUrl,
            managerEmail = base.text("trprChapEmail")?.limit(EMAIL_MAX),
            inquiryUrl = base.text("hpAddr"),
            sourceUrl = sourceUrl,
            status = BootcampStatus.RECRUITING,
            publicationStatus = BootcampPublicationStatus.PUBLISHED,
        )
    }

    private fun operationType(title: String, trainTarget: String?): OperationType {
        val text = "$title ${trainTarget.orEmpty()}"
        return when {
            text.contains("혼합") -> OperationType.HYBRID
            text.contains("원격") || text.contains("인터넷") -> OperationType.ONLINE
            else -> OperationType.OFFLINE
        }
    }

    private const val COMPANY_NAME_MAX = 150
    private const val TITLE_MAX = 255
    private const val PROGRAM_TYPE_MAX = 50
    private const val SHORT_DESCRIPTION_MAX = 500
    private const val EMAIL_MAX = 320
}
