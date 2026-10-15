package com.ogonggo.core.user.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.implement.dto.CompanyMemberDto
import com.ogonggo.core.user.implement.dto.CompanyMemberPageDto
import com.ogonggo.core.user.implement.dto.GeneralMemberDto
import com.ogonggo.core.user.implement.dto.GeneralMemberPageDto
import com.ogonggo.core.user.persistence.UserQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * 관리자 콘솔의 회원 조회다. 렛츠커리어로 가입한 일반 회원(`USER`·`MENTOR`·`ADMIN`)과 기업 회원(`COMPANY`)을 나눠 본다.
 * 역할 변경도 일반 회원 목록에서 하므로 멘토와 관리자도 일반 회원 목록에 나온다.
 * 탈퇴·정지한 회원도 운영자가 확인해야 하므로 상태와 무관하게 조회한다.
 */
@Component
class UserManagementReader internal constructor(
    private val userQueryRepository: UserQueryRepository,
) {

    fun readGeneralMemberPage(condition: UserManagementSearchCondition, page: Int, size: Int): GeneralMemberPageDto {
        validatePageRequest(page, size)
        val result = userQueryRepository.findGeneralMemberPage(condition, PageRequest.of(page, size))
        return GeneralMemberPageDto(
            members = result.content.map { (user, profile) -> GeneralMemberDto.of(user, profile) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readCompanyMemberPage(condition: UserManagementSearchCondition, page: Int, size: Int): CompanyMemberPageDto {
        validatePageRequest(page, size)
        val result = userQueryRepository.findCompanyMemberPage(condition, PageRequest.of(page, size))
        return CompanyMemberPageDto(
            members = result.content.map { (user, companyProfile) -> CompanyMemberDto.of(user, companyProfile) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
