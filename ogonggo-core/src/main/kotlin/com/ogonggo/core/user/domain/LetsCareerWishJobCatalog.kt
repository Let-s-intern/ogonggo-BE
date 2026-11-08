package com.ogonggo.core.user.domain

import com.ogonggo.core.job.domain.JobRole

/**
 * 렛츠커리어 커리어 정보 화면의 희망 직군·직무 선택지다. 회원 프로필의 희망 직무(`user_profiles.wish_job`)는
 * 이 목록의 직무 이름을 `", "`로 이어 저장한 값이다. 렛츠커리어가 소유하는 목록이라 오공고는 해석만 한다.
 *
 * 원본: 렛츠커리어 웹(`lets-intern-client`)의 `packages/utils/src/constants.ts` `JOB_FIELD_ROLES`.
 * 오공고 웹 `apps/web/src/features/sign-up/lib/careerOptions.ts`도 같은 목록이다. 목록이 바뀌면 세 곳을 함께 맞춘다.
 *
 * 직군마다 첫 항목은 `<직군> 직무 전체`이며 그 직군의 모든 직무를 뜻한다. `직무 무관`은 어떤 직무도 뜻하지 않는다.
 */
object LetsCareerWishJobCatalog {
    const val ANY_JOB_ROLE = "직무 무관"
    private const val ALL_ROLES_SUFFIX = " 직무 전체"

    /** 직군 이름 → 그 직군의 직무 이름(`직무 전체` 항목 제외). */
    val rolesByField: Map<String, List<String>> = linkedMapOf(
        "경영/인사" to listOf("경영지원", "회계/경리", "조직관리", "정보보호 담당자", "인사/평가", "교육", "채용담당자", "서비스운영", "CS 매니저"),
        "마케팅" to listOf(
            "마케터", "퍼포먼스 마케터", "콘텐츠 마케터", "디지털 마케터", "마케팅 전략 기획", "브랜드 마케터",
            "광고 기획(AE)", "CRM 전문가", "카피라이터/UX Writer", "마켓 리서처",
        ),
        "영업" to listOf("기업 영업", "기술 영업", "해외영업", "솔루션 컨설턴트", "세일즈", "제약 영업"),
        "기획" to listOf("서비스 기획", "PM/PO", "비즈니스 분석가", "사업개발/기획", "전략 기획", "해외 사업개발/기획", "상품 기획/MD"),
        "디자인" to listOf(
            "프로덕트 디자인", "웹/앱 디자인", "그래픽 디자인", "UX 디자인", "BI/BX 디자인", "광고 디자인",
            "영상/모션 디자인", "운영 디자인", "3D 디자인",
        ),
        "개발" to listOf(
            "백엔드/서버 개발", "프론트엔드 개발", "SW 엔지니어", "안드로이드 개발", "iOS 개발", "데이터 엔지니어",
            "데이터 사이언티스트", "데이터 분석가", "QA/테스트 엔지니어", "보안 엔지니어", "임베디드", "게임 개발자",
        ),
        "엔지니어링" to listOf("기계", "전자", "전기", "로봇", "설비", "공정"),
    )

    /** 직군 전체를 뜻하는 선택지 이름. 예: `마케팅 직무 전체`. */
    fun allRolesOf(field: String): String = "$field$ALL_ROLES_SUFFIX"

    /** 관리자가 고를 수 있는 선택지인지. 직군 전체 항목과 개별 직무만 허용하고 `직무 무관`은 받지 않는다. */
    fun isSelectable(label: String): Boolean =
        rolesByField.any { (field, roles) -> label == allRolesOf(field) || label in roles }

    /** 저장된 희망 직무 문자열을 선택지 목록으로 나눈다. 렛츠커리어와 같이 `,`로 자르고 앞뒤 공백을 지운다. */
    fun split(wishJob: String?): List<String> =
        wishJob?.split(",")?.map(String::trim)?.filter(String::isNotEmpty)?.distinct().orEmpty()

    /**
     * 회원의 희망 직무와 관리자가 고른 직무가 하나라도 겹치는지 본다.
     * 양쪽 모두 `<직군> 직무 전체`를 그 직군의 모든 직무로 펼쳐 비교한다. `직무 무관`과 목록에 없는 값은 아무 직무와도 겹치지 않는다.
     */
    fun matches(wishJob: String?, selected: Collection<String>): Boolean {
        val wanted = expand(selected)
        return wanted.isNotEmpty() && expand(split(wishJob)).any { it in wanted }
    }

