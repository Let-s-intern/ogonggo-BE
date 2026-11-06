package com.ogonggo.adminapi.banner.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.Optional

class AdminBannerCommandsTest {

    @Test
    fun `등록 본문은 프런트 목과 같은 문구로 처음 걸린 칸 하나만 알린다`() {
        assertMessage("name: 1~50자로 보내 주세요.", create(name = "   "))
        assertMessage("pcImageUrl: https 이미지 주소나 /banners/ 경로를 보내 주세요.", create(pcImageUrl = "http://a.com/x.png"))
        assertMessage("mobileImageUrl: https 이미지 주소나 /banners/ 경로를 보내 주세요.", create(mobileImageUrl = ""))
        assertMessage("linkUrl: http(s):// 주소나 / 로 시작하는 경로를 보내 주세요.", create(linkUrl = "//evil.example.com"))
        assertMessage("altText: 1~100자로 보내 주세요.", create(altText = "a".repeat(101)))
        assertMessage("openInNewTab: 참·거짓 값을 보내 주세요.", create().copy(openInNewTab = null))
        assertMessage("endAt: 노출 끝은 시작보다 뒤여야 합니다.", create(startAt = START, endAt = START))
    }

    @Test
    fun `오공고에 올리지 않았거나 지워진 https 이미지는 받지 않고 웹 기본 소재 경로는 그대로 받는다`() {
        // given
        val references = references(uploadedImages = setOf("https://cdn.example.com/ogonggo/banner/ok.png"))

        // when & then
        assertMessage(
            "pcImageUrl: 오공고에 올린 이미지가 아니거나 지워진 이미지입니다. 다시 올려 주세요.",
            create(pcImageUrl = "https://evil.example.com/x.png"),
            references,
        )
        val settings = create(pcImageUrl = "https://cdn.example.com/ogonggo/banner/ok.png").toDraft()
            .toSettings(BannerPlacement.HOME_TOP, references)
        assertEquals("https://cdn.example.com/ogonggo/banner/ok.png", settings.pcImageUrl)
    }

    @Test
    fun `광고 표시가 켜진 배너는 있는 광고주가 있어야 하고 계약은 그 광고주의 것만 잇는다`() {
        assertMessage("advertiserId: 광고 표시가 켜진 배너는 광고주를 골라 주세요.", create(showAdLabel = true))
        assertMessage("advertiserId: 있는 광고주를 골라 주세요.", create(advertiserId = 999))
        assertMessage("contractId: 있는 계약을 골라 주세요.", create(advertiserId = ADVERTISER_ID, contractId = 999))
        assertMessage("contractId: 고른 광고주의 계약만 이을 수 있습니다.", create(contractId = OTHER_CONTRACT_ID))
        val settings = create(showAdLabel = true, advertiserId = ADVERTISER_ID, contractId = CONTRACT_ID).toDraft()
            .toSettings(BannerPlacement.HOME_TOP, references())
        assertEquals(CONTRACT_ID, settings.contractId)
    }

    @Test
    fun `상세 사이드 자리는 모바일 이미지가 있어야 한다`() {
        assertMessage(
            "mobileImageUrl: 상세 사이드 배너는 모바일 이미지(750×200)가 있어야 합니다.",
            create(mobileImageUrl = null),
            placement = BannerPlacement.BOOTCAMP_DETAIL_SIDE,
        )
        assertNull(create(mobileImageUrl = null).toDraft().toSettings(BannerPlacement.HOME_BOTTOM, references()).mobileImageUrl)
    }

