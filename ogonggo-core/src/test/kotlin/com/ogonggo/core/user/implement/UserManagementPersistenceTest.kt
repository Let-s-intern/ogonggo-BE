package com.ogonggo.core.user.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserStatus
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
    UserAppender::class,
    UserProfileManager::class,
    CompanyProfileAppender::class,
    UserQueryRepository::class,
)
internal class UserManagementPersistenceTest @Autowired constructor(
    private val userManagementReader: UserManagementReader,
    private val userAppender: UserAppender,
    private val userProfileManager: UserProfileManager,
    private val companyProfileAppender: CompanyProfileAppender,
    private val userRepository: UserJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `일반 회원 목록은 기업 회원과 관리자를 빼고 최근 가입 순으로 주며 프로필이 없는 회원도 싣는다`() {
        // given
        val older = generalMember(letsCareerUserId = 1L, nickname = "렛츠")
        val withoutProfile = userAppender.append(UserAppendDto(letsCareerUserId = 2L, joinedAt = NOW)).userId
        val admin = generalMember(letsCareerUserId = 3L, nickname = "운영자")
        promoteToAdmin(admin)
        companyMember(email = "hr@ogonggo.co.kr", organizationName = "오공고", managerName = "김담당")

        // when
        val result = userManagementReader.readGeneralMemberPage(UserManagementSearchCondition(), page = 0, size = 20)

        // then
        assertEquals(listOf(withoutProfile, older), result.members.map { it.userId })
        assertEquals(2, result.totalElements)
        assertNull(result.members.first().profile)
        assertEquals("렛츠", result.members.last().profile?.nickname)
    }

    @Test
    fun `일반 회원은 닉네임이나 이메일로 대소문자를 가리지 않고 찾는다`() {
        // given
        val byNickname = generalMember(letsCareerUserId = 1L, nickname = "CareerKim", email = "a@test.com")
        val byEmail = generalMember(letsCareerUserId = 2L, nickname = "렛츠", email = "career@test.com")
        generalMember(letsCareerUserId = 3L, nickname = "오공고", email = "b@test.com")

        // when
        val result = userManagementReader.readGeneralMemberPage(
            UserManagementSearchCondition(keyword = "CAREER"),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(byEmail, byNickname), result.members.map { it.userId })
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
    fun `기업 회원은 회사명이나 담당자 이름으로 찾는다`() {
        // given
        val byOrganization = companyMember(email = "a@test.com", organizationName = "오공고랩스", managerName = "김담당")
        val byManager = companyMember(email = "b@test.com", organizationName = "렛츠커리어", managerName = "오공고")
        companyMember(email = "c@test.com", organizationName = "다른회사", managerName = "이담당")
        generalMember(letsCareerUserId = 1L, nickname = "오공고")

        // when
        val result = userManagementReader.readCompanyMemberPage(
            UserManagementSearchCondition(keyword = "오공고"),
            page = 0,
            size = 20,
        )

        // then
        assertEquals(listOf(byManager, byOrganization), result.members.map { it.userId })
        assertEquals("오공고랩스", result.members.last().companyProfile?.organizationName)
    }

    private fun generalMember(
        letsCareerUserId: Long,
        nickname: String? = null,
        email: String? = null,
        joinedAt: LocalDateTime = NOW,
    ): Long {
        val userId = userAppender.append(UserAppendDto(letsCareerUserId = letsCareerUserId, joinedAt = joinedAt)).userId
        userProfileManager.sync(
            UserProfileSyncDto(
                userId = userId,
                name = "회원$letsCareerUserId",
                email = email,
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

    /** 관리자 역할은 운영자가 DB에서 직접 부여하므로 도메인에 전이가 없다. */
    private fun promoteToAdmin(userId: Long) {
        entityManager.flush()
        entityManager.createNativeQuery("update users set role = 'ADMIN' where id = :id")
            .setParameter("id", userId)
            .executeUpdate()
        entityManager.clear()
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 28, 10, 0)
    }
}
