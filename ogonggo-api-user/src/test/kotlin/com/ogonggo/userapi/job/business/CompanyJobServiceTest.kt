package com.ogonggo.userapi.job.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserAccount
import com.ogonggo.core.user.implement.UserReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CompanyJobServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobAppender = Mockito.mock(JobAppender::class.java)
    private val jobManager = Mockito.mock(JobManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)
    private val service = CompanyJobService(userReader, jobReader, jobAppender, jobManager, clock)

    @Test
    fun `등록한 공고에 소유자를 채운다`() {
        givenAccount(role = UserRole.COMPANY)
        val saved = Mockito.mock(Job::class.java)
        Mockito.`when`(saved.id).thenReturn(JOB_ID)
        val expected = command().copy(ownerUserId = USER_ID)
        Mockito.`when`(jobAppender.append(expected)).thenReturn(saved)

        val id = service.create(USER_ID, command())

        assertEquals(JOB_ID, id)
        Mockito.verify(jobAppender).append(expected)
    }

    @Test
    fun `기업 회원이 아니면 등록을 막는다`() {
        givenAccount(role = UserRole.USER)

        val exception = assertThrows(ForbiddenException::class.java) { service.create(USER_ID, command()) }

        assertEquals(UserErrorCode.COMPANY_ROLE_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(jobAppender)
    }

    @Test
    fun `정지되거나 탈퇴한 계정은 사용할 수 없다`() {
        givenAccount(role = UserRole.COMPANY, status = UserStatus.SUSPENDED)
        assertEquals(
            UserErrorCode.USER_SUSPENDED,
            assertThrows(ForbiddenException::class.java) { service.getJobs(USER_ID, 0, 10) }.errorCode,
        )

        givenAccount(role = UserRole.COMPANY, status = UserStatus.WITHDRAWN)
        assertEquals(
            UserErrorCode.USER_WITHDRAWN,
            assertThrows(ForbiddenException::class.java) { service.getJobs(USER_ID, 0, 10) }.errorCode,
        )
    }

    @Test
    fun `게시와 마감은 소유한 공고를 잠그고 처리한다`() {
        givenAccount(role = UserRole.COMPANY)
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readOwnedForUpdate(USER_ID, JOB_ID)).thenReturn(job)

        service.publish(USER_ID, JOB_ID)
        service.close(USER_ID, JOB_ID)

        Mockito.verify(jobManager).publish(job)
        Mockito.verify(jobManager).close(job, NOW)
    }

    @Test
    fun `삭제는 이미 삭제된 공고도 찾는 조회를 사용한다`() {
        givenAccount(role = UserRole.COMPANY)
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readOwnedForDelete(USER_ID, JOB_ID)).thenReturn(job)

        service.delete(USER_ID, JOB_ID)

        Mockito.verify(jobManager).delete(job, NOW)
    }

    private fun givenAccount(role: UserRole, status: UserStatus = UserStatus.ACTIVE) {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(
            UserAccount(
                userId = USER_ID,
                letsCareerUserId = null,
                email = "company@example.com",
                status = status,
                role = role,
            ),
        )
    }

    private fun command(): JobAppendCommand = JobAppendCommand(
        companyName = "오공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        recruitmentType = JobRecruitmentType.PERIOD,
    )

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
        private const val USER_ID = 17L
        private const val JOB_ID = 3L
    }
}
