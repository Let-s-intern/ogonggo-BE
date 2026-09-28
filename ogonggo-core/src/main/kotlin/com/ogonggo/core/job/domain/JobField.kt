package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 채용공고의 직군이다. 값과 순서는 기획의 직군·직무 분류(job-taxonomy)를 따른다.
 * 직군 아래의 직무는 [JobRole]이다.
 */
enum class JobField(
    override val code: Int,
    override val desc: String,
) : EnumField {
    IT_DEVELOPMENT(1, "IT·개발"),
    AI_DATA(2, "AI·데이터"),
    GAME(3, "게임"),
    DESIGN(4, "디자인"),
    PLANNING_STRATEGY(5, "기획·전략"),
    MARKETING_ADVERTISING(6, "마케팅·광고"),
    MERCHANDISING(7, "상품기획·MD"),
    SALES(8, "영업"),
    TRADE_LOGISTICS(9, "무역·물류"),
    TRANSPORT_DELIVERY(10, "운송·배송"),
    LEGAL(11, "법률·법무"),
    HR_GENERAL_AFFAIRS(12, "HR·총무"),
    ACCOUNTING_TAX_FINANCE(13, "회계·세무·재무"),
    SECURITIES_ASSET_MANAGEMENT(14, "증권·운용"),
    BANKING_CARD_INSURANCE(15, "은행·카드·보험"),
    ENGINEERING_RND(16, "엔지니어링·R&D"),
    CONSTRUCTION_ARCHITECTURE(17, "건설·건축"),
    PRODUCTION_SKILLED_TRADES(18, "생산·기능직"),
    MEDICAL_HEALTH(19, "의료·보건"),
    PUBLIC_WELFARE(20, "공공·복지"),
    EDUCATION(21, "교육"),
    MEDIA_ENTERTAINMENT(22, "미디어·엔터"),
    CUSTOMER_SERVICE_TM(23, "고객상담·TM"),
    SERVICE(24, "서비스"),
    FOOD_BEVERAGE(25, "식음료"),
}
