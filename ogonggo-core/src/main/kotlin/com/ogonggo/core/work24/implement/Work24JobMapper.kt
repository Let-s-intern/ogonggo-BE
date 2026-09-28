package com.ogonggo.core.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.work24.implement.Work24Values.date
import com.ogonggo.core.work24.implement.Work24Values.limit
import com.ogonggo.core.work24.implement.Work24Values.number
import com.ogonggo.core.work24.implement.Work24Values.section
import com.ogonggo.core.work24.implement.Work24Values.text
import java.time.LocalTime

/**
 * 고용24 채용정보 목록 항목(`wanted`)과 상세(`wantedDtl`)를 채용공고로 옮긴다.
 *
 * 크롤러 공고처럼 소유자 없이 곧바로 게시한다. 같은 공고인지는 워크넷 채용정보 URL(`wantedInfoUrl`)을
 * 원문 URL로 보고 판단한다. 직군은 고용24 직종 분류와 오공고 분류가 달라 채우지 않고, 모집 직종을 직무에 넣는다.
 * 코드 값의 뜻은 고용24 개발명세를 따른다.
 */
internal object Work24JobMapper {

    fun sourceUrl(item: JsonNode): String? = item.text("wantedInfoUrl")

    fun detailParameters(item: JsonNode): Map<String, String> =
        mapOf("wantedAuthNo" to requireNotNull(item.text("wantedAuthNo")) { "구인인증번호가 없습니다." })

    fun toAppendDto(item: JsonNode, detail: JsonNode, sourceUrl: String): JobAppendDto {
        val corp = detail.path("corpInfo")
        val info = detail.path("wantedInfo")
        val charge = detail.path("empchargeInfo")

        val closeText = info.text("receiptCloseDt") ?: item.text("closeDt")
        val start = date(item.text("regDt"))?.atStartOfDay()
        val end = date(closeText)?.atTime(LocalTime.of(23, 59, 59))
        val hasPeriod = start != null && end != null && !start.isAfter(end)

        return JobAppendDto(
            companyName = requireNotNull(corp.text("corpNm") ?: item.text("company")) { "회사명이 없습니다." }
                .limit(COMPANY_NAME_MAX),
            title = requireNotNull(info.text("wantedTitle") ?: item.text("title")) { "채용 제목이 없습니다." }
                .limit(TITLE_MAX),
            jobRole = info.text("jobsNm")?.limit(CATEGORY_MAX),
            industry = (corp.text("indTpCdNm") ?: item.text("indTpNm"))?.limit(CATEGORY_MAX),
            employmentType = employmentType(info.text("empTpCd") ?: item.text("empTpCd")),
            experienceType = experienceType(info.text("enterTpCd"), item.text("career")),
            educationLevel = educationLevel(info.text("minEdubgIcd")),
            region = (item.text("region") ?: info.text("workRegion"))?.limit(CATEGORY_MAX),
            recruitmentType = if (hasPeriod) JobRecruitmentType.PERIOD else JobRecruitmentType.ALWAYS_OPEN,
            recruitmentHeadcount = number(info.text("collectPsncnt"))?.takeIf { it in 1..Int.MAX_VALUE }?.toInt(),
            recruitmentStartAt = start,
            recruitmentEndAt = if (hasPeriod) end else null,
            closesWhenFilled = closeText?.contains(UNTIL_FILLED),
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
            compensation = info.text("salTpNm") ?: item.text("sal"),
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
        )
    }

    /** 10·20은 기간의 정함이 없는·있는 근로계약, 11·21은 그 시간(선택)제, 4는 파견이다. */
    private fun employmentType(code: String?): EmploymentType = when (code) {
        "10" -> EmploymentType.FULL_TIME
        "20" -> EmploymentType.CONTRACT
        "11", "21" -> EmploymentType.PART_TIME
        else -> EmploymentType.ETC
    }

    /** 상세의 경력 코드(N 신입, E 경력, Z 관계없음)를 먼저 보고, 없으면 목록의 경력 문구로 판단한다. */
    private fun experienceType(code: String?, career: String?): ExperienceType = when (code) {
        "N" -> ExperienceType.NEWCOMER
        "E" -> ExperienceType.EXPERIENCED
        "Z" -> ExperienceType.IRRELEVANT
        else -> when {
            career == null || career.contains("관계없음") || career.contains("무관") -> ExperienceType.IRRELEVANT
            career.contains("신입") && career.contains("경력") -> ExperienceType.BOTH
            career.contains("경력") -> ExperienceType.EXPERIENCED
            career.contains("신입") -> ExperienceType.NEWCOMER
            else -> ExperienceType.IRRELEVANT
        }
    }

    /** 최소 학력 코드다. 초졸·중졸은 오공고에 없는 단계라 학력 무관으로 본다. */
    private fun educationLevel(code: String?): EducationLevel = when (code) {
        "03" -> EducationLevel.HIGH_SCHOOL
        "04" -> EducationLevel.ASSOCIATE
        "05" -> EducationLevel.BACHELOR
        "06" -> EducationLevel.MASTER
        "07" -> EducationLevel.DOCTORATE
        else -> EducationLevel.ANY
    }

    private const val UNTIL_FILLED = "채용시까지"
    private const val COMPANY_NAME_MAX = 150
    private const val TITLE_MAX = 255
    private const val CATEGORY_MAX = 100
}
