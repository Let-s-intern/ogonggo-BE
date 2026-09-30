package com.ogonggo.adminapi.ingestion.work24.implement.dto

import com.ogonggo.adminapi.ingestion.work24.implement.Work24CollectionTarget
import java.time.LocalDate

/**
 * 수집 대상 하나를 받아 온 결과다.
 * 건너뛴 수는 이미 등록된 항목, 제외한 수는 수집 조건에 맞지 않아 상세를 부르지 않은 항목(시급·일급, 분류할 수 없는 직종의 공고),
 * 실패한 수는 값이 모자라거나 상세 조회가 실패한 항목이다.
 */
data class Work24CollectDto(
    val target: Work24CollectionTarget,
    val pageCount: Int,
    val appendedCount: Int,
    val skippedCount: Int,
    val excludedCount: Int,
    val failedCount: Int,
)

/**
 * 고용24 훈련과정 상세 화면에서 읽은 값이다. Open API가 주지 않는 항목만 담는다.
 *
 * - [overview]: 훈련과정 개요 표의 제목과 글. 과정마다 제목 구성이 다르다(훈련목표, 훈련대상자요건, 훈련대상 요건 선수학습 등).
 * - [subjects]: 교과편성의 교과목. NCS를 적용한 과정은 세부내용이 없어 비어 있다.
 * - [lessons]: 시간표의 수업 시간 한 칸씩이다. 주차별 커리큘럼을 만드는 데 쓴다.
 */
data class Work24CoursePageDto(
    val overview: Map<String, String> = emptyMap(),
    val instructors: List<Instructor> = emptyList(),
    val textbooks: List<String> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
) {
    /** 화면이 이름을 `민*식`처럼 가려 보여 주며, 가려진 그대로 담는다. */
    data class Instructor(val name: String, val major: String?, val qualifications: String?)

    data class Subject(val name: String, val detail: String?, val hours: String?)

    data class Lesson(val date: LocalDate, val subject: String)
}

/**
 * 고용24 일학습병행 훈련과정 상세 화면에서 읽은 값이다. Open API가 주지 않는 항목만 담는다.
 *
 * 화면은 도제식 현장 교육훈련(OJT)과 사업장 외 교육훈련(Off-JT)을 따로 보여 준다.
 * 2026-09-30에 본 과정 6개는 모두 Off-JT 쪽 훈련내용과 편성이 비어 있고 훈련시간만 있어, 훈련내용과 편성은 OJT 것만 담는다.
 */
data class Work24WorkStudyPageDto(
    val company: String? = null,
    val purpose: String? = null,
    val mainContent: String? = null,
    val requirement: String? = null,
    val ojtHours: String? = null,
    val offJtHours: String? = null,
    val ojtSubjects: List<Subject> = emptyList(),
) {
    data class Subject(val name: String, val unit: String?, val required: String?, val hours: String?)
}

/**
 * 고용24 훈련기관 소개 화면의 이미지를 오공고 저장소로 옮긴 뒤의 주소다.
 * [photos]는 훈련기관 사진이며 화면 순서대로다. 옮기지 못한 이미지는 빠진다.
 */
data class Work24InstitutionImagesDto(
    val logoUrl: String? = null,
    val photos: List<Photo> = emptyList(),
) {
    /** [caption]은 훈련기관이 붙인 설명이다(강의실, 안내데스크 등). */
    data class Photo(val url: String, val caption: String?)
}
