package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.date
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.limit
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.number
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.section
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.text
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import java.time.LocalTime

/**
 * 고용24 채용정보 목록 항목(`wanted`)과 상세(`wantedDtl`)를 채용공고로 옮긴다.
 *
 * 등록 경로를 고용24로, 구인인증번호(`wantedAuthNo`)를 외부 식별값으로 두고 소유자 없이 곧바로 게시한다.
 * 원문 URL은 워크넷 채용정보 URL(`wantedInfoUrl`)이다.
 * 마감일은 상세(`receiptCloseDt`)가 `채용시까지`처럼 날짜 없이 오고 목록(`closeDt`)에만 날짜가 있는 경우가 있어
 * 날짜가 있는 쪽을 쓴다. 급여도 목록의 `연봉`·`3000만원 ~ 3500만원`이 상세보다 읽기 좋아 목록을 먼저 쓴다. 직군·직무는 목록의 직종코드(`jobsCd`)를 [Work24JobRoles] 표로 옮긴다.
 * 근무 지역은 도로명코드(`strtnmCd`) 앞 5자리 행정구역 코드로 찾고, 코드가 없으면 지역명(`region`)으로 찾는다.
 * 코드 값의 뜻은 고용24 개발명세를 따른다.
 */
internal object Work24JobMapper {

    fun sourceUrl(item: JsonNode): String? = item.text("wantedInfoUrl")

    fun detailParameters(item: JsonNode): Map<String, String> =
        mapOf("wantedAuthNo" to requireNotNull(item.text("wantedAuthNo")) { "구인인증번호가 없습니다." })

    fun toAppendDto(item: JsonNode, detail: JsonNode, sourceUrl: String, externalId: String): JobAppendDto {
        val corp = detail.path("corpInfo")
        val info = detail.path("wantedInfo")
        val charge = detail.path("empchargeInfo")

        val closeTexts = listOfNotNull(info.text("receiptCloseDt"), item.text("closeDt"))
        val start = date(item.text("regDt"))?.atStartOfDay()
        val end = closeTexts.firstNotNullOfOrNull(::date)?.atTime(LocalTime.of(23, 59, 59))
        val hasPeriod = start != null && end != null && !start.isAfter(end)

        val subRegion = subRegion(item.text("strtnmCd"), item.text("region"))
        val jobRole = Work24JobRoles.of(item.text("jobsCd"))

        return JobAppendDto(
            companyName = requireNotNull(corp.text("corpNm") ?: item.text("company")) { "회사명이 없습니다." }
                .limit(COMPANY_NAME_MAX),
            title = requireNotNull(info.text("wantedTitle") ?: item.text("title")) { "채용 제목이 없습니다." }
                .limit(TITLE_MAX),
            jobField = jobRole?.jobField,
            jobRole = jobRole,
            industry = (corp.text("indTpCdNm") ?: item.text("indTpNm"))?.limit(CATEGORY_MAX),
            employmentType = employmentType(info.text("empTpCd") ?: item.text("empTpCd")),
            experienceType = experienceType(info.text("enterTpCd"), item.text("career")),
            experienceMinYears = minCareerYears(detail)?.takeIf { it >= 1 },
            educationLevel = educationLevel(info.text("minEdubgIcd")),
            region = subRegion?.region ?: region(item.text("strtnmCd"), item.text("region")),
            subRegion = subRegion,
            recruitmentType = if (hasPeriod) JobRecruitmentType.PERIOD else JobRecruitmentType.ALWAYS_OPEN,
            recruitmentHeadcount = number(info.text("collectPsncnt"))?.takeIf { it in 1..Int.MAX_VALUE }?.toInt(),
            recruitmentStartAt = start,
            recruitmentEndAt = if (hasPeriod) end else null,
            closesWhenFilled = closeTexts.takeIf { it.isNotEmpty() }?.any { it.contains(UNTIL_FILLED) },
            companyAndTeamIntroduction = section(
                "주요 사업" to corp.text("busiCont"),
                "회사 규모" to corp.text("busiSize"),
                "근로자 수" to corp.text("totPsncnt"),
                "회사 주소" to corp.text("corpAddr"),
                "홈페이지" to corp.text("homePg"),
            ),
            responsibilities = info.text("jobCont"),
            qualifications = section(
                "경력" to (info.text("enterTpNm") ?: item.text("career")),
                "학력" to (info.text("eduNm") ?: item.text("minEdubg")),
                "전공" to info.text("major"),
                "자격면허" to info.text("certificate"),
                "외국어" to info.text("forLang"),
                "컴퓨터 활용" to info.text("compAbl"),
            ),
            preferredQualifications = section(
                "우대 조건" to info.text("pfCond"),
                "기타 우대 조건" to info.text("etcPfCond"),
                "병역특례 채용 희망" to info.text("mltsvcExcHope"),
            ),
            compensation = listOfNotNull(item.text("salTpNm"), item.text("sal")).joinToString(" ").ifBlank { null }
                ?: info.text("salTpNm")?.trimEnd(','),
            benefits = section(
                "4대 보험" to info.text("fourIns"),
                "퇴직금" to info.text("retirepay"),
                "기타 복리후생" to info.text("etcWelfare"),
                "장애인 편의시설" to info.text("disableCvntl"),
            ),
            hiringProcess = info.text("selMthd"),
            recruitmentNotice = section(
                "접수 방법" to info.text("rcptMthd"),
                "제출 서류" to info.text("submitDoc"),
                "근무 예정지" to info.text("workRegion"),
                "인근 전철역" to info.text("nearLine"),
                "근무 시간·형태" to info.text("workdayWorkhrCont"),
                "기타 안내" to info.text("etcHopeCont"),
                "채용 부서" to charge.text("empChargerDpt"),
                "문의 전화" to charge.text("contactTelno"),
            ),
            applicationMethod = JobApplicationMethod.EXTERNAL_PAGE,
            sourceUrl = sourceUrl,
            publicationStatus = JobPublicationStatus.PUBLISHED,
            source = ContentSource.WORK24,
            externalId = externalId,
        )
    }

