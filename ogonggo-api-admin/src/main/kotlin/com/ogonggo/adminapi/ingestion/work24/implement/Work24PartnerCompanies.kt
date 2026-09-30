package com.ogonggo.adminapi.ingestion.work24.implement

/**
 * 고용24 훈련과정명 앞의 괄호에서 연계 기업을 찾아 부트캠프 파트너사 이름으로 돌려준다.
 *
 * 고용24에는 기업 연계 과정인지 알려 주는 항목이 없다. 목록·상세 API, 공통코드(훈련종류·훈련기관 구분),
 * K-디지털 훈련 검색 화면의 세부 유형을 확인했지만 구분할 수 없었다.
 * 기업과 함께 운영하는 과정은 `[kt cloud] 생성형AI 과정`, `[LG전자] LG전자 AX School`처럼 과정명을 기업 이름 괄호로 시작하는 관례가 있다.
 * 다만 `[취업기업확대]`, `(스마트웹&콘텐츠개발)`처럼 직종이나 과정 성격을 적은 괄호가 더 많아, 괄호 안 글자를 그대로 쓰지 않고 아래 표에 있는 이름만 인정한다.
 *
 * 표는 2026-09-30 목록(717건)의 괄호 68개 중 기업·브랜드 이름으로 고른 것이다. 새 기업이 생기면 표에 추가해야 잡힌다.
 * 괄호 없이 기업이 직접 운영하는 과정(훈련기관이 기업인 경우)은 찾지 못한다.
 */
internal object Work24PartnerCompanies {

    private val LEADING_BRACKET = Regex("""^\s*[\[(（【]\s*([^\])）】]+)""")
    private val BLANK = Regex("""\s+""")

    /** 괄호 안 표기를 공백 없이 소문자로 맞춘 값과 파트너사 이름이다. 과정 브랜드 이름은 운영 기업 이름으로 바꾼다. */
    private val PARTNERS: Map<String, String> = listOf(
        "kt cloud" to "kt cloud",
        "SAP" to "SAP",
        "두산로보틱스" to "두산로보틱스",
        "엔비디아" to "엔비디아",
        "미디어 프론티어" to "미디어 프론티어",
        "포스코" to "포스코",
        "LG전자" to "LG전자",
        "MBC 미디어 캠퍼스" to "MBC",
        "MBC" to "MBC",
        "MBC+" to "MBC플러스",
        "이스트캠프" to "이스트소프트",
        "메가존클라우드" to "메가존클라우드",
        "엔코아" to "엔코아",
        "솔트룩스" to "솔트룩스",
        "KBS미디어" to "KBS미디어",
        "IBM" to "IBM",
        "KG그룹" to "KG그룹",
        "KG ICT" to "KG ICT",
        "현대건설" to "현대건설",
        "SK플래닛" to "SK플래닛",
        "Microsoft" to "Microsoft",
        "더존비즈온" to "더존비즈온",
        "코오롱 베니트" to "코오롱베니트",
        "한글과컴퓨터" to "한글과컴퓨터",
        "대보정보통신" to "대보정보통신",
        "에스트래픽" to "에스트래픽",
    ).associate { (label, partner) -> label.key() to partner }

    fun of(courseTitle: String?): String? =
        courseTitle?.let(LEADING_BRACKET::find)?.groupValues?.get(1)?.let { PARTNERS[it.key()] }

    private fun String.key(): String = replace(BLANK, "").lowercase()
}
