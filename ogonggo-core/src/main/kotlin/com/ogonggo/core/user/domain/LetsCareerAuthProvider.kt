package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 렛츠커리어 계정의 가입 경로다. 렛츠커리어의 `AuthProvider`와 이름과 code를 맞춰 두었다.
 * 이메일(SERVICE)로 가입한 계정만 비밀번호가 있다.
 */
enum class LetsCareerAuthProvider(
    override val code: Int,
    override val desc: String,
) : EnumField {
    KAKAO(1, "카카오톡"),
    NAVER(2, "네이버"),
    GOOGLE(3, "구글"),
    SERVICE(4, "이메일"),
    ;

    /** 소셜 가입은 비밀번호가 없다. 값을 모르는 과거 계정은 막지 않고 렛츠커리어 판정에 맡긴다. */
    val hasPassword: Boolean get() = this == SERVICE
}
