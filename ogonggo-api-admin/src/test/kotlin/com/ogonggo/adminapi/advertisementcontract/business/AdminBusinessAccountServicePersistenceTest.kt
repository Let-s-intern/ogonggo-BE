package com.ogonggo.adminapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-business-account-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminBusinessAccountServicePersistenceTest @Autowired constructor(
    private val service: AdminBusinessAccountService,
    private val contractAppender: AdvertisementContractAppender,
    private val contractReader: AdvertisementContractReader,
    private val userAppender: UserAppender,
    private val userReader: UserReader,
    private val companyProfileReader: CompanyProfileReader,
    private val jdbcTemplate: JdbcTemplate,
) {

    /** 테스트마다 커밋하므로 이메일을 나눠 서로의 행을 보지 않는다. */
    private val email = "ad-${System.nanoTime()}@example.com"

    @Test
    fun `가입하지 않은 이메일이면 신청서 값으로 비즈니스 회원을 만들고 같은 이메일의 비회원 신청을 모두 붙인다`() {
        // given
        val contractId = contractAppender.appendFromForm(form(email))
        val otherContractId = contractAppender.appendFromForm(form(email))
        val otherEmailContractId = contractAppender.appendFromForm(form("other@example.com"))

        // when
        val result = service.issue(contractId)

        // then
        assertTrue(result.created)
        assertEquals(2, result.linkedContractCount)
        val account = checkNotNull(userReader.readByEmail(email))
        assertEquals(result.userId, account.userId)
        assertEquals(UserRole.COMPANY, account.role)
        val profile = checkNotNull(companyProfileReader.read(result.userId))
        assertEquals("샘플기업", profile.organizationName)
        assertEquals("김담당", profile.managerName)
        assertEquals("010-1234-5678", profile.managerPhone)
        assertEquals(CompanyBusinessNumberType.BUSINESS_REGISTRATION, profile.businessNumberType)
        assertEquals("1234567890", profile.businessNumber)
        assertEquals(result.userId, contractReader.read(contractId).userId)
        assertEquals(result.userId, contractReader.read(otherContractId).userId)
        assertEquals(null, contractReader.read(otherEmailContractId).userId)
    }

    @Test
    fun `새로 만든 계정의 임시 비밀번호를 메일 대기열에 넣고 같은 값이 로그인 비밀번호가 된다`() {
        // given
        val contractId = contractAppender.appendFromForm(form(email))

        // when
        val result = service.issue(contractId)

        // then
        val row = jdbcTemplate.queryForMap(
            "select template_code, recipient_address, payload_json from notifications where recipient_user_id = ?",
            result.userId,
        )
        assertEquals("company_account_issued", row["template_code"])
        assertEquals(email, row["recipient_address"])
        val temporaryPassword = Regex("\"temporaryPassword\":\"([^\"]+)\"").find(row["payload_json"].toString())!!.groupValues[1]
        val credential = checkNotNull(userReader.readCredentialByEmail(email))
        assertTrue(BCryptPasswordEncoder().matches(temporaryPassword, credential.encodedPassword))
    }

    @Test
    fun `이미 가입한 비즈니스 회원이면 새로 만들지 않고 비밀번호와 메일 없이 신청만 붙인다`() {
        // given
        val member = userAppender.appendCompany(CompanyAccountAppendDto(email, "encoded-password", NOW))
        val contractId = contractAppender.appendFromForm(form(email))

        // when
        val result = service.issue(contractId)

        // then
        assertFalse(result.created)
        assertEquals(member.userId, result.userId)
        assertEquals(1, result.linkedContractCount)
        assertEquals(member.userId, contractReader.read(contractId).userId)
        assertEquals("encoded-password", userReader.readCredentialByEmail(email)?.encodedPassword)
        assertEquals(0, countNotifications())
    }

    @Test
    fun `정지된 비즈니스 회원의 이메일이면 409이고 신청은 그대로 둔다`() {
        // given
        val member = userAppender.appendCompany(CompanyAccountAppendDto(email, "encoded-password", NOW))
        jdbcTemplate.update("update users set status = 'SUSPENDED' where id = ?", member.userId)
        val contractId = contractAppender.appendFromForm(form(email))

        // when
        val exception = assertThrows(ConflictException::class.java) { service.issue(contractId) }

        // then
        assertEquals(AdvertisementContractErrorCode.BUSINESS_ACCOUNT_MEMBER_NOT_ACTIVE, exception.errorCode)
        assertEquals(null, contractReader.read(contractId).userId)
    }

    @Test
    fun `회원이 이미 있는 신청이나 비즈니스 공고 계약에는 발급하지 않는다`() {
        // given
        val memberContractId = contractAppender.appendFromForm(form(email, userId = 77L))
        val postingContractId = contractAppender.appendForCompanyPosting(
            AdvertisementContractCompanyPostingAppendDto(
                userId = 77L,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                targetId = 501L,
                companyName = "샘플기업",
                recruitmentTitle = "백엔드 인턴",
                recruitmentDeadline = null,
            ),
        )

        // when, then
        listOf(memberContractId, postingContractId).forEach { contractId ->
            val exception = assertThrows(ConflictException::class.java) { service.issue(contractId) }
            assertEquals(AdvertisementContractErrorCode.BUSINESS_ACCOUNT_NOT_ISSUABLE, exception.errorCode)
        }
        assertEquals(null, userReader.readByEmail(email))
    }

    @Test
    fun `발급한 뒤 다시 누르면 이미 붙은 신청이라 409다`() {
        // given
        val contractId = contractAppender.appendFromForm(form(email))
        val first = service.issue(contractId)

        // when
        val exception = assertThrows(ConflictException::class.java) { service.issue(contractId) }

        // then
        assertEquals(AdvertisementContractErrorCode.BUSINESS_ACCOUNT_NOT_ISSUABLE, exception.errorCode)
        assertNotEquals(0L, first.userId)
        assertEquals(1, countNotifications())
    }

    private fun countNotifications(): Int =
        checkNotNull(jdbcTemplate.queryForObject("select count(*) from notifications where recipient_address = ?", Int::class.java, email))

    private fun form(email: String, userId: Long? = null) = AdvertisementContractFormAppendDto(
        userId = userId,
        promotionTarget = AdvertisementContractPromotionTarget.JOB,
        companyName = "샘플기업",
        recruitmentTitle = "백엔드 인턴",
        recruitmentUrl = "https://example.com/recruit",
        recruitmentDeadline = LocalDate.of(2026, 12, 31),
        purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
        budgetAmount = null,
        categories = emptySet(),
        scheduleRequest = null,
        advertisementRequest = null,
        managerName = "김담당",
        email = email,
        phoneNumber = "010-1234-5678",
        businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION,
        businessNumber = "1234567890",
        invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
        creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 11, 12, 0)
    }
}
