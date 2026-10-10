package com.ogonggo.userapi.notification.channel.alimtalk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AlimTalkRecipientNumberPolicyTest {

    @Test
    @DisplayName("010 국내 휴대폰 번호의 하이픈만 제거해 NHN 수신번호로 만든다")
    fun `허용된 010 번호만 하이픈 없이 반환한다`() {
        // given
        val formatted = "010-1234-5678"
        val unformatted = "01012345678"

        // when
        val normalizedFormatted = AlimTalkRecipientNumberPolicy.normalize(formatted)
        val normalizedUnformatted = AlimTalkRecipientNumberPolicy.normalize(unformatted)
        val rejected = AlimTalkRecipientNumberPolicy.normalize("011-1234-5678")

        // then
        assertEquals("01012345678", normalizedFormatted)
        assertEquals("01012345678", normalizedUnformatted)
        assertNull(rejected)
    }
}
