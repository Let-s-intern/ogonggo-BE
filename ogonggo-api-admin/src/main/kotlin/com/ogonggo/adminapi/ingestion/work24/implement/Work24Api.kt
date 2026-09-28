package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.core.enumeration.EnumField

/**
 * 고용24에 사용 신청한 서비스다. 인증키가 서비스마다 따로 발급되므로 키를 고르는 단위가 된다.
 */
enum class Work24Service(
    override val code: Int,
    override val desc: String,
) : EnumField {
    RECRUITMENT(1, "채용정보"),
    TOMORROW_LEARNING_CARD(2, "국민내일배움카드 훈련과정"),
    WORK_STUDY(3, "일학습병행 훈련과정"),
    GOVERNMENT_JOB(4, "정부지원일자리정보"),
    JOB_SEEKER_PROGRAM(5, "구직자취업역량 강화프로그램"),
    OCCUPATION(6, "직업정보"),
    DUTY(7, "직무정보"),
    SMALL_GIANT_COMPANY(8, "강소기업"),
}

/** 고용24 응답 형식이다. 대부분의 API는 XML만 지원한다. */
enum class Work24ReturnType {
    XML,
    JSON,
}

/**
 * 오공고가 호출하는 고용24 Open API 목록이다.
 *
 * 요청 파라미터는 API마다 달라 호출자가 넘긴다. 인증키와 응답 형식, 그리고 명세가
 * "반드시 지정"하라고 정해 둔 값은 여기서 고정해 호출자가 틀리게 보낼 수 없게 한다.
 * 파라미터와 응답 항목은 고용24 Open API 개발명세(서비스 소개 및 신청 메뉴)를 따른다.
 */
