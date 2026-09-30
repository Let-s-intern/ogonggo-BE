package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.implement.dto.CompanyBasicInfoUpdateDto
import com.ogonggo.core.user.implement.dto.CompanyManagerInfoUpdateDto
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyProfileManager internal constructor(
    private val companyProfileRepository: CompanyProfileJpaRepository,
) {

    /**
     * 기본 정보(기관명·로고)를 교체하고 그 전에 쓰던 로고 이미지 식별자를 돌려준다.
     * 이전 로고 이미지를 정리하는 것은 호출한 쪽이 한다.
     */
    fun replaceBasicInfo(userId: Long, command: CompanyBasicInfoUpdateDto): String? {
        val profile = read(userId)
        val previousLogoImageId = profile.logoImageId

        profile.replaceBasicInfo(
            organizationName = command.organizationName,
            logoImageId = command.logo?.imageId,
            logoUrl = command.logo?.url,
        )
        companyProfileRepository.save(profile)
        return previousLogoImageId
    }

    fun replaceManagerInfo(userId: Long, command: CompanyManagerInfoUpdateDto) {
        val profile = read(userId)

        profile.replaceManagerInfo(
            managerName = command.managerName,
            managerPhone = command.managerPhone,
            notificationEmail = command.notificationEmail,
        )
        companyProfileRepository.save(profile)
    }

    /**
     * 기업 정보는 가입과 같은 트랜잭션에서 만들어지므로 기업 회원이면 행이 반드시 있다.
     * 호출 전에 기업 회원인지 확인하므로 행이 없으면 데이터가 어긋난 것이다.
     */
    private fun read(userId: Long): CompanyProfile = checkNotNull(companyProfileRepository.findByUserId(userId)) {
        "기업 회원의 기업 정보가 없습니다. userId=$userId"
    }
}
