package com.ogonggo.userapi.notification.channel.alimtalk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.userapi.notification.channel.alimtalk.dto.ReminderAlimTalkParametersDto
import com.ogonggo.userapi.notification.channel.alimtalk.dto.SignUpAlimTalkParametersDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AlimTalkParametersDtoTest {

    private val objectMapper = ObjectMapper()

    @Test
    @DisplayName("스크랩 리마인드 변수는 NHN 템플릿 이름으로 직렬화한다")
    fun `스크랩 리마인드 템플릿 변수를 등록된 이름으로 직렬화한다`() {
        // given
        val parameters = ReminderAlimTalkParametersDto(
            name = "홍길동",
            postingTitle = "백엔드 개발자",
        )

        // when
        val serialized = objectMapper.writeValueAsString(parameters)
        val actual = objectMapper.readValue(serialized, STRING_MAP_TYPE)

        // then
        assertEquals(
            mapOf("name" to "홍길동", "posting-title" to "백엔드 개발자"),
            actual,
        )
    }

    @Test
    @DisplayName("가입 완료 변수는 NHN 템플릿 이름으로 직렬화한다")
    fun `가입 템플릿 변수를 등록된 이름으로 직렬화한다`() {
        // given
        val parameters = SignUpAlimTalkParametersDto(
            name = "홍길동",
            userEmail = "hong@example.com",
            loginType = "렛츠커리어 로그인",
            createDate = "2026-10-06",
        )

        // when
        val serialized = objectMapper.writeValueAsString(parameters)
        val actual = objectMapper.readValue(serialized, STRING_MAP_TYPE)

        // then
        assertEquals(
            mapOf(
                "name" to "홍길동",
                "userEmail" to "hong@example.com",
                "loginType" to "렛츠커리어 로그인",
                "createDate" to "2026-10-06",
            ),
            actual,
        )
    }

    private companion object {
        val STRING_MAP_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
