package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 렛츠커리어 콘텐츠가 다루는 취업 준비 단계다. 공고가 요구하는 제출물·전형과 맞춰 추천에 쓴다.
 * 크롤러가 AI로 콘텐츠마다 붙인다.
 */
enum class LetsCareerContentTopic(
    override val code: Int,
    override val desc: String,
) : EnumField {
    CAREER_START(1, "취업 준비 시작"),
    RESUME(2, "이력서"),
    PERSONAL_STATEMENT(3, "자기소개서"),
    PORTFOLIO(4, "포트폴리오"),
    INTERVIEW(5, "면접"),
    WRITTEN_TEST(6, "인적성·필기"),
    EXPERIENCE_SUMMARY(7, "경험 정리"),
    INDUSTRY_COMPANY_ANALYSIS(8, "산업·기업 분석"),
    LARGE_COMPANY(9, "대기업"),
    INTERNSHIP(10, "인턴"),
}
