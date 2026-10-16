package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField
import java.time.Duration
import java.time.LocalDateTime

/**
 * 일반 회원이 오공고에 어떻게 들어왔는지다. 한 번 정하면 다시 계산하지 않는다.
 * 렛츠커리어가 계정의 첫 로그인 서비스를 알려 주면 그 값으로 정한다.
 * 첫 로그인 서비스를 기록하기 전에 만든 렛츠커리어 계정만 가입 일시 차이로 가른다.
 */
enum class UserInflowSource(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LETSCAREER(1, "렛츠커리어 기존 회원"),
    OGONGGO(2, "오공고 신규 유입"),
    ;

    companion object {
        /**
         * 첫 로그인 서비스를 모를 때만 쓴다.
         * 오공고에서 로그인하려다 렛츠커리어 계정이 없어 가입하면, 가입 직후 바로 오공고로 돌아와 계정이 만들어진다.
         * 소셜 가입은 몇 초, 이메일 가입도 몇 분이면 끝나므로 여유를 두어 1시간 안이면 오공고로 처음 들어온 회원으로 본다.
         */
        val OGONGGO_SIGN_UP_WINDOW: Duration = Duration.ofHours(1)

        fun classify(letsCareerJoinedAt: LocalDateTime, ogonggoJoinedAt: LocalDateTime): UserInflowSource =
            if (letsCareerJoinedAt.isBefore(ogonggoJoinedAt.minus(OGONGGO_SIGN_UP_WINDOW))) LETSCAREER else OGONGGO
    }
}
