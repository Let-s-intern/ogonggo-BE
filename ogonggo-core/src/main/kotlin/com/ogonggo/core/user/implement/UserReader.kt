package com.ogonggo.core.user.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

interface UserReader {
    fun readByLetsCareerUserId(letsCareerUserId: Long): UserAccount?
    fun read(userId: Long): UserAccount

    /** 기업 회원 로그인용 조회. 자격증명이 없는 일반 회원 계정은 반환하지 않는다. */
    fun readCredentialByEmail(email: String): UserCredential?
}

@Component
internal class UserReaderImpl(
    private val userRepository: UserJpaRepository,
) : UserReader {

    override fun readByLetsCareerUserId(letsCareerUserId: Long): UserAccount? =
        userRepository.findByLetsCareerUserId(letsCareerUserId)?.toAccount()

    override fun read(userId: Long): UserAccount =
        userRepository.findByIdOrNull(userId)?.toAccount()
            ?: throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)

    override fun readCredentialByEmail(email: String): UserCredential? {
        val user = userRepository.findByEmail(email) ?: return null
        val encodedPassword = user.password ?: return null

        return UserCredential(
            userId = user.requiredId(),
            encodedPassword = encodedPassword,
            status = user.status,
            role = user.role,
        )
    }
}

internal fun User.toAccount(): UserAccount = UserAccount(
    userId = requiredId(),
    letsCareerUserId = letsCareerUserId,
    email = email,
    status = status,
    role = role,
)

private fun User.requiredId(): Long = checkNotNull(id) { "사용자 식별자가 없습니다." }
