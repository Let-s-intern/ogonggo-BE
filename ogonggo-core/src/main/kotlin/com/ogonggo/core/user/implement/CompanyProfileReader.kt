package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import org.springframework.stereotype.Component

interface CompanyProfileReader {
    /** 기업 회원의 기업 정보를 읽는다. 일반 회원에게는 행이 없으므로 null을 반환한다. */
    fun read(userId: Long): CompanyProfileData?
}

@Component
internal class CompanyProfileReaderImpl(
    private val companyProfileRepository: CompanyProfileJpaRepository,
) : CompanyProfileReader {

    override fun read(userId: Long): CompanyProfileData? =
        companyProfileRepository.findByUserId(userId)?.let(CompanyProfileData::from)
}

data class CompanyProfileData(
    val organizationName: String,
    val managerName: String,
) {
    companion object {
        internal fun from(profile: CompanyProfile): CompanyProfileData = CompanyProfileData(
            organizationName = profile.organizationName,
            managerName = profile.managerName,
        )
    }
}
