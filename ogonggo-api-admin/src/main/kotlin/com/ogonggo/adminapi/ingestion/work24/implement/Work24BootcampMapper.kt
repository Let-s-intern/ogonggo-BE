package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.date
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.limit
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.number
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.section
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.text
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.review.domain.ContentSource
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 고용24 훈련과정 목록 항목(`scn_list`)과 과정·기관정보(`inst_base_info`, `inst_detail_info`)를 부트캠프로 옮긴다.
 *
 * 등록 경로를 고용24로, 과정 ID와 회차(`trprId-trprDegr`)를 외부 식별값으로 두고 모집 중으로 곧바로 게시한다.
 * 고용24에 없는 값은 다음처럼 채운다.
 * - 대표 이미지·로고: 비운다. 고용24는 과정 이미지를 주지 않고, 훈련기관 로고(`filePath`)는 다운로드 주소라
 *   이미지로 열리지 않는다. 비어 있으면 클라이언트가 기본 이미지를 그린다.
 * - 모집 기간: 목록에 없어 수집한 시각부터 개강일 끝까지로 둔다.
 * - 수강료: 교육생이 내는 본인부담액(`tgcrGnrlTrneOwepAllt`)을 쓴다. 목록의 수강비(`courseMan`)는 정부 지원금을 포함한 총 훈련비다.
 * - 진행 방식: 훈련방법 코드(`traingMthCd`)로 정한다. M1005 인터넷은 온라인, M1010 혼합·M1014 스마트혼합은 온·오프라인, 그 밖은 오프라인이다.
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
        externalId: String,
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
            programType = (target.programType ?: target.api.service.desc).limit(PROGRAM_TYPE_MAX),
            operationType = operationType(base.text("traingMthCd")),
            recruitmentType = if (hasPeriod) BootcampRecruitmentType.PERIOD else BootcampRecruitmentType.ALWAYS_OPEN,
            recruitmentStartAt = if (hasPeriod) now else null,
            recruitmentEndAt = if (hasPeriod) recruitmentEndAt else null,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            // 일학습병행은 정원을 0으로 주는 경우가 있어 0은 정원 없음으로 본다.
            capacity = number(item.text("yardMan"))?.takeIf { it in 1..Int.MAX_VALUE }?.toInt(),
            tuitionType = TuitionType.GOVERNMENT_FUNDED,
            tuitionAmount = number(detailInfo.text("tgcrGnrlTrneOwepAllt")),
            representativeImageUrl = null,
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
                    "본인 부담액" to detailInfo.text("tgcrGnrlTrneOwepAllt")?.let { "${it}원" },
                    "총 훈련비" to (base.text("instPerTrco") ?: item.text("realMan"))?.let { "${it}원" },
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
            source = ContentSource.WORK24,
            externalId = externalId,
        )
    }

    private fun operationType(trainingMethodCode: String?): OperationType = when (trainingMethodCode) {
        "M1005" -> OperationType.ONLINE
        "M1010", "M1014" -> OperationType.HYBRID
        else -> OperationType.OFFLINE
    }

    private const val COMPANY_NAME_MAX = 150
    private const val TITLE_MAX = 255
    private const val PROGRAM_TYPE_MAX = 50
    private const val SHORT_DESCRIPTION_MAX = 500
    private const val EMAIL_MAX = 320
}
