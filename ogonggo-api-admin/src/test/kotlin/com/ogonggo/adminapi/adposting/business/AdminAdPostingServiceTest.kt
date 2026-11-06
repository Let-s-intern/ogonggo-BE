package com.ogonggo.adminapi.adposting.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.adposting.implement.AdPostingAppender
import com.ogonggo.core.adposting.implement.AdPostingManager
import com.ogonggo.core.adposting.implement.AdPostingReader
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserContact
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class AdminAdPostingServiceTest {

    private val adPostingReader = Mockito.mock(AdPostingReader::class.java)
    private val adPostingAppender = Mockito.mock(AdPostingAppender::class.java)
    private val adPostingManager = Mockito.mock(AdPostingManager::class.java)
    private val adPostingTargetReader = Mockito.mock(AdPostingTargetReader::class.java)
    private val advertiserReader = Mockito.mock(AdvertiserReader::class.java)
    private val advertiserContractReader = Mockito.mock(AdvertiserContractReader::class.java)
    private val service = AdminAdPostingService(
        adPostingReader = adPostingReader,
        adPostingAppender = adPostingAppender,
        adPostingManager = adPostingManager,
        adPostingTargetReader = adPostingTargetReader,
        advertiserReader = advertiserReader,
        advertiserContractReader = advertiserContractReader,
        clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL),
    )

    @Test
    fun `자리와 광고주가 모두 없으면 자리 문구로 거절한다`() {
        assertMessage(PLACEMENT_MESSAGE) { service.getAdPostings(null, null) }
        assertMessage(PLACEMENT_MESSAGE) { service.createAdPosting(create(placement = null)) }
        assertMessage(PLACEMENT_MESSAGE) { service.replaceOrder(AdminAdPostingOrderCommand(null, listOf(1))) }
    }

    @Test
    fun `없는 대상은 자리가 정한 대상 이름으로 알리고 같은 자리의 같은 대상은 거절한다`() {
        // given
        Mockito.`when`(adPostingTargetReader.exists(AdPostingTargetType.JOB, TARGET_ID)).thenReturn(true)
        Mockito.`when`(adPostingReader.existsInPlacement(AdPostingPlacement.JOB_LIST, TARGET_ID)).thenReturn(true)

        // then
        assertMessage("targetId: 있는 부트캠프 id 를 보내 주세요.") {
            service.createAdPosting(create(placement = AdPostingPlacement.BOOTCAMP_LIST))
        }
        assertMessage("targetId: 있는 채용공고 id 를 보내 주세요.") { service.createAdPosting(create(targetId = null)) }
        assertMessage("targetId: 이 자리에 이미 등록된 대상입니다.") { service.createAdPosting(create()) }
        Mockito.verifyNoInteractions(adPostingAppender)
    }

    @Test
    fun `등록은 광고주, 계약, 켜기, 기간 순으로 보고 처음 걸린 칸 하나만 알린다`() {
        // given
        Mockito.`when`(adPostingTargetReader.exists(AdPostingTargetType.JOB, TARGET_ID)).thenReturn(true)
        Mockito.`when`(advertiserReader.readAllByIds(listOf(ADVERTISER_ID))).thenReturn(listOf(advertiser(ADVERTISER_ID)))
        val otherContract = contract(OTHER_ADVERTISER_ID)
        Mockito.`when`(advertiserContractReader.readAllByIds(listOf(OTHER_CONTRACT_ID))).thenReturn(listOf(otherContract))

        // then
        assertMessage("advertiserId: 광고주를 골라 주세요.") { service.createAdPosting(create(advertiserId = 999)) }
        assertMessage("contractId: 있는 계약을 골라 주세요.") { service.createAdPosting(create(contractId = 404, enabled = null)) }
        assertMessage("contractId: 고른 광고주의 계약만 이을 수 있습니다.") {
            service.createAdPosting(create(contractId = OTHER_CONTRACT_ID))
        }
        assertMessage("enabled: 참·거짓 값을 보내 주세요.") { service.createAdPosting(create(enabled = null, startAt = NOW, endAt = NOW)) }
        assertMessage("endAt: 노출 끝은 시작보다 뒤여야 합니다.") { service.createAdPosting(create(startAt = NOW, endAt = NOW)) }
        Mockito.verifyNoInteractions(adPostingAppender)
    }

    @Test
    fun `수정은 보낸 기간을 저장된 값과 합쳐 보고 광고주를 바꾸면 남은 계약도 그 광고주의 것이어야 한다`() {
        // given
        val adPosting = Mockito.mock(AdPosting::class.java)
        Mockito.`when`(adPosting.advertiserId).thenReturn(ADVERTISER_ID)
        Mockito.`when`(adPosting.contractId).thenReturn(CONTRACT_ID)
        Mockito.`when`(adPosting.startAt).thenReturn(NOW)
        Mockito.`when`(adPostingReader.readForUpdate(AD_POSTING_ID)).thenReturn(adPosting)
        Mockito.`when`(advertiserReader.readAllByIds(listOf(OTHER_ADVERTISER_ID))).thenReturn(listOf(advertiser(OTHER_ADVERTISER_ID)))
        val contract = contract(ADVERTISER_ID)
        Mockito.`when`(advertiserContractReader.readAllByIds(listOf(CONTRACT_ID))).thenReturn(listOf(contract))

        // then
        assertMessage("endAt: 노출 끝은 시작보다 뒤여야 합니다.") {
            service.updateAdPosting(AD_POSTING_ID, AdminAdPostingUpdateCommand(endAt = Optional.of(NOW.minusDays(1))))
        }
        assertMessage("contractId: 고른 광고주의 계약만 이을 수 있습니다.") {
            service.updateAdPosting(AD_POSTING_ID, AdminAdPostingUpdateCommand(advertiserId = OTHER_ADVERTISER_ID))
        }
        Mockito.verifyNoInteractions(adPostingManager)
    }

    @Test
    fun `순서 목록이 그 자리의 광고 공고와 다르면 거절한다`() {
        // given
        val adPostings = listOf(1L, 2L).map { id -> Mockito.mock(AdPosting::class.java).also { Mockito.`when`(it.id).thenReturn(id) } }
        Mockito.`when`(adPostingReader.readAllByPlacementForUpdate(AdPostingPlacement.JOB_LIST)).thenReturn(adPostings)
        val message = "adPostingIds: 그 자리의 광고 id 를 빠짐없이 한 번씩 보내 주세요."

        // then
        listOf(listOf(1L), listOf(1L, 1L), listOf(1L, 3L), listOf(2L, 1L, 1L), null).forEach { ids ->
            assertMessage(message) { service.replaceOrder(AdminAdPostingOrderCommand(AdPostingPlacement.JOB_LIST, ids)) }
        }
        Mockito.verifyNoInteractions(adPostingManager)
    }

    private fun create(
        placement: AdPostingPlacement? = AdPostingPlacement.JOB_LIST,
        targetId: Long? = TARGET_ID,
        advertiserId: Long? = ADVERTISER_ID,
        contractId: Long? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        enabled: Boolean? = true,
    ) = AdminAdPostingCreateCommand(placement, targetId, advertiserId, contractId, startAt, endAt, enabled)

    private fun advertiser(id: Long): Advertiser =
        Advertiser.manual("광고주$id", AdvertiserContact()).also { ReflectionTestUtils.setField(it, "id", id) }

    private fun contract(advertiserId: Long): AdvertiserContract =
        Mockito.mock(AdvertiserContract::class.java).also { Mockito.`when`(it.advertiserId).thenReturn(advertiserId) }

    private fun assertMessage(message: String, action: () -> Unit) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java, action)
        assertEquals(message, exception.message)
    }

    private companion object {
        const val AD_POSTING_ID = 5L
        const val TARGET_ID = 13L
        const val ADVERTISER_ID = 3L
        const val OTHER_ADVERTISER_ID = 4L
        const val CONTRACT_ID = 21L
        const val OTHER_CONTRACT_ID = 22L
        const val PLACEMENT_MESSAGE =
            "placement: JOB_LIST, JOB_PICK, BOOTCAMP_LIST, JOB_DETAIL_SIDE, CALENDAR, MYPAGE_SCRAP 중 하나를 보내 주세요."
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
