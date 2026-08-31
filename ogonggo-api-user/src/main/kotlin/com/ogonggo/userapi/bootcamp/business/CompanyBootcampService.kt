package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.implement.BootcampAppendCommand
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.BootcampUpdateCommand
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class CompanyBootcampService(
    private val userReader: UserReader,
    private val bootcampReader: BootcampReader,
    private val bootcampContentReader: BootcampContentReader,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val clock: Clock,
) {

    @Transactional
    fun create(userId: Long, command: BootcampAppendCommand): Long {
        verifyCompany(userId)
        val closedAt = if (command.status == BootcampStatus.CLOSED) LocalDateTime.now(clock) else null
        val bootcamp = bootcampAppender.append(
            command.copy(ownerUserId = userId, closedAt = closedAt),
        )
        return bootcamp.requiredId()
    }

    fun getBootcamps(userId: Long, page: Int, size: Int): CompanyBootcampPageResult {
        verifyCompany(userId)
        return CompanyBootcampPageResult.from(bootcampReader.readOwnedPage(userId, page, size))
    }

    fun getBootcamp(userId: Long, bootcampId: Long): CompanyBootcampResult {
        verifyCompany(userId)
        val bootcamp = bootcampReader.readOwned(userId, bootcampId)
        return bootcamp.toResult()
    }

    @Transactional
    fun update(userId: Long, bootcampId: Long, command: BootcampUpdateCommand) {
        verifyCompany(userId)
        bootcampManager.update(bootcampReader.readOwnedForUpdate(userId, bootcampId), command)
    }

    @Transactional
    fun startRecruitment(userId: Long, bootcampId: Long) {
        verifyCompany(userId)
        bootcampManager.startRecruitment(bootcampReader.readOwnedForUpdate(userId, bootcampId))
    }

    @Transactional
    fun close(userId: Long, bootcampId: Long) {
        verifyCompany(userId)
        bootcampManager.close(
            bootcampReader.readOwnedForUpdate(userId, bootcampId),
            LocalDateTime.now(clock),
        )
    }

    @Transactional
    fun delete(userId: Long, bootcampId: Long) {
        verifyCompany(userId)
        bootcampManager.delete(
            bootcampReader.readOwnedForDelete(userId, bootcampId),
            LocalDateTime.now(clock),
        )
    }

    private fun verifyCompany(userId: Long) {
        val account = userReader.read(userId)
        when (account.status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
        if (account.role != UserRole.COMPANY) {
            throw ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED)
        }
    }

    private fun Bootcamp.toResult(): CompanyBootcampResult {
        val bootcampId = requiredId()
        return CompanyBootcampResult.from(
            bootcamp = this,
            partners = bootcampContentReader.readPartners(bootcampId),
            curriculums = bootcampContentReader.readCurriculums(bootcampId),
        )
    }
}
