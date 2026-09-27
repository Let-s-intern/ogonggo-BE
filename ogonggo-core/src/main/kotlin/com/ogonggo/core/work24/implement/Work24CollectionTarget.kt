package com.ogonggo.core.work24.implement

import com.ogonggo.core.work24.domain.Work24Api
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 고용24 목록의 페이지 파라미터와 전체 건수 항목이다. 훈련과정(HRD-Net)과 나머지의 이름이 다르다. */
enum class Work24Paging(
    val pageParameter: String,
    val sizeParameter: String,
    val totalField: String,
) {
    START_PAGE("startPage", "display", "total"),
    PAGE_NUM("pageNum", "pageSize", "scn_cnt"),
}

/**
 * 매일 받아 오는 고용24 목록이다. 항목 위치와 식별 필드는 고용24 개발명세의 출력결과를 따른다.
 *
 * 검색어·사업ID가 필수인 API(직업사전, 직무정보, 정부지원일자리 기관·통계)와 상세 API는
 * 조건 없이 전체를 받을 수 없어 대상이 아니다.
 *
 * - [itemPath]: 최상위 요소를 벗긴 응답에서 항목 요소까지의 경로
 * - [idFields]: 항목을 구별하는 필드. 여러 개면 `-`로 잇는다. 비어 있으면 항목 내용 전체의 해시를 쓴다.
 * - [parameters]: 수집일을 받아 고정 검색 조건을 만든다. 날짜 조건은 누락된 날을 메우도록 며칠 겹치게 잡는다.
 */
enum class Work24CollectionTarget(
    val api: Work24Api,
    val paging: Work24Paging,
    val itemPath: List<String>,
    val idFields: List<String>,
    val parameters: (LocalDate) -> Map<String, String> = { emptyMap() },
) {
    /** 최근 3일 안에 등록된 공고만 받는다. 전체 채용정보는 페이지 상한(1,000쪽)을 넘는다. */
    RECRUITMENTS(
        Work24Api.RECRUITMENTS, Work24Paging.START_PAGE, listOf("wanted"), listOf("wantedAuthNo"),
        { mapOf("callTp" to "L", "regDate" to "D-3", "sortOrderBy" to "DESC") },
    ),

    /** 오늘부터 90일 안에 시작하는 과정을 받는다. 과정 ID와 회차가 같아야 같은 과정이다. */
    TOMORROW_LEARNING_CARD_COURSES(
        Work24Api.TOMORROW_LEARNING_CARD_COURSES, Work24Paging.PAGE_NUM, listOf("srchList", "scn_list"),
        listOf("trprId", "trprDegr"), ::trainingCourseParameters,
    ),
    WORK_STUDY_COURSES(
        Work24Api.WORK_STUDY_COURSES, Work24Paging.PAGE_NUM, listOf("srchList", "scn_list"),
        listOf("trprId", "trprDegr"), ::trainingCourseParameters,
    ),

    /** 최근 3일 안에 등록된 모집공고를 받는다. 날짜 형식은 명세에 없어 훈련과정과 같은 yyyyMMdd로 보낸다. */
    GOVERNMENT_JOB_RECRUITMENTS(
        Work24Api.GOVERNMENT_JOB_RECRUITMENTS, Work24Paging.START_PAGE, listOf("ilmoaJob"), listOf("rcritPblancId"),
        { today -> mapOf("srchBgnDt" to today.minusDays(RECENT_DAYS).format(DATE), "srchEndDt" to today.format(DATE)) },
    ),
    GOVERNMENT_JOB_PROGRAMS(
        Work24Api.GOVERNMENT_JOB_PROGRAMS, Work24Paging.START_PAGE, listOf("ilmoaJob"), listOf("bsnsId"),
    ),

    /** 프로그램에 식별 필드가 없어 항목 내용으로 구별한다. */
    JOB_SEEKER_PROGRAMS(
        Work24Api.JOB_SEEKER_PROGRAMS, Work24Paging.START_PAGE, listOf("empPgmSchdInvite"), emptyList(),
    ),

    OCCUPATIONS(
        Work24Api.OCCUPATIONS, Work24Paging.START_PAGE, listOf("jobList"), listOf("jobCd"),
    ),

    /** 같은 기업이 해마다 다시 선정되므로 선정연도까지 봐야 한다. */
    SMALL_GIANT_COMPANIES(
        Work24Api.SMALL_GIANT_COMPANIES, Work24Paging.START_PAGE, listOf("smallGiant"), listOf("busiNo", "selYear"),
    ),
    SMALL_GIANT_COMPANY_VISITS(
        Work24Api.SMALL_GIANT_COMPANY_VISITS, Work24Paging.START_PAGE, listOf("company"),
        listOf("busiNo", "collectDtm"), { mapOf("callTp" to "L") },
    ),

    /** sregDtmValCd=6은 등록일 전체다. */
    YOUTH_SMALL_GIANT_COMPANY_EXPERIENCES(
        Work24Api.YOUTH_SMALL_GIANT_COMPANY_EXPERIENCES, Work24Paging.START_PAGE, listOf("traOrg"),
        listOf("wantedAuthNo"), { mapOf("callTp" to "L", "sregDtmValCd" to "6") },
    ),
    YOUTH_FRIENDLY_SMALL_GIANT_COMPANIES(
        Work24Api.YOUTH_FRIENDLY_SMALL_GIANT_COMPANIES, Work24Paging.START_PAGE, listOf("smallGiant"),
        listOf("busiNo"),
    ),
}

private const val RECENT_DAYS = 3L
private const val TRAINING_START_DAYS = 90L
private val DATE: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

/** 훈련시작일 오름차순(sortCol=2)으로 받는다. */
private fun trainingCourseParameters(today: LocalDate): Map<String, String> = mapOf(
    "srchTraStDt" to today.format(DATE),
    "srchTraEndDt" to today.plusDays(TRAINING_START_DAYS).format(DATE),
    "sort" to "ASC",
    "sortCol" to "2",
)
