package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 부트캠프 목록에서 고를 수 있는 분류다. 부트캠프에 저장하는 값이 아니라 조회할 때 등록 경로와 프로그램 유형으로 가른다.
 *
 * - [KDT]: 고용24에서 수집한 과정 중 프로그램 유형이 K-디지털 트레이닝인 것. 같은 조건으로 함께 수집되는
 *   국가기간전략산업직종 등 다른 훈련유형은 들지 않는다.
 * - [SESAC]: 크롤러가 등록한 부트캠프. 지금 크롤러가 수집하는 곳이 새싹(청년취업사관학교)뿐이라 등록 경로로 가른다.
 *   크롤러가 다른 곳도 수집하게 되면 기준을 다시 정해야 한다.
 */
enum class BootcampCategory(
    override val code: Int,
    override val desc: String,
) : EnumField {
    KDT(1, "KDT"),
    SESAC(2, "새싹"),
    ;

    companion object {
        /** 고용24 목록의 훈련유형 이름이며 수집할 때 프로그램 유형에 그대로 들어간다. */
        const val KDT_PROGRAM_TYPE = "K-디지털 트레이닝"
    }
}
