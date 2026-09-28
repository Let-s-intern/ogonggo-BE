package com.ogonggo.adminapi.ingestion.work24.implement

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

/** 수집한 항목을 넣을 곳이다. */
enum class Work24Destination {
    JOB,
    BOOTCAMP,
}

/**
 * 매일 받아 오는 고용24 목록이다. 목록에서 새 항목을 찾고 상세 API로 본문을 채워 채용공고·부트캠프로 등록한다.
 * 항목 위치와 식별 필드는 고용24 개발명세의 출력결과를 따른다.
 *
 * - [itemPath]: 최상위 요소를 벗긴 목록 응답에서 항목 요소까지의 경로
 * - [idFields]: 한 번의 수집 안에서 같은 항목을 구별하는 필드. 여러 개면 `-`로 잇는다.
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
) {
    /** 최근 3일 안에 등록된 공고만 받는다. 전체 채용정보는 페이지 상한(1,000쪽)을 넘는다. */
    RECRUITMENTS(
        Work24Api.RECRUITMENTS, Work24Api.RECRUITMENT_DETAIL, Work24Destination.JOB, Work24Paging.START_PAGE,
        listOf("wanted"), listOf("wantedAuthNo"),
        { mapOf("regDate" to "D-3", "sortOrderBy" to "DESC") },
    ),

    /** 오늘부터 90일 안에 시작하는 과정을 받는다. 과정 ID와 회차가 같아야 같은 과정이다. */
    TOMORROW_LEARNING_CARD_COURSES(
        Work24Api.TOMORROW_LEARNING_CARD_COURSES, Work24Api.TOMORROW_LEARNING_CARD_COURSE_DETAIL,
        Work24Destination.BOOTCAMP, Work24Paging.PAGE_NUM,
        listOf("srchList", "scn_list"), listOf("trprId", "trprDegr"), ::trainingCourseParameters,
    ),
    WORK_STUDY_COURSES(
        Work24Api.WORK_STUDY_COURSES, Work24Api.WORK_STUDY_COURSE_DETAIL,
        Work24Destination.BOOTCAMP, Work24Paging.PAGE_NUM,
        listOf("srchList", "scn_list"), listOf("trprId", "trprDegr"), ::trainingCourseParameters,
    ),
}

private const val TRAINING_START_DAYS = 90L
private val DATE: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

/** 훈련시작일 오름차순(sortCol=2)으로 받는다. */
private fun trainingCourseParameters(today: LocalDate): Map<String, String> = mapOf(
    "srchTraStDt" to today.format(DATE),
    "srchTraEndDt" to today.plusDays(TRAINING_START_DAYS).format(DATE),
    "sort" to "ASC",
    "sortCol" to "2",
)
