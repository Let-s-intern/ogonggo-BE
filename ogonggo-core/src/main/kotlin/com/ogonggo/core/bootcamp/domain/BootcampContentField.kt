package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 운영자가 고칠 수 있는 부트캠프 본문 칸이다.
 * 커리큘럼과 파트너사는 구조가 있는 값이라 본문 수정으로 고치지 않는다.
 * `fieldName`은 엔티티의 속성 이름이며 화면이 본문을 고칠 때 되돌려 보내는 키다.
 */
enum class BootcampContentField(
    override val code: Int,
    override val desc: String,
    val fieldName: String,
    /** 비울 수 없는 칸이다. 상세 내용은 부트캠프의 필수값이다. */
    val required: Boolean,
) : EnumField {
    CONTENT(1, "상세 내용", "content", required = true),
    ELIGIBILITY_AND_SELECTION_PROCESS(2, "지원 자격 및 전형 안내", "eligibilityAndSelectionProcess", required = false),
    ;

    companion object {
        fun fromFieldName(fieldName: String): BootcampContentField? = entries.firstOrNull { it.fieldName == fieldName }
    }
}
