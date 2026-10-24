package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import org.springframework.stereotype.Component

@Component
class UserFcmTokenReader internal constructor(private val tokenRepository: UserFcmTokenJpaRepository) {
    fun readLatest(userId: Long): String? =
        tokenRepository.findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(userId)?.token

    fun readActive(userId: Long): List<UserFcmTokenDto> =
        tokenRepository.findAllByUserIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(userId)
            .map { UserFcmTokenDto(checkNotNull(it.id), checkNotNull(it.token)) }

    /** 발송 시점의 회원 상태와 토큰 소유권을 함께 확인한다. */
    fun isDeliverable(userId: Long, token: String): Boolean =
        tokenRepository.isDeliverable(userId, UserFcmToken.hash(token))
}
