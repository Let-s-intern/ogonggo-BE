package com.ogonggo.core.user.implement

import com.ogonggo.core.user.implement.dto.CompanyProfileUpdateDto
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyProfileManager internal constructor(
    private val companyProfileRepository: CompanyProfileJpaRepository,
) {

    /**
     * 기업 정보는 가입과 같은 트랜잭션에서 만들어지므로 기업 회원이면 행이 반드시 있다.
     * 호출 전에 기업 회원인지 확인하므로 행이 없으면 데이터가 어긋난 것이다.
     */
    fun replace(userId: Long, command: CompanyProfileUpdateDto) {
        val profile = checkNotNull(companyProfileRepository.findByUserId(userId)) {
            "기업 회원의 기업 정보가 없습니다. userId=$userId"
        }

        profile.replace(
            organizationName = command.organizationName,
            managerName = command.managerName,
        )
        companyProfileRepository.save(profile)
    }
}
