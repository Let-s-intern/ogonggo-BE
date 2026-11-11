package com.ogonggo.adminapi.advertisementcontract.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.advertisementcontract.implement.CompanyTemporaryPasswordIssuer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractManager
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.CompanyProfileAppender
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.CompanyProfileAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 비회원이 광고 신청 폼으로 낸 계약에서 운영자가 비즈니스 계정을 발급한다. 비회원 신청 조회 화면을 대신한다.
 *
 * 신청 이메일이 곧 로그인 이메일이다. 그 이메일의 비즈니스 회원이 없으면 신청서 값으로 만들고 임시 비밀번호를 메일로 보낸다.
 * 있으면 만들지 않고 비밀번호도 건드리지 않는다. 어느 쪽이든 그 이메일로 낸 비회원 신청을 모두 그 회원에게 붙인다.
 */
@Service
class AdminBusinessAccountService(
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementContractManager: AdvertisementContractManager,
    private val userReader: UserReader,
    private val userAppender: UserAppender,
    private val companyProfileAppender: CompanyProfileAppender,
    private val temporaryPasswordIssuer: CompanyTemporaryPasswordIssuer,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    @Transactional
    fun issue(contractId: Long): AdminBusinessAccountIssueResult {
        val contract = advertisementContractReader.readForUpdate(contractId)
        if (contract.source != AdvertisementContractSource.APPLICATION_FORM || contract.userId != null) {
            throw ConflictException(AdvertisementContractErrorCode.BUSINESS_ACCOUNT_NOT_ISSUABLE)
        }
        val email = checkNotNull(contract.email) { "신청 폼 계약에 이메일이 없습니다. contractId=$contractId" }

        val existing = userReader.readByEmail(email)
        val userId = if (existing == null) {
            createAccount(contract, email)
        } else {
            check(existing.role == UserRole.COMPANY) { "로그인 이메일은 비즈니스 회원에만 있습니다. userId=${existing.userId}" }
            if (existing.status != UserStatus.ACTIVE) {
                throw ConflictException(AdvertisementContractErrorCode.BUSINESS_ACCOUNT_MEMBER_NOT_ACTIVE)
            }
            existing.userId
        }

        val linkedContractCount = advertisementContractManager.linkGuestApplications(email, userId)
        return AdminBusinessAccountIssueResult(
            userId = userId,
            created = existing == null,
            linkedContractCount = linkedContractCount,
        )
    }

    /** 같은 이메일이 동시에 가입하면 유니크 제약으로 409 EMAIL_ALREADY_EXISTS다. 다시 누르면 기존 회원 연결로 간다. */
    private fun createAccount(contract: AdvertisementContract, email: String): Long {
        val now = LocalDateTime.now(clock)
        val password = temporaryPasswordIssuer.issue()
        val account = userAppender.appendCompany(
            CompanyAccountAppendDto(email = email, encodedPassword = password.encoded, joinedAt = now),
        )
        companyProfileAppender.append(
            CompanyProfileAppendDto(
                userId = account.userId,
                organizationName = contract.companyName,
                managerName = checkNotNull(contract.managerName) { "신청 폼 계약에 담당자 이름이 없습니다." },
                managerPhone = contract.phoneNumber,
                businessNumberType = contract.businessNumberType,
                businessNumber = contract.businessNumber,
            ),
        )
        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = "$TEMPLATE_CODE:user:${account.userId}",
                channel = NotificationChannel.EMAIL,
                templateCode = TEMPLATE_CODE,
                recipientAddress = email,
                payloadJson = objectMapper.writeValueAsString(
                    mapOf(
                        "managerName" to contract.managerName,
                        "companyName" to contract.companyName,
                        "email" to email,
                        "temporaryPassword" to password.raw,
                    ),
                ),
                scheduledAt = now,
                recipientUserId = account.userId,
            ),
        )
        return account.userId
    }

    private companion object {
        /** 사용자 API의 EmailTemplate.COMPANY_ACCOUNT_ISSUED가 처리하는 적재 계약이다. */
        const val TEMPLATE_CODE = "company_account_issued"
    }
}

data class AdminBusinessAccountIssueResult(
    val userId: Long,
    val created: Boolean,
    val linkedContractCount: Int,
)
