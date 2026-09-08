package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 사용자의 학년이다. 렛츠커리어의 같은 이름 enum과 값과 code를 맞춰 두어 최초 가입 시 그대로 옮길 수 있다.
 * 렛츠커리어는 code를 컬럼에 저장하지만 오공고는 다른 업무 enum과 같이 이름을 저장한다.
 */
enum class UserGrade(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FIRST(1, "1학년"),
    SECOND(2, "2학년"),
    THIRD(3, "3학년"),
    FOURTH(4, "4학년"),
    ETC(5, "5학년 이상"),
    GRADUATE(6, "졸업생"),
}
