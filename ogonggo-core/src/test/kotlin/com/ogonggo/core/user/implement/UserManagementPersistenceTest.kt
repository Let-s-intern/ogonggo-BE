package com.ogonggo.core.user.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.user.domain.UserInflowSource
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.CompanyProfileAppendDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.user.implement.dto.UserProfileSyncDto
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.user.persistence.UserQueryRepository
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    UserManagementReader::class,
    UserManager::class,
    UserAppender::class,
    UserProfileManager::class,
    CompanyProfileAppender::class,
    UserQueryRepository::class,
)
internal class UserManagementPersistenceTest @Autowired constructor(
    private val userManagementReader: UserManagementReader,
    private val userManager: UserManager,
    private val userAppender: UserAppender,
    private val userProfileManager: UserProfileManager,
    private val companyProfileAppender: CompanyProfileAppender,
    private val userRepository: UserJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `일반 회원 목록은 기업 회원을 빼고 멘토와 관리자를 포함해 최근 가입 순으로 주며 프로필이 없는 회원도 싣는다`() {
        // given
        val older = generalMember(letsCareerUserId = 1L, nickname = "렛츠")
        val withoutProfile = userAppender.append(UserAppendDto(letsCareerUserId = 2L, joinedAt = NOW)).userId
        val admin = generalMember(letsCareerUserId = 3L, nickname = "운영자")
        promoteToAdmin(admin)
        val mentor = userAppender.append(UserAppendDto(letsCareerUserId = 4L, joinedAt = NOW, isMentor = true)).userId
        companyMember(email = "hr@ogonggo.co.kr", organizationName = "오공고", managerName = "김담당")

        // when
        val result = userManagementReader.readGeneralMemberPage(UserManagementSearchCondition(), page = 0, size = 20)

        // then
        assertEquals(listOf(mentor, admin, withoutProfile, older), result.members.map { it.userId })
        assertEquals(listOf(UserRole.MENTOR, UserRole.ADMIN, UserRole.USER, UserRole.USER), result.members.map { it.role })
        assertEquals(4, result.totalElements)
        assertNull(result.members[2].profile)
        assertEquals("렛츠", result.members.last().profile?.nickname)
    }

    @Test
    fun `일반 회원 목록을 역할로 거른다`() {
        // given
        generalMember(letsCareerUserId = 1L)
        val mentor = userAppender.append(UserAppendDto(letsCareerUserId = 2L, joinedAt = NOW, isMentor = true)).userId

        // when
        val result = userManagementReader.readGeneralMemberPage(
            UserManagementSearchCondition(role = UserRole.MENTOR),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(mentor), result.members.map { it.userId })
    }

    @Test
    fun `일반 회원 목록을 유입 구분으로 거르며 나중에 채운 회원도 걸린다`() {
        // given
        val existing = userAppender.append(
            UserAppendDto(letsCareerUserId = 1L, joinedAt = NOW, letsCareerJoinedAt = NOW.minusYears(1)),
        ).userId
        val newcomer = userAppender.append(
            UserAppendDto(letsCareerUserId = 2L, joinedAt = NOW, letsCareerJoinedAt = NOW.minusMinutes(5)),
        ).userId
        val filledLater = userAppender.append(UserAppendDto(letsCareerUserId = 3L, joinedAt = NOW)).userId
        userAppender.append(UserAppendDto(letsCareerUserId = 4L, joinedAt = NOW))
        userManager.classifyInflowSource(filledLater, null, NOW.minusSeconds(30))
        entityManager.flush()
        entityManager.clear()

        // when
        val letsCareer = userManagementReader.readGeneralMemberPage(
            UserManagementSearchCondition(inflowSource = UserInflowSource.LETSCAREER),
            page = 0,
            size = 20,
        )
        val ogonggo = userManagementReader.readGeneralMemberPage(
            UserManagementSearchCondition(inflowSource = UserInflowSource.OGONGGO),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(existing), letsCareer.members.map { it.userId })
        assertEquals(listOf(filledLater, newcomer), ogonggo.members.map { it.userId })
        assertEquals(UserInflowSource.OGONGGO, ogonggo.members.first().inflowSource)
    }

    @Test
    fun `일반 회원은 이름이나 닉네임이나 이메일로 대소문자를 가리지 않고 찾는다`() {
        // given
        val byNickname = generalMember(letsCareerUserId = 1L, nickname = "CareerKim", email = "a@test.com")
        val byEmail = generalMember(letsCareerUserId = 2L, nickname = "렛츠", email = "career@test.com")
        generalMember(letsCareerUserId = 3L, nickname = "오공고", email = "b@test.com")
        val byName = generalMember(letsCareerUserId = 4L, name = "Career Lee")

        // when
        val result = userManagementReader.readGeneralMemberPage(
            UserManagementSearchCondition(keyword = "CAREER"),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(byName, byEmail, byNickname), result.members.map { it.userId })
    }

    @Test
    fun `상태와 가입 기간으로 거르며 가입 기간은 시작일과 종료일을 모두 포함한다`() {
        // given
        generalMember(letsCareerUserId = 1L, joinedAt = LocalDateTime.of(2026, 9, 9, 23, 59, 59))
        val firstDay = generalMember(letsCareerUserId = 2L, joinedAt = LocalDateTime.of(2026, 9, 10, 0, 0))
        val lastDay = generalMember(letsCareerUserId = 3L, joinedAt = LocalDateTime.of(2026, 9, 20, 23, 59, 59))
        generalMember(letsCareerUserId = 4L, joinedAt = LocalDateTime.of(2026, 9, 21, 0, 0))
        val suspended = generalMember(letsCareerUserId = 5L, joinedAt = LocalDateTime.of(2026, 9, 15, 12, 0))
        userRepository.findByIdOrNull(suspended)!!.suspend()
        entityManager.flush()

        // when
        val period = UserManagementSearchCondition(
            joinedFrom = LocalDate.of(2026, 9, 10),
            joinedTo = LocalDate.of(2026, 9, 20),
        )
        val inPeriod = userManagementReader.readGeneralMemberPage(period, page = 0, size = 20)
        val suspendedInPeriod = userManagementReader.readGeneralMemberPage(
            period.copy(status = UserStatus.SUSPENDED),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(suspended, lastDay, firstDay), inPeriod.members.map { it.userId })
        assertEquals(listOf(suspended), suspendedInPeriod.members.map { it.userId })
    }

    @Test
    fun `정지한 회원도 역할을 바꾸며 같은 역할로 바꾸면 그대로 둔다`() {
        // given
        val userId = generalMember(letsCareerUserId = 1L)
        userRepository.findByIdOrNull(userId)!!.suspend()
        entityManager.flush()

        // when
        userManager.changeRole(userId, UserRole.MENTOR)
        userManager.changeRole(userId, UserRole.MENTOR)
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(UserRole.MENTOR, userRepository.findByIdOrNull(userId)!!.role)
    }

    @Test
    fun `기업 회원과 탈퇴한 회원은 역할을 바꿀 수 없다`() {
        // given
        val company = companyMember(email = "hr@ogonggo.co.kr", organizationName = "오공고", managerName = "김담당")
        val withdrawn = generalMember(letsCareerUserId = 1L)
        userRepository.findByIdOrNull(withdrawn)!!.withdraw(NOW)
        entityManager.flush()

        // when & then
        val companyError = assertThrows(InvalidValueException::class.java) { userManager.changeRole(company, UserRole.MENTOR) }
        assertEquals(UserErrorCode.COMPANY_MEMBER_ROLE_UNCHANGEABLE, companyError.errorCode)
        val withdrawnError = assertThrows(ConflictException::class.java) { userManager.changeRole(withdrawn, UserRole.MENTOR) }
        assertEquals(UserErrorCode.WITHDRAWN_USER_ROLE_UNCHANGEABLE, withdrawnError.errorCode)
    }

    @Test
    fun `기업 회원은 회사명이나 담당자 이름이나 계정 이메일로 찾는다`() {
        // given
        val byOrganization = companyMember(email = "a@test.com", organizationName = "오공고랩스", managerName = "김담당")
        val byManager = companyMember(email = "b@test.com", organizationName = "렛츠커리어", managerName = "오공고")
        companyMember(email = "c@test.com", organizationName = "다른회사", managerName = "이담당")
        generalMember(letsCareerUserId = 1L, nickname = "오공고")
        val byEmail = companyMember(email = "recruit@ogonggo.co.kr", organizationName = "채용회사", managerName = "박담당")

        // when
        val result = userManagementReader.readCompanyMemberPage(
            UserManagementSearchCondition(keyword = "오공고"),
            page = 0,
            size = 20,
        )
        val byEmailResult = userManagementReader.readCompanyMemberPage(
            UserManagementSearchCondition(keyword = "RECRUIT@"),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(byManager, byOrganization), result.members.map { it.userId })
        assertEquals("오공고랩스", result.members.last().companyProfile?.organizationName)
        assertEquals(listOf(byEmail), byEmailResult.members.map { it.userId })
    }

    private fun generalMember(
        letsCareerUserId: Long,
        name: String = "회원$letsCareerUserId",
        nickname: String? = null,
        email: String? = null,
        joinedAt: LocalDateTime = NOW,
    ): Long {
        val userId = userAppender.append(UserAppendDto(letsCareerUserId = letsCareerUserId, joinedAt = joinedAt)).userId
        userProfileManager.sync(
            UserProfileSyncDto(
                userId = userId,
                name = name,
                email = email,
                phoneNum = null,
                letsCareerAuthProvider = null,
                nickname = nickname,
                profileImageUrl = null,
                letsCareerUpdatedAt = NOW,
                syncedAt = NOW,
            ),
        )
        return userId
    }

    private fun companyMember(email: String, organizationName: String, managerName: String): Long {
        val userId = userAppender.appendCompany(
            CompanyAccountAppendDto(email = email, encodedPassword = "encoded", joinedAt = NOW),
        ).userId
        companyProfileAppender.append(
            CompanyProfileAppendDto(userId = userId, organizationName = organizationName, managerName = managerName),
        )
        return userId
    }

    private fun promoteToAdmin(userId: Long) {
        userManager.changeRole(userId, UserRole.ADMIN)
        entityManager.flush()
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 28, 10, 0)
    }
}
