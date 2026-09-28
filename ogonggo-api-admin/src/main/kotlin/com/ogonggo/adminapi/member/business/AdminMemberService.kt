package com.ogonggo.adminapi.member.business

import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.implement.UserManagementReader
import com.ogonggo.core.user.implement.dto.CompanyMemberPageDto
import com.ogonggo.core.user.implement.dto.GeneralMemberPageDto
import org.springframework.stereotype.Service

@Service
class AdminMemberService(
    private val userManagementReader: UserManagementReader,
) {

    fun getGeneralMembers(condition: UserManagementSearchCondition, page: Int, size: Int): GeneralMemberPageDto =
        userManagementReader.readGeneralMemberPage(condition, page, size)

    fun getCompanyMembers(condition: UserManagementSearchCondition, page: Int, size: Int): CompanyMemberPageDto =
        userManagementReader.readCompanyMemberPage(condition, page, size)
}
