package com.ogonggo.adminapi.advertiser.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserContact
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.implement.AdvertiserAdCountReader
import com.ogonggo.core.advertiser.implement.AdvertiserAppender
import com.ogonggo.core.advertiser.implement.AdvertiserManager
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.banner.implement.BannerReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class AdminAdvertiserServiceTest {

    private val advertiserReader = Mockito.mock(AdvertiserReader::class.java)
    private val advertiserAppender = Mockito.mock(AdvertiserAppender::class.java)
    private val advertiserManager = Mockito.mock(AdvertiserManager::class.java)
    private val advertiserContractReader = Mockito.mock(AdvertiserContractReader::class.java)
    private val service = AdminAdvertiserService(
        advertiserReader = advertiserReader,
        advertiserAppender = advertiserAppender,
        advertiserManager = advertiserManager,
        advertiserContractReader = advertiserContractReader,
        advertiserAdCountReader = AdvertiserAdCountReader(Mockito.mock(BannerReader::class.java)),
        clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL),
    )

    @Test
    fun `목록은 진행 중 계약, 계약만 있는 곳, 계약 없는 곳 순이고 그 안에서 계약 끝이 늦은 곳, 이름순이며 정렬한 뒤 50건을 자른다`() {
        // given
        val noContractB = advertiser(1, "나다")
        val noContractA = advertiser(2, "가나")
        val endedLate = advertiser(3, "끝난 늦은 계약")
        val endedEarly = advertiser(4, "끝난 이른 계약")
        val active = advertiser(5, "진행 중")
        val filler = (100L until 150L).map { advertiser(it, "채움$it") }
        Mockito.`when`(advertiserReader.readAll(AdvertiserSearchCondition()))
            .thenReturn(listOf(noContractB, noContractA, endedLate, endedEarly, active) + filler)
        val contracts = mapOf(
            3L to listOf(contract(end = NOW.minusDays(1), active = false)),
            4L to listOf(contract(end = NOW.minusDays(10), active = false)),
            5L to listOf(contract(end = NOW.minusDays(30), active = false), contract(end = NOW.plusDays(1), active = true)),
        )
        Mockito.`when`(advertiserContractReader.readAllByAdvertisers(Mockito.anyList())).thenReturn(contracts)

        // when
        val result = service.getAdvertisers(AdvertiserSearchCondition())

        // then
        assertEquals(50, result.size)
        assertEquals(listOf(5L, 3L, 4L, 2L, 1L), result.take(5).map { it.id })
        assertEquals(2, result.first().contractTotals.contractCount)
        assertEquals(1, result.first().contractTotals.activeContractCount)
        Mockito.verify(advertiserManager).syncSources()
    }

    @Test
    fun `비회원 광고주 추가는 이름을 다듬어 1~50자인지 보고 같은 이름이 있으면 목록에서 고르라고 알린다`() {
        assertMessage("name: 1~50자로 보내 주세요.") { service.createAdvertiser(AdminAdvertiserCreateCommand(name = "  ")) }
        assertMessage("name: 1~50자로 보내 주세요.") { service.createAdvertiser(AdminAdvertiserCreateCommand(name = "가".repeat(51))) }

        Mockito.`when`(advertiserReader.existsSameName(anyString(), Mockito.isNull())).thenReturn(true)
        assertMessage("name: 같은 이름의 광고주가 이미 있습니다. 목록에서 골라 주세요.") {
            service.createAdvertiser(AdminAdvertiserCreateCommand(name = " 새싹 "))
        }
        Mockito.verify(advertiserReader).existsSameName("새싹", null)
    }

    @Test
    fun `메일은 형식만 본다`() {
        assertMessage("contactEmail: 메일 주소 형식으로 보내 주세요.") {
            service.createAdvertiser(AdminAdvertiserCreateCommand(name = "새싹", contactEmail = "sprout"))
        }
        Mockito.verifyNoInteractions(advertiserAppender)
    }

    @Test
    fun `회원 광고주는 고칠 수 없다고 알린다`() {
        // given
        val member = Advertiser.member(7, "오공고", AdvertiserContact())
        ReflectionTestUtils.setField(member, "id", 9L)
        Mockito.`when`(advertiserReader.readForUpdate(9)).thenReturn(member)

        // then
        assertMessage("회원 광고주는 비즈니스 회원 정보를 따릅니다. 회원 정보에서 바꿔 주세요.") {
            service.updateAdvertiser(9, AdminAdvertiserUpdateCommand(name = "다른 이름"))
        }
        Mockito.verifyNoInteractions(advertiserManager)
    }

    @Test
    fun `수정할 때 같은 이름은 자기 자신을 빼고 본다`() {
        // given
        val advertiser = advertiser(9, "새싹")
        Mockito.`when`(advertiserReader.readForUpdate(9)).thenReturn(advertiser)
        Mockito.`when`(advertiserReader.existsSameName("그로우랩", 9)).thenReturn(true)

        // then
        assertMessage("name: 같은 이름의 광고주가 이미 있습니다.") {
            service.updateAdvertiser(9, AdminAdvertiserUpdateCommand(name = "그로우랩"))
        }
    }

    private fun assertMessage(message: String, action: () -> Unit) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java, action)
        assertEquals(message, exception.message)
    }

    private fun advertiser(id: Long, name: String): Advertiser =
        Advertiser.manual(name, AdvertiserContact()).also { ReflectionTestUtils.setField(it, "id", id) }

    private fun contract(end: LocalDateTime, active: Boolean): AdvertiserContract {
        val contract = Mockito.mock(AdvertiserContract::class.java)
        Mockito.`when`(contract.endAt).thenReturn(end)
        Mockito.`when`(contract.isActive(NOW)).thenReturn(active)
        Mockito.`when`(contract.amount).thenReturn(1_000_000)
        Mockito.`when`(contract.vatIncluded).thenReturn(true)
        return contract
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
