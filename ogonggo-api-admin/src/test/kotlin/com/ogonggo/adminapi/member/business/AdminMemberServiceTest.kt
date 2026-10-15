package com.ogonggo.adminapi.member.business

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserManagementReader
import com.ogonggo.core.user.implement.UserManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AdminMemberServiceTest {

    private val userManager = Mockito.mock(UserManager::class.java)
    private val service = AdminMemberService(Mockito.mock(UserManagementReader::class.java), userManager)

    @Test
    fun `관리자는 다른 일반 회원의 역할을 바꾼다`() {
        service.changeGeneralMemberRole(adminUserId = 1L, userId = 7L, role = UserRole.MENTOR)

        Mockito.verify(userManager).changeRole(7L, UserRole.MENTOR)
    }

    @Test
    fun `관리자는 자기 역할을 바꿀 수 없다`() {
        val error = assertThrows(InvalidValueException::class.java) {
            service.changeGeneralMemberRole(adminUserId = 1L, userId = 1L, role = UserRole.USER)
        }

        assertEquals(UserErrorCode.OWN_ROLE_UNCHANGEABLE, error.errorCode)
        Mockito.verifyNoInteractions(userManager)
    }
}
