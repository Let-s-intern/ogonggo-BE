package com.ogonggo.core.user.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

interface CompanyProfileAppender {
    fun append(command: CompanyProfileAppendCommand)
}

@Component
internal class CompanyProfileAppenderImpl(
    private val companyProfileRepository: CompanyProfileJpaRepository,
) : CompanyProfileAppender {

    override fun append(command: CompanyProfileAppendCommand) {
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
