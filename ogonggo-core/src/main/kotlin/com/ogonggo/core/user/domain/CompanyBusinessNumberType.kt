package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 기업 회원이 가입할 때 낸 번호의 종류다. 해외 기업은 국내 번호가 없어 번호 없이 가입한다.
 * 번호는 숫자만 저장하며 [digitCount]가 그 자릿수다. 진위는 확인하지 않고 형식만 검사한다.
 */
enum class CompanyBusinessNumberType(
    override val code: Int,
    override val desc: String,
    val digitCount: Int?,
) : EnumField {
    BUSINESS_REGISTRATION(1, "사업자등록번호", 10),
    CORPORATE_REGISTRATION(2, "법인등록번호", 13),
    OVERSEAS(3, "해외 기업", null),
    ;

    /** 이 종류에 맞는 번호인지 본다. 번호가 있는 종류는 자릿수만큼의 숫자, 해외 기업은 번호가 없어야 한다. */
    fun accepts(businessNumber: String?): Boolean {
        val expectedDigits = digitCount ?: return businessNumber == null
        return businessNumber != null &&
            businessNumber.length == expectedDigits &&
            businessNumber.all { it in '0'..'9' }
    }
}
