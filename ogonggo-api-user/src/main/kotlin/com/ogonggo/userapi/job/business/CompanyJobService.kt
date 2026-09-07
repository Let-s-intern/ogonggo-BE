package com.ogonggo.userapi.job.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobUpdateCommand
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 기업회원이 자기 채용공고를 등록하고 관리한다.
 *
 * 등록과 게시를 나눈다. 등록은 항상 임시저장으로 끝내고 게시는 별도 요청으로 처리한다.
 * 게시 조건이 지금은 기업회원 자격뿐이지만 앞으로 결제 확인이 추가되므로,
 * 그 판단을 publish 한 곳에 모아 두면 요청 계약을 바꾸지 않고 조건만 늘릴 수 있다.
 */
@Service
class CompanyJobService(
    private val userReader: UserReader,
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val clock: Clock,
) {

    @Transactional
    fun create(userId: Long, command: JobAppendCommand): Long {
        verifyCompany(userId)
        return jobAppender.append(command.copy(ownerUserId = userId)).requiredId()
    }

    fun getJobs(userId: Long, page: Int, size: Int): CompanyJobPageResult {
        verifyCompany(userId)
        return CompanyJobPageResult.from(jobReader.readOwnedPage(userId, page, size))
    }

    fun getJob(userId: Long, jobId: Long): CompanyJobResult {
        verifyCompany(userId)
        return CompanyJobResult.from(jobReader.readOwned(userId, jobId))
    }

    @Transactional
    fun update(userId: Long, jobId: Long, command: JobUpdateCommand) {
        verifyCompany(userId)
        jobManager.update(jobReader.readOwnedForUpdate(userId, jobId), command)
    }

    /** 게시 조건을 여기 모은다. 결제가 도입되면 확인 절차가 이 자리에 들어간다. */
    @Transactional
    fun publish(userId: Long, jobId: Long) {
        verifyCompany(userId)
        jobManager.publish(jobReader.readOwnedForUpdate(userId, jobId))
    }

    @Transactional
    fun close(userId: Long, jobId: Long) {
        verifyCompany(userId)
        jobManager.close(jobReader.readOwnedForUpdate(userId, jobId), LocalDateTime.now(clock))
    }

    @Transactional
    fun delete(userId: Long, jobId: Long) {
        verifyCompany(userId)
        jobManager.delete(jobReader.readOwnedForDelete(userId, jobId), LocalDateTime.now(clock))
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
}
