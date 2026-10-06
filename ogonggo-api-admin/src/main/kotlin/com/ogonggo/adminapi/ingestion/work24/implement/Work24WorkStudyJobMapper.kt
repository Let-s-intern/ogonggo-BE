package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.block
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.date
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.limit
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.number
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.section
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.text
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24WorkStudyPageDto
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.contentreview.domain.ContentSource
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 고용24 일학습병행 훈련과정 목록 항목(`scn_list`)과 과정·기관정보(`inst_base_info`)를 채용공고로 옮긴다.
 *
 * 일학습병행은 학습기업에 채용되어 일하면서 훈련기관 교육을 함께 받는 제도라 고용 형태가 일학습병행인 채용공고로 넣는다.
 * 등록 경로를 고용24로, 과정 ID와 회차(`trprId-trprDegr`)를 외부 식별값으로 두고 소유자 없이 곧바로 게시한다.
 * 고용24가 채용 조건을 주지 않아 다음처럼 채운다.
 * - 회사명: Open API에 학습기업 이름 항목이 없어 과정 상세 화면([Work24WorkStudyPageDto])의 학습기업 이름을 쓴다.
 *   화면을 못 읽었으면 과정명에서 뗀다. 과정명이 `2026년_공동훈련센터형_선박도장_L2_25V1_거제대학교_주식회사화인기업`처럼
 *   `_`로 이어지고 마지막이 학습기업이다(2026-09-30 기준 21건 모두 같은 모양). `_`도 없으면 훈련기관명을 쓴다.
 * - 담당 업무: Open API 값 뒤에 상세 화면의 훈련시간, 훈련목적, 주요 훈련 내용, 현장 교육훈련(OJT) 편성을 덧붙인다.
 * - 자격 요건: 상세 화면의 훈련 대상 요건. 뜻 없는 안내 문구만 있는 과정은 비운다.
 * - 직군·직무: 비운다. 과정은 NCS 코드로 분류되어 있고 오공고 직무 표([Work24JobRoles])는 직종코드 기준이다.
 * - 경력·학력: 조건이 없어 경력 무관, 학력 무관으로 둔다.
 * - 모집 기간: 목록에 없어 수집한 시각부터 훈련 시작일 끝까지로 둔다. 시작일이 지났으면 상시 채용이다.
 * - 근무 지역: 목록의 지역코드(`trngAreaCd`, 5자리 행정구역 코드)로 찾고, 없으면 주소(`address`)의 지역명으로 찾는다.
 */
internal object Work24WorkStudyJobMapper {

    fun sourceUrl(item: JsonNode): String? = item.text("titleLink")

    /** 부트캠프로 넣는 국민내일배움카드 과정과 파라미터가 같다. */
    fun detailParameters(item: JsonNode): Map<String, String> = Work24BootcampMapper.detailParameters(item)

    fun toAppendDto(
        item: JsonNode,
        detail: JsonNode,
        page: Work24WorkStudyPageDto?,
        sourceUrl: String,
        externalId: String,
        now: LocalDateTime,
    ): JobAppendDto {
        val base = detail.path("inst_base_info")

        val title = requireNotNull(base.text("trprNm") ?: item.text("title")) { "훈련과정명이 없습니다." }
        val institution = base.text("inoNm") ?: item.text("subTitle")
        val company = page?.company ?: title.substringAfterLast(COMPANY_SEPARATOR, "").trim().ifEmpty { null }
        val startDate = date(item.text("traStartDate"))
        val endDate = date(item.text("traEndDate"))
        val recruitmentEndAt = startDate?.atTime(LocalTime.of(23, 59, 59))
        val hasPeriod = recruitmentEndAt != null && !now.isAfter(recruitmentEndAt)
        val subRegion = Work24JobMapper.subRegion(item.text("trngAreaCd"), item.text("address"))

        return JobAppendDto(
            companyName = requireNotNull(company ?: institution) { "학습기업과 훈련기관 이름이 없습니다." }.limit(COMPANY_NAME_MAX),
            title = title.limit(TITLE_MAX),
            employmentType = JobEmploymentType.WORK_STUDY,
            experienceType = JobExperienceType.IRRELEVANT,
            region = subRegion?.region ?: Work24JobMapper.region(item.text("trngAreaCd"), item.text("address")),
            subRegion = subRegion,
            recruitmentType = if (hasPeriod) JobRecruitmentType.PERIOD else JobRecruitmentType.ALWAYS_OPEN,
            // 정원을 0으로 주는 과정이 많다. 0은 정원 없음으로 본다.
            recruitmentHeadcount = number(item.text("yardMan"))?.takeIf { it in 1..Int.MAX_VALUE }?.toInt(),
            recruitmentStartAt = if (hasPeriod) now else null,
            recruitmentEndAt = if (hasPeriod) recruitmentEndAt else null,
            companyAndTeamIntroduction = section(
                "학습기업" to company,
                "훈련기관" to institution,
                "훈련기관 주소" to listOfNotNull(base.text("addr1"), base.text("addr2")).joinToString(" ").ifBlank { null },
                "훈련기관 홈페이지" to base.text("hpAddr"),
            ),
            responsibilities = listOfNotNull(
                section(
                    "훈련 분야" to base.text("ncsNm"),
                    "훈련 유형" to item.text("trainTarget"),
                    "훈련 기간" to if (startDate != null && endDate != null) "$startDate ~ $endDate" else null,
                    "현장 교육훈련(OJT)" to page?.ojtHours,
                    "사업장 외 교육훈련(Off-JT)" to page?.offJtHours,
                ),
                block("훈련목적", page?.purpose),
                block("주요 훈련 내용", page?.mainContent),
                block("현장 교육훈련(OJT) 편성", page?.ojtSubjects?.joinToString("\n", transform = ::subject)),
            ).joinToString("\n\n").ifBlank { null },
            qualifications = page?.requirement,
            recruitmentNotice = section("문의 전화" to item.text("telNo")),
            applicationMethod = JobApplicationMethod.EXTERNAL_PAGE,
            sourceUrl = sourceUrl,
            publicationStatus = JobPublicationStatus.PUBLISHED,
            source = ContentSource.WORK24,
            externalId = externalId,
        )
    }

    /** `- 교과목: 능력단위 (필수, 120 시간)` 한 줄로 적는다. 교과목과 능력단위 이름이 같으면 한 번만 적는다. */
    private fun subject(subject: Work24WorkStudyPageDto.Subject): String {
        val name = listOfNotNull(subject.name, subject.unit?.takeIf { it != subject.name }).joinToString(": ")
        val note = listOfNotNull(subject.required, subject.hours).joinToString(", ")
        return "- $name" + if (note.isEmpty()) "" else " ($note)"
    }

    private const val COMPANY_SEPARATOR = "_"
    private const val COMPANY_NAME_MAX = 150
    private const val TITLE_MAX = 255
}
