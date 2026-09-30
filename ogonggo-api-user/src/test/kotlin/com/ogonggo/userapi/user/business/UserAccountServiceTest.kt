package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.CompanyProfileManager
import com.ogonggo.core.user.implement.dto.CompanyProfileDto
import com.ogonggo.core.user.implement.dto.CompanyBasicInfoUpdateDto
import com.ogonggo.core.user.implement.dto.CompanyLogoDto
import com.ogonggo.core.user.implement.dto.CompanyManagerInfoUpdateDto
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.LetsCareerJobProfileOutboxManager
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    private val companyProfileManager = Mockito.mock(CompanyProfileManager::class.java)
    private val letsCareerJobProfileOutboxManager = Mockito.mock(LetsCareerJobProfileOutboxManager::class.java)
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = UserAccountService(
        userReader,
        userProfileReader,
        companyProfileReader,
        userProfileManager,
        companyProfileManager,
        letsCareerJobProfileOutboxManager,
        imageAssetManager,
        clock,
    )

    @Test
    fun `일반 회원은 렛츠커리어 프로필만 담고 기업 정보는 읽지 않는다`() {
        givenAccount(UserRole.USER, email = null)
        Mockito.`when`(userProfileReader.read(USER_ID)).thenReturn(
            UserProfileDto(
                name = "김렛츠",
                email = "lets@career.co.kr",
                phoneNum = "010-1234-5678",
                letsCareerAuthProvider = LetsCareerAuthProvider.KAKAO,
                notificationEmail = "today@example.com",
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
        assertEquals("010-1234-5678", result.profile?.phoneNum)
        assertEquals("today@example.com", result.profile?.notificationEmail)
        assertEquals(LetsCareerAuthProvider.KAKAO, result.profile?.authProvider)
        // 카카오로 가입해 비밀번호가 없다.
        assertEquals(false, result.passwordChangeable)
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
            CompanyProfileDto(
                organizationName = "렛츠커리어",
                managerName = "김담당",
                logoUrl = null,
                managerPhone = null,
                notificationEmail = null,
            ),
        )

        val result = service.getMyAccount(USER_ID)

        assertEquals(UserRole.COMPANY, result.role)
        assertEquals("렛츠커리어", result.companyProfile?.organizationName)
        assertEquals("김담당", result.companyProfile?.managerName)
        assertEquals(true, result.passwordChangeable)
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
            UserAccountDto(
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
        val command = UserProfileJobInfoDto(
            university = "오공고대학교",
            major = "컴퓨터공학과",
            grade = UserGrade.THIRD,
            wishField = "개발",
            wishJob = null,
            wishIndustry = null,
            wishEmploymentType = null,
            wishCompany = null,
        )

        givenAccount(UserRole.USER, email = null)

        service.replaceMyProfile(USER_ID, command)

        Mockito.verify(userProfileManager).replaceJobInfo(USER_ID, command, NOW)
        // 렛츠커리어에서도 고칠 수 있는 값이라 같은 트랜잭션에서 보낼 변경을 적재한다.
        Mockito.verify(letsCareerJobProfileOutboxManager).enqueue(USER_ID, NOW)
    }

    @Test
    fun `렛츠커리어 계정이 없으면 프로필만 고치고 보낼 변경은 적재하지 않는다`() {
        givenAccount(UserRole.COMPANY, email = "company@example.com")

        service.replaceMyProfile(USER_ID, EMPTY_JOB_INFO)

        Mockito.verify(userProfileManager).replaceJobInfo(USER_ID, EMPTY_JOB_INFO, NOW)
        Mockito.verifyNoInteractions(letsCareerJobProfileOutboxManager)
    }

    @Test
    fun `기업 로고를 바꾸면 새 이미지를 연결하고 전에 쓰던 로고 이미지는 참조를 푼다`() {
        // given
        givenAccount(UserRole.COMPANY, email = "company@example.com")
        Mockito.`when`(imageAssetManager.attachProfileImage(USER_ID, "new-logo")).thenReturn(NEW_IMAGE_URL)
        Mockito.`when`(
            companyProfileManager.replaceBasicInfo(
                USER_ID,
                CompanyBasicInfoUpdateDto("오공고", CompanyLogoDto(imageId = "new-logo", url = NEW_IMAGE_URL)),
            ),
        ).thenReturn("old-logo")

        // when
        service.replaceMyCompanyBasicInfo(USER_ID, CompanyBasicInfoCommand("오공고", logoImageId = "new-logo"))

        // then
        Mockito.verify(imageAssetManager).unreferenceProfileImage(USER_ID, "old-logo", NOW)
    }

    @Test
    fun `로고를 빼고 기본 정보를 고치면 로고를 지우고 전에 쓰던 로고 이미지는 참조를 푼다`() {
        // given
        givenAccount(UserRole.COMPANY, email = "company@example.com")
        Mockito.`when`(companyProfileManager.replaceBasicInfo(USER_ID, CompanyBasicInfoUpdateDto("오공고", logo = null)))
            .thenReturn("old-logo")

        // when
        service.replaceMyCompanyBasicInfo(USER_ID, CompanyBasicInfoCommand("오공고", logoImageId = null))

        // then
        Mockito.verify(imageAssetManager).unreferenceProfileImage(USER_ID, "old-logo", NOW)
    }

    @Test
    fun `기업 회원은 담당자 정보를 교체한다`() {
        // given
        givenAccount(UserRole.COMPANY, email = "company@example.com")

        // when
        service.replaceMyCompanyManagerInfo(USER_ID, MANAGER_INFO_COMMAND)

        // then
        Mockito.verify(companyProfileManager).replaceManagerInfo(USER_ID, MANAGER_INFO_COMMAND)
    }

    @Test
    fun `일반 회원이 기업 정보를 고치면 COMPANY_ROLE_REQUIRED로 막고 저장하지 않는다`() {
        // given
        givenAccount(UserRole.USER, email = null)

        // when
        val exception = assertThrows(ForbiddenException::class.java) {
            service.replaceMyCompanyBasicInfo(USER_ID, CompanyBasicInfoCommand("오공고", logoImageId = "new-logo"))
        }

        // then
        assertEquals(UserErrorCode.COMPANY_ROLE_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(companyProfileManager, imageAssetManager)
    }

    @Test
    fun `정지된 기업 회원은 기업 정보를 고칠 수 없다`() {
        // given
        givenAccount(UserRole.COMPANY, email = "company@example.com", status = UserStatus.SUSPENDED)

        // when
        val exception = assertThrows(ForbiddenException::class.java) {
            service.replaceMyCompanyManagerInfo(USER_ID, MANAGER_INFO_COMMAND)
        }

        // then
        assertEquals(UserErrorCode.USER_SUSPENDED, exception.errorCode)
        Mockito.verifyNoInteractions(companyProfileManager)
    }

    @Test
    fun `일반 회원은 수신 이메일을 오공고 프로필에 저장한다`() {
        // given
        givenAccount(UserRole.USER, email = null)

        // when
        service.changeMyNotificationEmail(USER_ID, "today@example.com")

        // then
        Mockito.verify(userProfileManager).changeNotificationEmail(USER_ID, "today@example.com", NOW)
    }

    @Test
    fun `기업 회원이 수신 이메일을 고치면 GENERAL_MEMBER_REQUIRED로 막는다`() {
        // given
        givenAccount(UserRole.COMPANY, email = "company@example.com")

        // when
        val exception = assertThrows(ForbiddenException::class.java) {
            service.changeMyNotificationEmail(USER_ID, "today@example.com")
        }

        // then
        assertEquals(UserErrorCode.GENERAL_MEMBER_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(userProfileManager)
    }

    @Test
    fun `프로필 이미지를 바꾸면 새 이미지를 연결하고 전에 쓰던 이미지는 참조를 푼다`() {
        // given
        givenAccount(UserRole.USER, email = null)
        Mockito.`when`(imageAssetManager.attachProfileImage(USER_ID, "new-image")).thenReturn(NEW_IMAGE_URL)
        Mockito.`when`(userProfileManager.changeOgonggoProfileImage(USER_ID, "new-image", NEW_IMAGE_URL, NOW))
            .thenReturn("old-image")

        // when
        service.replaceMyProfileImage(USER_ID, "new-image")

        // then
        Mockito.verify(imageAssetManager).unreferenceProfileImage(USER_ID, "old-image", NOW)
    }

    @Test
    fun `같은 프로필 이미지를 다시 보내면 그 이미지의 참조를 풀지 않는다`() {
        // given
        givenAccount(UserRole.USER, email = null)
        Mockito.`when`(imageAssetManager.attachProfileImage(USER_ID, "new-image")).thenReturn(NEW_IMAGE_URL)
        Mockito.`when`(userProfileManager.changeOgonggoProfileImage(USER_ID, "new-image", NEW_IMAGE_URL, NOW))
            .thenReturn("new-image")

        // when
        service.replaceMyProfileImage(USER_ID, "new-image")

        // then
        Mockito.verify(imageAssetManager, Mockito.never()).unreferenceProfileImage(USER_ID, "new-image", NOW)
    }

    @Test
    fun `기업 회원이 프로필 이미지를 바꾸면 GENERAL_MEMBER_REQUIRED로 막고 이미지를 연결하지 않는다`() {
        // given
        givenAccount(UserRole.COMPANY, email = "company@example.com")

        // when
        val exception = assertThrows(ForbiddenException::class.java) {
            service.replaceMyProfileImage(USER_ID, "new-image")
        }

        // then
        assertEquals(UserErrorCode.GENERAL_MEMBER_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(imageAssetManager, userProfileManager)
    }

    @Test
    fun `프로필 이미지를 지우면 지운 이미지의 참조를 푼다`() {
        // given
        givenAccount(UserRole.USER, email = null)
        Mockito.`when`(userProfileManager.removeOgonggoProfileImage(USER_ID)).thenReturn("old-image")

        // when
        service.deleteMyProfileImage(USER_ID)

        // then
        Mockito.verify(imageAssetManager).unreferenceProfileImage(USER_ID, "old-image", NOW)
    }

    companion object {
        private const val NEW_IMAGE_URL = "https://cdn.example.com/images/new-image.png"
        private val MANAGER_INFO_COMMAND = CompanyManagerInfoUpdateDto(
            managerName = "이담당",
            managerPhone = null,
            notificationEmail = null,
        )
        private val EMPTY_JOB_INFO = UserProfileJobInfoDto(null, null, null, null, null, null, null, null)
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private val JOINED_AT: LocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0)
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
    }
}