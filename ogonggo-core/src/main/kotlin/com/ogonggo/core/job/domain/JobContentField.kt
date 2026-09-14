package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 운영자가 고칠 수 있는 채용공고 본문 칸이다.
 * 목록 밖의 칸은 고칠 수 없어, 요청이 아무 키나 실어 식별자나 지표를 덮어쓰는 일을 막는다.
 * `fieldName`은 엔티티의 속성 이름이며 화면이 본문을 고칠 때 되돌려 보내는 키다.
 */
enum class JobContentField(
    override val code: Int,
    override val desc: String,
    val fieldName: String,
) : EnumField {
    COMPANY_AND_TEAM_INTRODUCTION(1, "회사 및 팀 소개", "companyAndTeamIntroduction"),
    RESPONSIBILITIES(2, "주요 업무", "responsibilities"),
    QUALIFICATIONS(3, "자격 요건", "qualifications"),
    PREFERRED_QUALIFICATIONS(4, "우대 사항", "preferredQualifications"),
    COMPENSATION(5, "급여 및 처우", "compensation"),
    BENEFITS(6, "복지 및 혜택", "benefits"),
    HIRING_PROCESS(7, "채용 절차", "hiringProcess"),
    ;

    companion object {
        fun fromFieldName(fieldName: String): JobContentField? = entries.firstOrNull { it.fieldName == fieldName }
    }
}
