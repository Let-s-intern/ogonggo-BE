package com.ogonggo.adminapi.member.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Component

@Component
class CompanyMemberValidator(
    private val userReader: UserReader,
) {

    /**
     * 운영자가 공고를 대신 등록할 비즈니스 회원인지 본다. 회원 상세 조회처럼 없거나 비즈니스 회원이 아니면 404다.
     * 정지·탈퇴한 회원은 자기 공고를 관리할 수 없으므로 대신 등록하지 않는다.
     */
    fun validateProxyRegistrable(userId: Long) {
        val account = userReader.read(userId)
        if (account.role != UserRole.COMPANY) {
            throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)
        }
        if (account.status != UserStatus.ACTIVE) {
            throw ConflictException(UserErrorCode.COMPANY_MEMBER_NOT_ACTIVE)
        }
    }
}
