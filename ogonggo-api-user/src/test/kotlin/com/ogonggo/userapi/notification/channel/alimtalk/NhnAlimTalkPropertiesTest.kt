package com.ogonggo.userapi.notification.channel.alimtalk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource

class NhnAlimTalkPropertiesTest {

    @Test
    fun `참조 서버와 같은 NHN 설정 키를 바인딩한다`() {
        val context = AnnotationConfigApplicationContext()
        context.environment.propertySources.addFirst(
            MapPropertySource(
                "test-nhn-properties",
                mapOf(
                    "nhn.appKey" to "test-app-key",
                    "nhn.secretKey" to "test-secret-key",
                    "nhn.sendKey" to "test-send-key",
                    "nhn.templateCode" to "legacy-template-code",
                ),
            ),
        )
        context.register(NhnAlimTalkProperties::class.java)
        context.refresh()

        val properties = context.getBean(NhnAlimTalkProperties::class.java)

        assertEquals("test-app-key", properties.appKey)
        assertEquals("test-secret-key", properties.secretKey)
        assertEquals("test-send-key", properties.sendKey)
        assertEquals("legacy-template-code", properties.templateCode)
        context.close()
    }
}