    /**
     * 회원의 희망 직무를 오공고 공고 직무([JobRole])로 옮긴다. `<직군> 직무 전체`는 그 직군의 직무를 모두 옮긴 것이다.
     * `직무 무관`, 목록에 없는 값, 오공고에 맞는 직무가 없는 값(예: `그래픽 디자인`)은 아무 직무도 되지 않는다.
     */
    fun jobRolesOf(wishJob: String?): Set<JobRole> =
        expand(split(wishJob)).flatMapTo(mutableSetOf()) { jobRolesByWishJob[it].orEmpty() }

    private fun expand(labels: Collection<String>): Set<String> = labels.flatMapTo(mutableSetOf()) { label ->
        rolesByField.entries.firstOrNull { allRolesOf(it.key) == label }?.value
            ?: listOfNotNull(label.takeIf { rolesByField.values.any { roles -> label in roles } })
    }

    /**
     * 렛츠커리어 직무 → 오공고 공고 직무. 두 분류는 따로 만들어져 이름이 거의 겹치지 않아 뜻으로 맞췄다(LC-3580, 2026-11-06).
     * [rolesByField]의 모든 직무가 키로 있어야 한다. 맞는 오공고 직무가 없으면 빈 집합이다.
     */
    val jobRolesByWishJob: Map<String, Set<JobRole>> = mapOf(
        "경영지원" to setOf(JobRole.PLANNING_MANAGEMENT_SUPPORT, JobRole.HR_GENERAL_AFFAIRS_SECRETARY),
        "회계/경리" to setOf(
            JobRole.ACCOUNTING_ACCOUNTING, JobRole.ACCOUNTING_FINANCE, JobRole.ACCOUNTING_TAX, JobRole.ACCOUNTING_ETC,
        ),
        "조직관리" to setOf(JobRole.HR_PLANNING, JobRole.HR_HRD_CULTURE),
        "정보보호 담당자" to setOf(JobRole.IT_SECURITY),
        "인사/평가" to setOf(JobRole.HR_PLANNING, JobRole.HR_COMPENSATION, JobRole.HR_LABOR_RELATIONS, JobRole.HR_ETC),
        "교육" to setOf(JobRole.HR_HRD_CULTURE, JobRole.EDUCATION_INSTRUCTIONAL_DESIGN),
        "채용담당자" to setOf(JobRole.HR_PLANNING),
        "서비스운영" to setOf(JobRole.PLANNING_OPERATIONS_MANAGEMENT),
        "CS 매니저" to setOf(JobRole.CS_CS, JobRole.CS_CX_MANAGER),

        "마케터" to setOf(JobRole.MARKETING_STRATEGY, JobRole.MARKETING_ETC),
        "퍼포먼스 마케터" to setOf(JobRole.MARKETING_PERFORMANCE),
        "콘텐츠 마케터" to setOf(JobRole.MARKETING_CONTENT, JobRole.MARKETING_SNS, JobRole.MEDIA_CONTENT_PLANNING_EDITOR),
        "디지털 마케터" to setOf(JobRole.MARKETING_PERFORMANCE, JobRole.MARKETING_SNS),
        "마케팅 전략 기획" to setOf(JobRole.MARKETING_STRATEGY),
        "브랜드 마케터" to setOf(JobRole.MARKETING_BRAND),
        "광고 기획(AE)" to setOf(JobRole.MARKETING_AD_PLANNING_AE),
        "CRM 전문가" to setOf(JobRole.MARKETING_CRM),
        "카피라이터/UX Writer" to setOf(JobRole.MEDIA_CONTENT_PLANNING_EDITOR),
        "마켓 리서처" to setOf(JobRole.DESIGN_RESEARCH, JobRole.MARKETING_STRATEGY),

        "기업 영업" to setOf(JobRole.SALES_B2B),
        "기술 영업" to setOf(JobRole.SALES_TECH_IT),
        "해외영업" to setOf(JobRole.SALES_OVERSEAS, JobRole.TRADE_OVERSEAS_TRADING_SALES),
        "솔루션 컨설턴트" to setOf(JobRole.SALES_TECH_IT, JobRole.PLANNING_CONSULTING),
        "세일즈" to setOf(
            JobRole.SALES_B2C, JobRole.SALES_B2B, JobRole.SALES_GENERAL, JobRole.SALES_MANAGEMENT_SUPPORT, JobRole.SALES_ETC,
        ),
        "제약 영업" to setOf(JobRole.SALES_PHARMA_MEDICAL),

        "서비스 기획" to setOf(JobRole.PLANNING_SERVICE_PRODUCT_PLANNING),
        "PM/PO" to setOf(JobRole.PLANNING_PM_PO, JobRole.PLANNING_PROJECT_MANAGER),
        "비즈니스 분석가" to setOf(JobRole.PLANNING_BUSINESS_DEVELOPMENT),
        "사업개발/기획" to setOf(JobRole.PLANNING_BUSINESS_DEVELOPMENT, JobRole.PLANNING_BUSINESS_STRATEGY),
        "전략 기획" to setOf(JobRole.PLANNING_BUSINESS_STRATEGY),
        "해외 사업개발/기획" to setOf(JobRole.PLANNING_BUSINESS_DEVELOPMENT),
        "상품 기획/MD" to setOf(
            JobRole.MD_PRODUCT_PLANNING, JobRole.MD_ONLINE, JobRole.MD_FOOD, JobRole.MD_FASHION, JobRole.MD_BEAUTY,
            JobRole.MD_SALES, JobRole.MD_RETAIL, JobRole.MD_ETC,
        ),

        "프로덕트 디자인" to setOf(JobRole.DESIGN_UX_UI_PRODUCT),
        "웹/앱 디자인" to setOf(JobRole.DESIGN_UX_UI_PRODUCT),
        "그래픽 디자인" to emptySet(),
        "UX 디자인" to setOf(JobRole.DESIGN_UX_UI_PRODUCT, JobRole.DESIGN_RESEARCH),
        "BI/BX 디자인" to emptySet(),
        "광고 디자인" to emptySet(),
        "영상/모션 디자인" to setOf(JobRole.MEDIA_VIDEO_EDITOR),
        "운영 디자인" to emptySet(),
        "3D 디자인" to setOf(JobRole.GAME_MODELING_3D),

        "백엔드/서버 개발" to setOf(JobRole.IT_BACKEND),
        "프론트엔드 개발" to setOf(JobRole.IT_FRONTEND_FULLSTACK, JobRole.IT_WEB),
        "SW 엔지니어" to setOf(JobRole.IT_BACKEND, JobRole.IT_FRONTEND_FULLSTACK, JobRole.IT_WEB, JobRole.IT_ETC),
        "안드로이드 개발" to setOf(JobRole.IT_ANDROID),
        "iOS 개발" to setOf(JobRole.IT_IOS),
        "데이터 엔지니어" to setOf(JobRole.AI_DATA_ENGINEER),
        "데이터 사이언티스트" to setOf(JobRole.AI_DATA_ANALYST_SCIENTIST, JobRole.AI_ML_MLOPS),
        "데이터 분석가" to setOf(JobRole.AI_DATA_ANALYST_SCIENTIST),
        "QA/테스트 엔지니어" to setOf(JobRole.IT_ETC),
        "보안 엔지니어" to setOf(JobRole.IT_SECURITY),
        "임베디드" to setOf(JobRole.IT_EMBEDDED_IOT_ROBOTICS),
        "게임 개발자" to setOf(JobRole.GAME_CLIENT, JobRole.GAME_SERVER, JobRole.GAME_MOBILE),

        "기계" to setOf(JobRole.ENGINEERING_MECHANICAL, JobRole.ENGINEERING_MECHANICAL_DESIGN_CAD),
        "전자" to setOf(JobRole.ENGINEERING_ELECTRICAL_ELECTRONIC_CONTROL, JobRole.ENGINEERING_SEMICONDUCTOR_DISPLAY),
        "전기" to setOf(JobRole.ENGINEERING_ELECTRICAL_ELECTRONIC_CONTROL),
        "로봇" to setOf(JobRole.IT_EMBEDDED_IOT_ROBOTICS, JobRole.ENGINEERING_ELECTRICAL_ELECTRONIC_CONTROL),
        "설비" to setOf(JobRole.PRODUCTION_EQUIPMENT, JobRole.PRODUCTION_FACILITY_MANAGEMENT),
        "공정" to setOf(JobRole.PRODUCTION_PRODUCTION, JobRole.PRODUCTION_QUALITY),
    )
}
