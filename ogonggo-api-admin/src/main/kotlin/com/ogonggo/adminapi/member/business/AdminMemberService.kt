package com.ogonggo.adminapi.member.business

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserManagementReader
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.dto.CompanyMemberPageDto
import com.ogonggo.core.user.implement.dto.GeneralMemberPageDto
import org.springframework.stereotype.Service

@Service
class AdminMemberService(
    private val userManagementReader: UserManagementReader,
    private val userManager: UserManager,
) {

    fun getGeneralMembers(condition: UserManagementSearchCondition, page: Int, size: Int): GeneralMemberPageDto =
        userManagementReader.readGeneralMemberPage(condition, page, size)

    fun getCompanyMembers(condition: UserManagementSearchCondition, page: Int, size: Int): CompanyMemberPageDto =
        userManagementReader.readCompanyMemberPage(condition, page, size)

    /**
     * 관리자가 일반 회원을 멘토·관리자로 올리거나 내린다.
     * 자기 역할은 바꿀 수 없다. 실수로 자기를 내리면 바로 콘솔에서 막히고, 관리자가 한 명뿐이면 DB를 직접 고쳐야 한다.
     */
    fun changeGeneralMemberRole(adminUserId: Long, userId: Long, role: UserRole) {
        if (adminUserId == userId) {
            throw InvalidValueException(UserErrorCode.OWN_ROLE_UNCHANGEABLE)
        }
        userManager.changeRole(userId, role)
    }
}
