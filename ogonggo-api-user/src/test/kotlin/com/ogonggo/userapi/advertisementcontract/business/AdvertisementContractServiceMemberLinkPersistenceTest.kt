package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractNotificationAppender
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(UserAppender::class, UserReader::class, AdvertisementContractAppender::class)
internal class AdvertisementContractServiceMemberLinkPersistenceTest @Autowired constructor(
    private val userAppender: UserAppender,
    private val userReader: UserReader,
    private val advertisementContractAppender: AdvertisementContractAppender,
    private val entityManager: TestEntityManager,
) {

    @Test
    fun `실제 회원 행으로 접수한 계약을 다시 읽으면 회원 ID가 연결돼 있다`() {
        // given
        val member = userAppender.append(UserAppendDto(letsCareerUserId = 4821L, joinedAt = NOW))
        val service = AdvertisementContractService(
            userReader = userReader,
            advertisementContractAppender = advertisementContractAppender,
            advertisementContractNotificationAppender = Mockito.mock(AdvertisementContractNotificationAppender::class.java),
        )

        // when
        val returnedContractId = service.createContract(member.userId, COMMAND)
        entityManager.flush()
        entityManager.clear()
        val persistedContract = entityManager.find(AdvertisementContract::class.java, returnedContractId)

        // then
        assertEquals(returnedContractId, persistedContract.id)
        assertEquals(member.userId, persistedContract.userId)
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 7, 12, 0)
        val COMMAND = CreateAdvertisementContractCommand(
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            budgetAmount = null,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = "12월 첫째 주 진행 희망",
            advertisementRequest = "채용 안내 문구 검토 요청",
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
    }
}
