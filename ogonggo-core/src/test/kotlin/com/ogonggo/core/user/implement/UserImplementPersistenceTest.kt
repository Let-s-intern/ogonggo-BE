package com.ogonggo.core.user.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.CompanyProfileAppendDto
import com.ogonggo.core.user.implement.dto.CompanyBasicInfoUpdateDto
import com.ogonggo.core.user.implement.dto.CompanyLogoDto
import com.ogonggo.core.user.implement.dto.CompanyManagerInfoUpdateDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.core.user.implement.dto.UserProfileSyncDto
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    UserReader::class,
    UserAppender::class,
    UserManager::class,
    LetsCareerJobProfileOutboxManager::class,
    UserProfileManager::class,
    UserProfileReader::class,
    CompanyProfileAppender::class,
    CompanyProfileReader::class,
    CompanyProfileManager::class,
)
internal class UserImplementPersistenceTest @Autowired constructor(
    private val userReader: UserReader,
    private val userAppender: UserAppender,
    private val userManager: UserManager,
    private val outboxManager: LetsCareerJobProfileOutboxManager,
    private val userProfileManager: UserProfileManager,
    private val userProfileReader: UserProfileReader,
    private val companyProfileAppender: CompanyProfileAppender,
    private val companyProfileReader: CompanyProfileReader,
    private val companyProfileManager: CompanyProfileManager,
    private val userProfileRepository: UserProfileJpaRepository,
    private val companyProfileRepository: CompanyProfileJpaRepository,
) {

    @Test
    fun `렛츠커리어 식별자로 가입하고 조회한다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        assertEquals(4821L, account.letsCareerUserId)
        assertEquals(UserStatus.ACTIVE, account.status)
        assertEquals(account.userId, userReader.readByLetsCareerUserId(4821L)?.userId)
    }

    @Test
    fun `가입하지 않은 렛츠커리어 사용자는 null을 반환한다`() {
        assertNull(userReader.readByLetsCareerUserId(4821L))
    }

    @Test
    fun `FCM 토큰을 저장하고 최신 토큰으로 교체하며 이전 토큰은 지울 수 있다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        userManager.changeFcmToken(account.userId, "fcm-token-1")
        assertEquals("fcm-token-1", userReader.readFcmToken(account.userId))

        userManager.changeFcmToken(account.userId, "fcm-token-2")
        userManager.clearFcmToken(account.userId, "fcm-token-1")
        assertEquals("fcm-token-2", userReader.readFcmToken(account.userId))

        userManager.clearFcmToken(account.userId, "fcm-token-2")
        assertNull(userReader.readFcmToken(account.userId))
    }

    @Test
    fun `존재하지 않는 사용자 조회는 USER_NOT_FOUND로 실패한다`() {
        val exception = assertThrows(EntityNotFoundException::class.java) { userReader.read(9999L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `같은 렛츠커리어 사용자를 두 번 가입시키면 재시도 가능한 충돌로 처리한다`() {
        userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        val exception = assertThrows(ConflictException::class.java) {
            userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        }

        assertEquals(UserErrorCode.USER_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `프로필이 없으면 생성하고 렛츠커리어 수정 일시가 바뀌면 갱신한다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))
        val created = userProfileRepository.findByUserId(account.userId)
        assertNotNull(created)
        assertEquals("김렛츠", created?.name)

        userProfileManager.sync(
            syncCommand(account.userId, name = "김커리어", letsCareerUpdatedAt = NOW.plusDays(1)),
        )

        assertEquals("김커리어", userProfileRepository.findByUserId(account.userId)?.name)
    }

    @Test
    fun `렛츠커리어 수정 일시가 같으면 프로필을 갱신하지 않는다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))

        userProfileManager.sync(syncCommand(account.userId, name = "바뀐이름", letsCareerUpdatedAt = NOW))

        val profile = userProfileRepository.findByUserId(account.userId)
        assertEquals("김렛츠", profile?.name)
        assertEquals(NOW, profile?.lastSyncedAt)
    }

    @Test
    fun `Reader는 계정 종류에 없는 프로필을 null로 반환한다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))

        val profile = userProfileReader.read(account.userId)

        assertEquals("김렛츠", profile?.name)
        assertEquals("lets@career.co.kr", profile?.email)
        assertEquals("렛츠", profile?.nickname)
        // 일반 회원에게는 기업 정보가 없다.
        assertNull(companyProfileReader.read(account.userId))

        val companyAccount = userAppender.appendCompany(
            CompanyAccountAppendDto(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )
        companyProfileAppender.append(
            CompanyProfileAppendDto(
                userId = companyAccount.userId,
                organizationName = "렛츠커리어",
                managerName = "김담당",
            ),
        )

        assertEquals("렛츠커리어", companyProfileReader.read(companyAccount.userId)?.organizationName)
        assertEquals("김담당", companyProfileReader.read(companyAccount.userId)?.managerName)
        // 기업 회원에게는 렛츠커리어 프로필이 없다.
        assertNull(userProfileReader.read(companyAccount.userId))
    }

    @Test
    fun `기업 정보는 기존 행의 모든 값을 함께 교체한다`() {
        // given
        val companyAccount = userAppender.appendCompany(
            CompanyAccountAppendDto(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )
        companyProfileAppender.append(
            CompanyProfileAppendDto(
                userId = companyAccount.userId,
                organizationName = "렛츠커리어",
                managerName = "김담당",
            ),
        )

        // when
        companyProfileManager.replaceBasicInfo(
            companyAccount.userId,
            CompanyBasicInfoUpdateDto(
                organizationName = "오공고",
                logo = CompanyLogoDto(imageId = "logo-image", url = "https://cdn.example.com/logo.png"),
            ),
        )
        companyProfileManager.replaceManagerInfo(
            companyAccount.userId,
            CompanyManagerInfoUpdateDto(
                managerName = "이담당",
                managerPhone = "010-1234-5678",
                notificationEmail = "hr@example.com",
            ),
        )

        // then
        val replaced = companyProfileReader.read(companyAccount.userId)
        assertEquals("오공고", replaced?.organizationName)
        assertEquals("이담당", replaced?.managerName)
        assertEquals("logo-image", replaced?.logoImageId)
        assertEquals("https://cdn.example.com/logo.png", replaced?.logoUrl)
        assertEquals("010-1234-5678", replaced?.managerPhone)
        assertEquals("hr@example.com", replaced?.notificationEmail)
        assertEquals(1L, companyProfileRepository.count())
    }

    @Test
    fun `구직 정보는 프로필 행에 함께 저장하고 여덟 값을 한 번에 교체한다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))

        userProfileManager.replaceJobInfo(
            account.userId,
            UserProfileJobInfoDto(
                "오공고대학교", "컴퓨터공학과", UserGrade.GRADUATE,
                "개발", "백엔드 개발", "IT", "정규직", "오공고",
            ),
            NOW,
        )

        val saved = userProfileReader.read(account.userId)
        assertEquals("오공고대학교", saved?.university)
        assertEquals(UserGrade.GRADUATE, saved?.grade)
        assertEquals("개발", saved?.wishField)
        // 렛츠커리어에서 복제한 값은 그대로다.
        assertEquals("김렛츠", saved?.name)

        // 보내지 않은 값은 비우는 것으로 본다.
        userProfileManager.replaceJobInfo(
            account.userId,
            UserProfileJobInfoDto(null, null, null, "데이터", null, null, null, null),
            NOW.plusMinutes(1),
        )

        val replaced = userProfileReader.read(account.userId)
        assertEquals("데이터", replaced?.wishField)
        assertNull(replaced?.university)
        assertNull(replaced?.grade)
        assertNull(replaced?.wishJob)
        assertEquals(1L, userProfileRepository.count())
    }

    @Test
    fun `로그인 동기화는 사용자가 입력한 구직 정보를 덮어쓰지 않는다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))
        userProfileManager.replaceJobInfo(
            account.userId,
            UserProfileJobInfoDto(null, null, null, "개발", null, null, null, null),
            NOW,
        )

        // 렛츠커리어 값이 바뀌어 다시 동기화해도 구직 정보는 남는다.
        userProfileManager.sync(
            syncCommand(account.userId, name = "김커리어", letsCareerUpdatedAt = NOW.plusDays(1)),
        )

        val profile = userProfileReader.read(account.userId)
        assertEquals("김커리어", profile?.name)
        assertEquals("개발", profile?.wishField)
    }

    @Test
    fun `프로필 행이 없어도 구직 정보를 저장하면 행이 생긴다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendDto("mock@example.com", "encoded-password", NOW),
        )

        assertNull(userProfileReader.read(account.userId))

        userProfileManager.replaceJobInfo(
            account.userId,
            UserProfileJobInfoDto(null, null, null, "개발", null, null, null, null),
            NOW,
        )

        assertEquals("개발", userProfileReader.read(account.userId)?.wishField)
    }

    @Test
    fun `계정 조회는 가입 일시를 함께 담는다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        assertEquals(NOW, userReader.read(account.userId).joinedAt)
    }

    @Test
    fun `기업 계정을 만들고 기업 프로필을 함께 저장한다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendDto(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )
        companyProfileAppender.append(
            CompanyProfileAppendDto(
                userId = account.userId,
                organizationName = "렛츠커리어",
                managerName = "김담당",
            ),
        )

        assertEquals(UserRole.COMPANY, account.role)
        assertNull(account.letsCareerUserId)
        assertEquals("company@example.com", account.email)
        assertNotNull(companyProfileRepository.findByUserId(account.userId))
    }

    @Test
    fun `같은 이메일로 두 번 가입하면 충돌로 처리한다`() {
        val command = CompanyAccountAppendDto(
            email = "company@example.com",
            encodedPassword = "encoded-password",
            joinedAt = NOW,
        )
        userAppender.appendCompany(command)

        val exception = assertThrows(ConflictException::class.java) { userAppender.appendCompany(command) }

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `이메일로 기업 계정의 자격증명을 조회한다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendDto(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )

        val credential = userReader.readCredentialByEmail("company@example.com")

        assertEquals(account.userId, credential?.userId)
        assertEquals("encoded-password", credential?.encodedPassword)
        assertEquals(UserRole.COMPANY, credential?.role)
    }

    @Test
    fun `자격증명이 없는 렛츠커리어 계정은 이메일로 조회되지 않는다`() {
        userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        assertNull(userReader.readCredentialByEmail("company@example.com"))
    }

    @Test
    fun `같은 사용자의 기업 프로필을 두 번 만들면 충돌로 처리한다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendDto(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )
        val command = CompanyProfileAppendDto(
            userId = account.userId,
            organizationName = "렛츠커리어",
            managerName = "김담당",
        )
        companyProfileAppender.append(command)

        val exception = assertThrows(ConflictException::class.java) { companyProfileAppender.append(command) }

        assertEquals(UserErrorCode.COMPANY_PROFILE_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `렛츠커리어 수정 일시가 같아도 휴대폰 번호나 가입 경로가 비어 있으면 채운다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        // 휴대폰 번호와 가입 경로를 복제하기 전에 만들어진 행이다.
        userProfileManager.sync(
            syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW, phoneNum = null, authProvider = null),
        )

        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))

        val profile = userProfileReader.read(account.userId)
        assertEquals("010-1234-5678", profile?.phoneNum)
        assertEquals(LetsCareerAuthProvider.SERVICE, profile?.letsCareerAuthProvider)
    }

    @Test
    fun `로그인 동기화는 사용자가 입력한 수신 이메일을 덮어쓰지 않는다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))
        userProfileManager.changeNotificationEmail(account.userId, "today@example.com", NOW)

        userProfileManager.sync(
            syncCommand(account.userId, name = "김커리어", letsCareerUpdatedAt = NOW.plusDays(1)),
        )

        val profile = userProfileReader.read(account.userId)
        assertEquals("김커리어", profile?.name)
        assertEquals("today@example.com", profile?.notificationEmail)
    }

    @Test
    fun `프로필 행이 없어도 수신 이메일을 저장하면 행이 생기고 null이면 비운다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        userProfileManager.changeNotificationEmail(account.userId, "today@example.com", NOW)
        assertEquals("today@example.com", userProfileReader.read(account.userId)?.notificationEmail)

        userProfileManager.changeNotificationEmail(account.userId, null, NOW)
        assertNull(userProfileReader.read(account.userId)?.notificationEmail)
    }

    @Test
    fun `기업 회원의 비밀번호를 바꾸면 새 값으로 로그인 자격증명이 바뀐다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendDto("mock@example.com", "encoded-password", NOW),
        )

        userManager.changePassword(account.userId, "new-encoded-password")

        assertEquals("new-encoded-password", userReader.readCredential(account.userId)?.encodedPassword)
    }

    @Test
    fun `일반 회원은 비밀번호 자격증명이 없다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        assertNull(userReader.readCredential(account.userId))
    }

    @Test
    fun `오공고에서 고치면 고친 일시를 남긴다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        userProfileManager.replaceJobInfo(account.userId, jobInfo("개발"), NOW)

        assertEquals(NOW, userProfileReader.read(account.userId)?.jobInfoUpdatedAt)
    }

    @Test
    fun `렛츠커리어 값은 더 나중에 고친 것일 때만 반영하고 렛츠커리어의 일시를 남긴다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.replaceJobInfo(account.userId, jobInfo("오공고에서 고침"), NOW)

        // 같은 일시(이미 받은 수정)·더 이른 일시·일시 없음은 받지 않는다.
        listOf(NOW, NOW.minusMinutes(1), null).forEach { letsCareerUpdatedAt ->
            assertEquals(
                false,
                userProfileManager.applyLetsCareerJobInfo(account.userId, jobInfo("렛츠커리어"), letsCareerUpdatedAt, NOW),
            )
        }
        assertEquals("오공고에서 고침", userProfileReader.read(account.userId)?.wishField)

        val later = NOW.plusMinutes(1)
        assertEquals(true, userProfileManager.applyLetsCareerJobInfo(account.userId, jobInfo("렛츠커리어"), later, NOW))

        val profile = userProfileReader.read(account.userId)
        assertEquals("렛츠커리어", profile?.wishField)
        assertEquals(later, profile?.jobInfoUpdatedAt)
    }

    @Test
    fun `한 번도 고친 적 없으면 렛츠커리어 값을 일시가 없어도 받는다`() {
        val account = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))

        assertEquals(true, userProfileManager.applyLetsCareerJobInfo(account.userId, jobInfo("렛츠커리어"), null, NOW))

        val profile = userProfileReader.read(account.userId)
        assertEquals("렛츠커리어", profile?.wishField)
        assertNull(profile?.jobInfoUpdatedAt)
    }

    @Test
    fun `아웃박스는 사용자당 한 행이고 다시 적재하면 일시만 바뀐다`() {
        outboxManager.enqueue(17L, NOW)
        outboxManager.enqueue(17L, NOW.plusMinutes(1))

        val pending = outboxManager.readPending()
        assertEquals(1, pending.size)
        assertEquals(NOW.plusMinutes(1), pending.single().requestedAt)
    }

    @Test
    fun `보내는 사이 다시 적재됐으면 보낸 것으로 표시하지 않는다`() {
        outboxManager.enqueue(17L, NOW)
        val sending = outboxManager.readPending().single()
        outboxManager.enqueue(17L, NOW.plusMinutes(1))

        outboxManager.markSent(sending, NOW.plusMinutes(2))

        assertEquals(NOW.plusMinutes(1), outboxManager.readPending().single().requestedAt)

        outboxManager.markSent(outboxManager.readPending().single(), NOW.plusMinutes(3))
        assertEquals(0, outboxManager.readPending().size)
    }

    @Test
    fun `실패가 적은 행부터 보낸다`() {
        outboxManager.enqueue(17L, NOW)
        outboxManager.enqueue(18L, NOW.plusMinutes(1))
        outboxManager.markFailed(outboxManager.readPending().first { it.userId == 17L })

        val pending = outboxManager.readPending()
        assertEquals(listOf(18L, 17L), pending.map { it.userId })
        assertEquals(1, pending.last().attemptCount)
    }

    private fun jobInfo(wishField: String) =
        UserProfileJobInfoDto(null, null, null, wishField, null, null, null, null)

    private fun syncCommand(
        userId: Long,
        name: String,
        letsCareerUpdatedAt: LocalDateTime,
        phoneNum: String? = "010-1234-5678",
        authProvider: LetsCareerAuthProvider? = LetsCareerAuthProvider.SERVICE,
    ): UserProfileSyncDto = UserProfileSyncDto(
        userId = userId,
        name = name,
        email = "lets@career.co.kr",
        phoneNum = phoneNum,
        letsCareerAuthProvider = authProvider,
        nickname = "렛츠",
        profileImageUrl = null,
        letsCareerUpdatedAt = letsCareerUpdatedAt,
        syncedAt = letsCareerUpdatedAt,
    )

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 27, 10, 0)
    }
}
