package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 고용24 목록의 페이지 파라미터와 전체 건수 항목이다. 훈련과정(HRD-Net)과 채용정보의 이름이 다르다. */
enum class Work24Paging(
    val pageParameter: String,
    val sizeParameter: String,
    val totalField: String,
) {
    START_PAGE("startPage", "display", "total"),
    PAGE_NUM("pageNum", "pageSize", "scn_cnt"),
}

/** 수집한 항목을 넣을 곳이다. 일학습병행 훈련과정은 채용정보와 항목이 달라 따로 둔다. */
enum class Work24Destination {
    JOB,
    BOOTCAMP,
    WORK_STUDY_JOB,
}

/**
 * 매일 받아 오는 고용24 목록이다. 목록에서 새 항목을 찾고 상세 API로 본문을 채워 채용공고·부트캠프로 등록한다.
 * 항목 위치와 식별 필드는 고용24 개발명세의 출력결과를 따른다.
 *
 * - [itemPath]: 최상위 요소를 벗긴 목록 응답에서 항목 요소까지의 경로
 * - [idFields]: 같은 항목을 구별하는 필드. 여러 개면 `-`로 잇는다. 등록한 콘텐츠의 외부 식별값(`external_id`)이 된다.
 * - [excludes]: 목록 조건으로 거를 수 없어 목록 항목을 보고 빼는 조건. 해당하면 상세를 부르지 않는다.
 * - [parameters]: 수집일을 받아 고정 검색 조건을 만든다. 날짜 조건은 빠진 날을 메우도록 며칠 겹치게 잡는다.
 */
enum class Work24CollectionTarget(
    val api: Work24Api,
    val detailApi: Work24Api,
    val destination: Work24Destination,
    val paging: Work24Paging,
    val itemPath: List<String>,
    val idFields: List<String>,
    val parameters: (LocalDate) -> Map<String, String>,
    val excludes: (JsonNode) -> Boolean = { false },
) {
    /**
     * 최근 3일 안에 등록된 정규직·계약직 공고만 받는다. 전체 채용정보는 페이지 상한(1,000쪽)을 넘는다.
     *
     * 20대 취업준비생 서비스라 일용직 성격의 공고는 뺀다. 고용형태 코드로 시간제(11·21)·파견(4)·대체인력을 빼고(`empTp=10|20`),
     * 급여가 시급·일급인 공고는 목록에서 보고 뺀다. 급여 형태 검색 조건(`salTp`)은 고용24가 무시해 목록에서 거른다.
     * 채용구분(`empTpGb`)의 일용직(2)은 이 API로 조회되지 않아, 받는 공고는 모두 상용직이다.
     *
     * 직종코드(`jobsCd`)가 [Work24JobRoles] 표에 없는 공고도 뺀다. 오공고 직군·직무로 분류할 수 있는 공고만 받는다.
     */
    RECRUITMENTS(
        Work24Api.RECRUITMENTS, Work24Api.RECRUITMENT_DETAIL, Work24Destination.JOB, Work24Paging.START_PAGE,
        listOf("wanted"), listOf("wantedAuthNo"),
        { mapOf("regDate" to "D-3", "sortOrderBy" to "DESC", "empTp" to "10|20") },
        excludes = { item ->
            item.path("salTpNm").asText().trim() in HOURLY_OR_DAILY_WAGES ||
                Work24JobRoles.of(item.path("jobsCd").asText()) == null
        },
    ),

    /**
     * 국민내일배움카드 훈련과정을 K-디지털 트레이닝 조건(훈련유형 `C0104`)으로 받는다. 오늘부터 90일 안에 시작하는 과정이다.
     * 국민내일배움카드 전체는 90일 안 개강만 11만 건이 넘고 대부분 재직자·원격·단기 자격증 과정이라,
     * 20대 취업준비생을 위한 부트캠프에 맞는 유형만 고른다. 과정 ID와 회차가 같아야 같은 과정이다.
     *
     * 고용24는 이 조건에 K-디지털 트레이닝이 아닌 과정도 함께 준다(2026-09-30 기준 717건 중 국가기간전략산업직종 295건,
     * 과정평가형훈련 44건 등). 그래서 프로그램 유형에는 목록 항목의 훈련유형 이름(`trainTarget`)을 쓴다.
     */
    K_DIGITAL_TRAINING_COURSES(
        Work24Api.TOMORROW_LEARNING_CARD_COURSES, Work24Api.TOMORROW_LEARNING_CARD_COURSE_DETAIL,
        Work24Destination.BOOTCAMP, Work24Paging.PAGE_NUM,
        listOf("srchList", "scn_list"), listOf("trprId", "trprDegr"),
        { today -> trainingCourseParameters(today) + ("crseTracseSe" to K_DIGITAL_TRAINING) },
    ),

    /**
     * 일학습병행 훈련과정 전체를 받는다. 오늘부터 90일 안에 시작하는 과정이다.
     * 학습기업에 채용되어 일하면서 받는 훈련이라 부트캠프가 아니라 고용 형태가 일학습병행인 채용공고로 넣는다.
     */
    WORK_STUDY_COURSES(
        Work24Api.WORK_STUDY_COURSES, Work24Api.WORK_STUDY_COURSE_DETAIL,
        Work24Destination.WORK_STUDY_JOB, Work24Paging.PAGE_NUM,
        listOf("srchList", "scn_list"), listOf("trprId", "trprDegr"), ::trainingCourseParameters,
    ),
}

private const val TRAINING_START_DAYS = 90L
private const val K_DIGITAL_TRAINING = "C0104"
private val HOURLY_OR_DAILY_WAGES = setOf("시급", "일급")
private val DATE: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

/** 훈련시작일 오름차순(sortCol=2)으로 받는다. */
private fun trainingCourseParameters(today: LocalDate): Map<String, String> = mapOf(
    "srchTraStDt" to today.format(DATE),
    "srchTraEndDt" to today.plusDays(TRAINING_START_DAYS).format(DATE),
    "sort" to "ASC",
    "sortCol" to "2",
)
