package com.ogonggo.core.advertiser.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdvertiserDomainTest {

    @Test
    fun `비회원 광고주만 공백과 대소문자를 뺀 이름 비교 값을 가진다`() {
        // given
        val manual = Advertiser.manual(" Grow  Lab ", AdvertiserContact())
        val member = Advertiser.member(7, "Grow Lab", AdvertiserContact())

        // then
        assertEquals("growlab", manual.nameKey)
        assertEquals(AdvertiserType.NON_MEMBER, manual.type)
        assertNull(member.nameKey)
        assertEquals(AdvertiserType.MEMBER, member.type)
    }

    @Test
    fun `비회원 광고주 수정은 보낸 칸만 바꾸고 빈 문자열은 그 칸을 지운다`() {
        // given
        val advertiser = Advertiser.manual("그로우랩", AdvertiserContact(name = "정하늘", email = "a@growlab.kr", phone = "010"))

        // when
        advertiser.edit(name = "그로우 랩스", contactName = null, contactEmail = "", contactPhone = "02-123")

        // then
        assertEquals("그로우 랩스", advertiser.name)
        assertEquals("그로우랩스", advertiser.nameKey)
        assertEquals("정하늘", advertiser.contactName)
        assertNull(advertiser.contactEmail)
        assertEquals("02-123", advertiser.contactPhone)
    }

    @Test
    fun `회원 광고주는 운영자가 고칠 수 없고 회원 정보를 따른다`() {
        // given
        val advertiser = Advertiser.member(7, "오공고", AdvertiserContact(name = "김담당"))

        // when
        advertiser.followMember("오공고랩스", AdvertiserContact(name = "이담당", email = "lee@ogonggo.kr"))

        // then
        assertEquals("오공고랩스", advertiser.name)
        assertEquals("이담당", advertiser.contactName)
        assertEquals("lee@ogonggo.kr", advertiser.contactEmail)
        assertThrows(IllegalStateException::class.java) { advertiser.edit("다른 이름", null, null, null) }
    }

    @Test
    fun `광고 문의는 더 최근 문의만 잇는다`() {
        // given
        val advertiser = Advertiser.fromInquiry(5, "그로우랩", AdvertiserContact())

        // when
        advertiser.linkInquiry(9)
        advertiser.linkInquiry(3)

        // then
        assertEquals(9, advertiser.adInquiryId)
    }
}
