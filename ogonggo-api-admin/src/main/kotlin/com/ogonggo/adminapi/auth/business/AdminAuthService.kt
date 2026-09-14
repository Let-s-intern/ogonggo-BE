package com.ogonggo.adminapi.auth.business

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service

@Service
class AdminAuthService(
    private val userReader: UserReader,
) {

    /**
     * 역할은 토큰에 없으므로 요청마다 현재 역할과 상태를 확인한다.
     * 역할을 회수하거나 계정을 정지하면 토큰이 만료되기 전이라도 다음 요청부터 막힌다.
     */
    fun isActiveAdmin(userId: Long): Boolean {
        val account = try {
            userReader.read(userId)
        } catch (exception: EntityNotFoundException) {
            return false
        }
        return account.role == UserRole.ADMIN && account.status == UserStatus.ACTIVE
    }
}