    @Test
    fun `수정은 저장된 값과 합쳐 보고 null을 보낸 칸은 비우며 자리는 바꿀 수 없다`() {
        // given
        val banner = Mockito.mock(Banner::class.java)
        Mockito.`when`(banner.placement).thenReturn(BannerPlacement.HOME_TOP)
        Mockito.`when`(banner.name).thenReturn("저장된 배너")
        Mockito.`when`(banner.pcImageUrl).thenReturn("https://deleted.example.com/old.png")
        Mockito.`when`(banner.mobileImageUrl).thenReturn("/banners/jobs-mo.svg")
        Mockito.`when`(banner.linkUrl).thenReturn("/calendar")
        Mockito.`when`(banner.altText).thenReturn("새 공고")
        Mockito.`when`(banner.startAt).thenReturn(START)
        Mockito.`when`(banner.showAdLabel).thenReturn(true)
        Mockito.`when`(banner.advertiserId).thenReturn(ADVERTISER_ID)
        Mockito.`when`(banner.contractId).thenReturn(null)

        // when
        val settings = AdminBannerUpdateCommand(enabled = false, mobileImageUrl = Optional.empty(), startAt = Optional.empty())
            .mergeInto(banner)
            .toSettings(BannerPlacement.HOME_TOP, references())

        // then
        assertEquals("저장된 배너", settings.name)
        assertEquals("https://deleted.example.com/old.png", settings.pcImageUrl)
        assertNull(settings.mobileImageUrl)
        assertNull(settings.startAt)
        assertEquals(false, settings.enabled)
        val placementChange = assertThrows(AdminConsoleBadRequestException::class.java) {
            AdminBannerUpdateCommand(placement = BannerPlacement.HOME_BOTTOM).mergeInto(banner)
        }
        assertEquals("placement: 자리는 바꿀 수 없습니다. 새로 등록해 주세요.", placementChange.message)
        val endBeforeStoredStart = assertThrows(AdminConsoleBadRequestException::class.java) {
            AdminBannerUpdateCommand(endAt = Optional.of(START.minusDays(1))).mergeInto(banner).toSettings(BannerPlacement.HOME_TOP, references())
        }
        assertEquals("endAt: 노출 끝은 시작보다 뒤여야 합니다.", endBeforeStoredStart.message)
        val adLabelWithoutAdvertiser = assertThrows(AdminConsoleBadRequestException::class.java) {
            AdminBannerUpdateCommand(advertiserId = Optional.empty()).mergeInto(banner).toSettings(BannerPlacement.HOME_TOP, references())
        }
        assertEquals("advertiserId: 광고 표시가 켜진 배너는 광고주를 골라 주세요.", adLabelWithoutAdvertiser.message)
    }

    private fun assertMessage(
        expected: String,
        command: AdminBannerCreateCommand,
        references: AdminBannerReferences = references(),
        placement: BannerPlacement = BannerPlacement.HOME_TOP,
    ) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
            command.toDraft().toSettings(placement, references)
        }
        assertEquals(expected, exception.message)
    }

    private fun references(uploadedImages: Set<String> = emptySet()) = object : AdminBannerReferences {
        override fun isUploadedImage(url: String): Boolean = url in uploadedImages
        override fun advertiserExists(advertiserId: Long): Boolean = advertiserId in setOf(ADVERTISER_ID, OTHER_ADVERTISER_ID)
        override fun contractAdvertiserId(contractId: Long): Long? =
            mapOf(CONTRACT_ID to ADVERTISER_ID, OTHER_CONTRACT_ID to OTHER_ADVERTISER_ID)[contractId]
    }

    private fun create(
        name: String? = "홈 배너",
        pcImageUrl: String? = "/banners/jobs-pc.svg",
        mobileImageUrl: String? = "/banners/jobs-mo.svg",
        linkUrl: String? = "/calendar",
        altText: String? = "새 공고",
        showAdLabel: Boolean = false,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        advertiserId: Long? = null,
        contractId: Long? = null,
    ) = AdminBannerCreateCommand(
        placement = BannerPlacement.HOME_TOP,
        name = name,
        pcImageUrl = pcImageUrl,
        mobileImageUrl = mobileImageUrl,
        linkUrl = linkUrl,
        openInNewTab = false,
        altText = altText,
        showAdLabel = showAdLabel,
        startAt = startAt,
        endAt = endAt,
        enabled = true,
        advertiserId = advertiserId,
        contractId = contractId,
    )

    companion object {
        private const val ADVERTISER_ID = 10L
        private const val OTHER_ADVERTISER_ID = 11L
        private const val CONTRACT_ID = 20L
        private const val OTHER_CONTRACT_ID = 21L
        private val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