    /** 행정구역 코드가 있으면 코드로, 없으면 지역명으로 시·군·구를 찾는다. 일학습병행 과정도 같은 방법을 쓴다. */
    fun subRegion(administrativeCode: String?, regionName: String?): SubRegion? {
        administrativeCode?.let { return SubRegion.fromAdministrativeCode(it) }
        val (region, subRegion) = regionNames(regionName) ?: return null
        return SubRegion.entries.firstOrNull { it.region == regionOf(region) && it.desc == subRegion }
    }

    fun region(administrativeCode: String?, regionName: String?): Region? {
        administrativeCode?.let { return Region.fromAdministrativeCode(it) }
        return regionNames(regionName)?.first?.let(::regionOf)
    }

    /** 지역명은 `서울 강남구`, `경기도 화성시 동탄구`처럼 시·도와 시·군·구를 공백으로 잇는다. */
    private fun regionNames(regionName: String?): Pair<String, String?>? {
        val words = regionName?.split(' ')?.filter(String::isNotBlank).orEmpty()
        return words.firstOrNull()?.let { it to words.getOrNull(1) }
    }

    /** `경기도`처럼 끝에 `도`를 붙여 오는 이름도 있다. */
    private fun regionOf(name: String): Region? =
        Region.entries.firstOrNull { it.desc == name || it.desc + "도" == name }

    /** 10·20은 기간의 정함이 없는·있는 근로계약, 11·21은 그 시간(선택)제, 4는 파견이다. */
    private fun employmentType(code: String?): JobEmploymentType = when (code) {
        "10" -> JobEmploymentType.FULL_TIME
        "20" -> JobEmploymentType.CONTRACT
        "11", "21" -> JobEmploymentType.PART_TIME
        else -> JobEmploymentType.ETC
    }

    /** 상세의 경력 코드(N 신입, E 경력, Z 관계없음)를 먼저 보고, 없으면 목록의 경력 문구로 판단한다. */
    private fun experienceType(code: String?, career: String?): JobExperienceType = when (code) {
        "N" -> JobExperienceType.NEWCOMER
        "E" -> JobExperienceType.EXPERIENCED
        "Z" -> JobExperienceType.IRRELEVANT
        else -> when {
            career == null || career.contains("관계없음") || career.contains("무관") -> JobExperienceType.IRRELEVANT
            career.contains("신입") && career.contains("경력") -> JobExperienceType.BOTH
            career.contains("경력") -> JobExperienceType.EXPERIENCED
            career.contains("신입") -> JobExperienceType.NEWCOMER
            else -> JobExperienceType.IRRELEVANT
        }
    }

    /**
     * 상세의 경력 문구(`enterTpNm`)에서 최소 경력 연수를 읽는다. `경력 (최소2년) 우대`는 2,
     * `경력 (6개월 이상) 우대`는 0이다. 신입·관계없음처럼 연수가 없으면 null이다.
     */
    fun minCareerYears(detail: JsonNode): Int? {
        val text = detail.path("wantedInfo").text("enterTpNm") ?: return null
        CAREER_YEARS.find(text)?.let { return it.groupValues[1].toIntOrNull() }
        return CAREER_MONTHS.find(text)?.groupValues?.get(1)?.toIntOrNull()?.div(MONTHS_PER_YEAR)
    }

    /** 최소 학력 코드다. 초졸·중졸은 오공고에 없는 단계라 학력 무관으로 본다. */
    private fun educationLevel(code: String?): JobEducationLevel = when (code) {
        "03" -> JobEducationLevel.HIGH_SCHOOL
        "04" -> JobEducationLevel.ASSOCIATE
        "05" -> JobEducationLevel.BACHELOR
        "06" -> JobEducationLevel.MASTER
        "07" -> JobEducationLevel.DOCTORATE
        else -> JobEducationLevel.ANY
    }

    private val CAREER_YEARS = Regex("""(\d+)\s*년""")
    private val CAREER_MONTHS = Regex("""(\d+)\s*개월""")
    private const val MONTHS_PER_YEAR = 12
    private const val UNTIL_FILLED = "채용시까지"
    private const val COMPANY_NAME_MAX = 150
    private const val TITLE_MAX = 255
    private const val CATEGORY_MAX = 100
}
