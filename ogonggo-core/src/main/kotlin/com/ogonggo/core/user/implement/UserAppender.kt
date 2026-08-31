package com.ogonggo.core.user.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

interface UserAppender {
    fun append(command: UserAppendCommand): UserAccount
    fun appendCompany(command: CompanyAccountAppendCommand): UserAccount
}

@Component
internal class UserAppenderImpl(
    private val userRepository: UserJpaRepository,
) : UserAppender {

    /**
     * 같은 렛츠커리어 사용자의 최초 로그인이 동시에 들어오면 letscareer_user_id 유니크 제약이 걸린다.
     * 제약 위반 이후에는 트랜잭션이 롤백 대상이 되어 같은 트랜잭션에서 재조회할 수 없으므로,
     * 재시도 가능한 409로 변환해 클라이언트가 다시 요청하도록 한다.
     */
    override fun append(command: UserAppendCommand): UserAccount =
        try {
            userRepository.saveAndFlush(
                User.ofLetsCareer(
                    letsCareerUserId = command.letsCareerUserId,
                    joinedAt = command.joinedAt,
                ),
            ).toAccount()
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(UserErrorCode.USER_ALREADY_EXISTS)
        }

    /**
     * 이메일 중복은 사전 조회 대신 유니크 제약으로 판정한다.
     * 조회와 저장 사이에 같은 이메일이 들어오는 경쟁 상태를 조회로는 막을 수 없기 때문이다.
     */
    override fun appendCompany(command: CompanyAccountAppendCommand): UserAccount =
        try {
            userRepository.saveAndFlush(
                User.ofCompany(
                    email = command.email,
                    encodedPassword = command.encodedPassword,
                    joinedAt = command.joinedAt,
                ),
            ).toAccount()
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(UserErrorCode.EMAIL_ALREADY_EXISTS)
        }
}
