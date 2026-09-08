package com.ogonggo.core.user.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.CompanyProfileAppendDto
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class CompanyProfileAppender internal constructor(
    private val companyProfileRepository: CompanyProfileJpaRepository,
) {

    fun append(command: CompanyProfileAppendDto) {
        try {
            companyProfileRepository.saveAndFlush(
                CompanyProfile(
                    userId = command.userId,
                    organizationName = command.organizationName,
                    managerName = command.managerName,
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(UserErrorCode.COMPANY_PROFILE_ALREADY_EXISTS)
        }
    }
}
