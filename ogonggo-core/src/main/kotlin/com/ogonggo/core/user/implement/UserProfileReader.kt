package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import org.springframework.stereotype.Component

@Component
class UserProfileReader internal constructor(
    private val userProfileRepository: UserProfileJpaRepository,
) {

    /** 일반 회원의 프로필을 읽는다. 기업 회원에게는 프로필 행이 없으므로 null을 반환한다. */

    fun read(userId: Long): UserProfileDto? =
        userProfileRepository.findByUserId(userId)?.let(UserProfileDto::from)
}
