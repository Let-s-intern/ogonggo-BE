package com.ogonggo.adminapi.member.business

import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampPageDto
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostPageDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserManagementReader
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.dto.CompanyMemberDto
import com.ogonggo.core.user.implement.dto.GeneralMemberDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class AdminMemberServiceTest {

    private val userManagementReader = Mockito.mock(UserManagementReader::class.java)
    private val userManager = Mockito.mock(UserManager::class.java)
    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobBookmarkReader = Mockito.mock(JobBookmarkReader::class.java)
    private val bootcampBookmarkReader = Mockito.mock(BootcampBookmarkReader::class.java)
    private val recruitmentPostReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val service = AdminMemberService(
        userManagementReader = userManagementReader,
        userManager = userManager,
        jobReader = jobReader,
        jobMetricReader = Mockito.mock(JobMetricReader::class.java),
        jobBookmarkReader = jobBookmarkReader,
        bootcampBookmarkReader = bootcampBookmarkReader,
        recruitmentPostReader = recruitmentPostReader,
        recruitmentPostApplicationReader = Mockito.mock(RecruitmentPostApplicationReader::class.java),
        recruitmentPostMetricReader = Mockito.mock(RecruitmentPostMetricReader::class.java),
    )

    @Test
    fun `관리자는 다른 일반 회원의 역할을 바꾼다`() {
        service.changeGeneralMemberRole(adminUserId = 1L, userId = 7L, role = UserRole.MENTOR)

        Mockito.verify(userManager).changeRole(7L, UserRole.MENTOR)
    }

    @Test
    fun `관리자는 자기 역할을 바꿀 수 없다`() {
        val error = assertThrows(InvalidValueException::class.java) {
            service.changeGeneralMemberRole(adminUserId = 1L, userId = 1L, role = UserRole.USER)
        }

        assertEquals(UserErrorCode.OWN_ROLE_UNCHANGEABLE, error.errorCode)
        Mockito.verifyNoInteractions(userManager)
    }

    @Test
    fun `일반 회원 상세는 그 회원의 북마크와 모집글을 각각 최근 50건까지만 읽는다`() {
        // given
        Mockito.`when`(userManagementReader.readGeneralMember(7L)).thenReturn(GENERAL_MEMBER)
        Mockito.`when`(jobBookmarkReader.readBookmarkedPublishedPage(7L, JobSearchCondition.NONE, 0, 50))
            .thenReturn(JobPageDto(emptyList(), page = 0, size = 50, totalElements = 0, totalPages = 0, hasNext = false))
        Mockito.`when`(bootcampBookmarkReader.readBookmarkedPublicPage(7L, BootcampSearchCondition.NONE, 0, 50))
            .thenReturn(BootcampPageDto(emptyList(), page = 0, size = 50, totalElements = 0, totalPages = 0, hasNext = false))
        Mockito.`when`(
            recruitmentPostReader.readConsolePage(
                RecruitmentPostConsoleSearchCondition(authorUserId = 7L),
                RecruitmentPostSortType.LATEST,
                0,
                50,
            ),
        ).thenReturn(RecruitmentPostPageDto(emptyList(), page = 0, size = 50, totalElements = 0, totalPages = 0))

        // when
        val result = service.getGeneralMember(7L)

        // then
        assertEquals(GENERAL_MEMBER, result.member)
        assertTrue(result.bookmarkedJobs.isEmpty())
        assertTrue(result.bookmarkedBootcamps.isEmpty())
        assertTrue(result.authoredSideStudies.isEmpty())
    }

    @Test
    fun `기업 회원 상세는 그 회원이 등록한 공고를 최근 50건까지만 읽는다`() {
        // given
        Mockito.`when`(userManagementReader.readCompanyMember(7L)).thenReturn(COMPANY_MEMBER)
        Mockito.`when`(jobReader.readOwnedPage(7L, 0, 50))
            .thenReturn(JobPageDto(emptyList(), page = 0, size = 50, totalElements = 0, totalPages = 0, hasNext = false))

        // when
        val result = service.getCompanyMember(7L)

        // then
        assertEquals(COMPANY_MEMBER, result.member)
        assertTrue(result.jobs.isEmpty())
    }

    private companion object {
        val JOINED_AT: LocalDateTime = LocalDateTime.of(2026, 9, 28, 10, 0)
        val GENERAL_MEMBER = GeneralMemberDto(
            userId = 7L,
            letsCareerUserId = 4821L,
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            joinedAt = JOINED_AT,
            withdrawnAt = null,
            profile = null,
        )
        val COMPANY_MEMBER = CompanyMemberDto(
            userId = 7L,
            email = "company@example.com",
            status = UserStatus.ACTIVE,
            joinedAt = JOINED_AT,
            withdrawnAt = null,
            companyProfile = null,
        )
    }
}