enum class Work24Api(
    override val code: Int,
    override val desc: String,
    val service: Work24Service,
    val path: String,
    val returnType: Work24ReturnType = Work24ReturnType.XML,
    val fixedParameters: Map<String, String> = emptyMap(),
) : EnumField {
    RECRUITMENTS(
        1, "채용정보 목록", Work24Service.RECRUITMENT, "/wk/callOpenApiSvcInfo210L01.do",
        fixedParameters = mapOf(CALL_TYPE to "L"),
    ),

    /** 워크넷 인증 공고(infoSvc=VALIDATION)만 조회한다. */
    RECRUITMENT_DETAIL(
        24, "채용정보 상세", Work24Service.RECRUITMENT, "/wk/callOpenApiSvcInfo210D01.do",
        fixedParameters = mapOf(CALL_TYPE to "D", "infoSvc" to "VALIDATION"),
    ),

    TOMORROW_LEARNING_CARD_COURSES(
        2, "국민내일배움카드 훈련과정 목록", Work24Service.TOMORROW_LEARNING_CARD,
        "/hr/callOpenApiSvcInfo310L01.do", fixedParameters = mapOf(OUT_TYPE to OUT_TYPE_LIST),
    ),
    TOMORROW_LEARNING_CARD_COURSE_DETAIL(
        3, "국민내일배움카드 훈련과정 과정·기관정보", Work24Service.TOMORROW_LEARNING_CARD,
        "/hr/callOpenApiSvcInfo310L02.do", fixedParameters = mapOf(OUT_TYPE to OUT_TYPE_DETAIL),
    ),
    TOMORROW_LEARNING_CARD_COURSE_SCHEDULES(
        4, "국민내일배움카드 훈련과정 훈련일정", Work24Service.TOMORROW_LEARNING_CARD,
        "/hr/callOpenApiSvcInfo310L03.do", fixedParameters = mapOf(OUT_TYPE to OUT_TYPE_DETAIL),
    ),

    WORK_STUDY_COURSES(
        5, "일학습병행 훈련과정 목록", Work24Service.WORK_STUDY,
        "/hr/callOpenApiSvcInfo313L01.do", fixedParameters = mapOf(OUT_TYPE to OUT_TYPE_LIST),
    ),
    WORK_STUDY_COURSE_DETAIL(
        6, "일학습병행 훈련과정 과정·기관정보", Work24Service.WORK_STUDY,
        "/hr/callOpenApiSvcInfo313D01.do", fixedParameters = mapOf(OUT_TYPE to OUT_TYPE_DETAIL),
    ),
    WORK_STUDY_COURSE_SCHEDULES(
        7, "일학습병행 훈련과정 훈련일정", Work24Service.WORK_STUDY,
        "/hr/callOpenApiSvcInfo313D02.do", fixedParameters = mapOf(OUT_TYPE to OUT_TYPE_DETAIL),
    ),

    GOVERNMENT_JOB_RECRUITMENTS(
        8, "정부지원일자리 참여자모집정보", Work24Service.GOVERNMENT_JOB, "/wk/callOpenApiSvcInfo211L01.do",
    ),
    GOVERNMENT_JOB_RECRUITMENT_DETAIL(
        9, "정부지원일자리 참여자모집상세정보", Work24Service.GOVERNMENT_JOB, "/wk/callOpenApiSvcInfo211D01.do",
    ),
    GOVERNMENT_JOB_PROGRAMS(
        10, "정부지원일자리 일자리사업정보", Work24Service.GOVERNMENT_JOB, "/wk/callOpenApiSvcInfo211L02.do",
    ),
    GOVERNMENT_JOB_PROGRAM_DETAIL(
        11, "정부지원일자리 일자리사업상세정보", Work24Service.GOVERNMENT_JOB, "/wk/callOpenApiSvcInfo211D02.do",
    ),
    GOVERNMENT_JOB_INSTITUTIONS(
        12, "정부지원일자리 기관기본정보", Work24Service.GOVERNMENT_JOB, "/wk/callOpenApiSvcInfo211L05.do",
    ),
    GOVERNMENT_JOB_PARTICIPANT_STATISTICS(
        13, "정부지원일자리 참여자통계", Work24Service.GOVERNMENT_JOB, "/wk/callOpenApiSvcInfo211L08.do",
    ),

    JOB_SEEKER_PROGRAMS(
        14, "구직자취업역량 강화프로그램", Work24Service.JOB_SEEKER_PROGRAM, "/wk/callOpenApiSvcInfo217L01.do",
    ),

    OCCUPATIONS(
        15, "직업정보 목록", Work24Service.OCCUPATION, "/wk/callOpenApiSvcInfo212L01.do",
        fixedParameters = mapOf(TARGET to "JOBCD"),
    ),
    OCCUPATION_DETAIL(
        16, "직업정보 상세", Work24Service.OCCUPATION, "/wk/callOpenApiSvcInfo212D01.do",
        fixedParameters = mapOf(TARGET to "JOBDTL", "jobGb" to "1"),
    ),
    OCCUPATION_DICTIONARY(
        17, "직업사전", Work24Service.OCCUPATION, "/wk/callOpenApiSvcInfo212L50.do",
        fixedParameters = mapOf(TARGET to "dJobCD"),
    ),

    STANDARD_JOB_DESCRIPTIONS(
        18, "표준직무기술서", Work24Service.DUTY, "/wk/callOpenApiSvcInfo215L01.do", Work24ReturnType.JSON,
    ),
    DUTY_DATA_DICTIONARY(
        19, "직무데이터사전", Work24Service.DUTY, "/wk/callOpenApiSvcInfo215L11.do", Work24ReturnType.JSON,
    ),

    SMALL_GIANT_COMPANIES(
        20, "강소기업", Work24Service.SMALL_GIANT_COMPANY, "/wk/callOpenApiSvcInfo216L01.do",
    ),
    SMALL_GIANT_COMPANY_VISITS(
        21, "강소기업 현장탐방기", Work24Service.SMALL_GIANT_COMPANY, "/wk/callOpenApiSvcInfo216L11.do",
    ),
    YOUTH_SMALL_GIANT_COMPANY_EXPERIENCES(
        22, "청년강소기업체험", Work24Service.SMALL_GIANT_COMPANY, "/wk/callOpenApiSvcInfo216L21.do",
    ),
    YOUTH_FRIENDLY_SMALL_GIANT_COMPANIES(
        23, "청년친화강소기업", Work24Service.SMALL_GIANT_COMPANY, "/wk/callOpenApiSvcInfo216L31.do",
    ),
    ;

    /** 호출자가 보낸 값보다 우선하는 파라미터다. 인증키는 여기에 싣지 않고 호출 직전에 붙인다. */
    fun reservedParameters(): Map<String, String> =
        fixedParameters + (RETURN_TYPE to returnType.name)

    companion object {
        const val AUTH_KEY = "authKey"
        const val RETURN_TYPE = "returnType"
    }
}

private const val OUT_TYPE = "outType"
private const val OUT_TYPE_LIST = "1"
private const val OUT_TYPE_DETAIL = "2"
private const val TARGET = "target"
private const val CALL_TYPE = "callTp"
