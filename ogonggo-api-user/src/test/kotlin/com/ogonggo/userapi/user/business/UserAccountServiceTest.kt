package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.CompanyProfileData
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.UserAccount
import com.ogonggo.core.user.implement.UserProfileData
import com.ogonggo.core.user.implement.UserProfileJobInfoCommand
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class UserAccountServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val companyProfileReader = Mockito.mock(CompanyProfileReader::class.java)
    private val userProfileManager = Mockito.mock(UserProfileManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = UserAccountService(
        userReader,
        userProfileReader,
        companyProfileReader,
        userProfileManager,
        clock,
    )

    @Test
    fun `일반 회원은 렛츠커리어 프로필만 담고 기업 정보는 읽지 않는다`() {
        givenAccount(UserRole.USER, email = null)
        Mockito.`when`(userProfileReader.read(USER_ID)).thenReturn(
            UserProfileData(
                name = "김렛츠",
                email = "lets@career.co.kr",
                nickname = "렛츠",
                profileImageUrl = "https://example.com/me.png",
                university = "오공고대학교",
                major = "컴퓨터공학과",
                grade = UserGrade.GRADUATE,
                wishField = "개발",
                wishJob = null,
                wishIndustry = null,
                wishEmploymentType = null,
                wishCompany = null,
            ),
        )

        val result = service.getMyAccount(USER_ID)

        assertEquals(UserRole.USER, result.role)
        assertEquals("김렛츠", result.profile?.name)
        assertEquals("렛츠", result.profile?.nickname)
        assertEquals("오공고대학교", result.profile?.university)
        assertEquals(UserGrade.GRADUATE, result.profile?.grade)
        assertEquals("개발", result.profile?.wishField)
        assertNull(result.companyProfile)
        // 일반 회원은 users.email이 없으므로 프로필의 이메일을 대표 이메일로 쓴다.
        assertEquals("lets@career.co.kr", result.email)
    }

    @Test
    fun `기업 회원은 기업 정보만 담고 렛츠커리어 프로필은 읽지 않는다`() {
        givenAccount(UserRole.COMPANY, email = "company@example.com")
        Mockito.`when`(companyProfileReader.read(USER_ID)).thenReturn(
            CompanyProfileData(organizationName = "렛츠커리어", managerName = "김담당"),
        )

        val result = service.getMyAccount(USER_ID)

        assertEquals(UserRole.COMPANY, result.role)
        assertEquals("렛츠커리어", result.companyProfile?.organizationName)
        assertEquals("김담당", result.companyProfile?.managerName)
        assertNull(result.profile)
        assertEquals("company@example.com", result.email)
    }

    @Test
    fun `정지된 계정도 막지 않고 현재 상태를 담아 반환한다`() {
        givenAccount(UserRole.USER, email = null, status = UserStatus.SUSPENDED)

        val result = service.getMyAccount(USER_ID)

        assertEquals(UserStatus.SUSPENDED, result.status)
        assertEquals(JOINED_AT, result.joinedAt)
    }

    private fun givenAccount(
        role: UserRole,
        email: String?,
        status: UserStatus = UserStatus.ACTIVE,
    ) {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(
            UserAccount(
                userId = USER_ID,
                letsCareerUserId = if (role == UserRole.COMPANY) null else LETSCAREER_USER_ID,
                email = email,
                status = status,
                role = role,
                joinedAt = JOINED_AT,
            ),
        )
    }

    @Test
    fun `프로필 수정은 사용자가 입력하는 값만 교체한다`() {
        val command = UserProfileJobInfoCommand(
            university = "오공고대학교",
            major = "컴퓨터공학과",
            grade = UserGrade.THIRD,
            wishField = "개발",
            wishJob = null,
            wishIndustry = null,
            wishEmploymentType = null,
            wishCompany = null,
        )

        service.replaceMyProfile(USER_ID, command)

        Mockito.verify(userProfileManager).replaceJobInfo(USER_ID, command, NOW)
    }

    companion object {
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private val JOINED_AT: LocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0)
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
    }
}