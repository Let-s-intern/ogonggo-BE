package com.ogonggo.logging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.logging.LoggingInitializationContext
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mock.env.MockEnvironment

@ExtendWith(OutputCaptureExtension::class)
class LogbackConfigurationTest {

    private val loggingSystem = LoggingSystem.get(javaClass.classLoader)

    @AfterEach
    fun tearDown() {
        System.clearProperty(LOG_FORMAT)
        MDC.clear()
        loggingSystem.cleanUp()
    }

    @Test
    fun `json 형식은 한 줄에 JSON 하나로 남기고 MDC의 트레이스 식별자를 필드로 내보낸다`(output: CapturedOutput) {
        // given
        initialize(format = "json")
        MDC.put("dd.trace_id", "1234567890")

        // when
        LoggerFactory.getLogger("ogonggo.test").info("공고 목록 조회")

        // then
        val line = output.out.lines().last { it.contains("공고 목록 조회") }
        val fields = jacksonObjectMapper().readTree(line)
        assertThat(fields["message"].asText()).isEqualTo("공고 목록 조회")
        assertThat(fields["level"].asText()).isEqualTo("INFO")
        assertThat(fields["dd.trace_id"].asText()).isEqualTo("1234567890")
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["jsno", "JSON"])
    fun `json이 아니거나 비어 있으면 로그를 버리지 않고 텍스트로 남긴다`(format: String?, output: CapturedOutput) {
        // given
        initialize(format = format)

        // when
        LoggerFactory.getLogger("ogonggo.test").info("공고 목록 조회")

        // then
        val line = output.out.lines().last { it.contains("공고 목록 조회") }
        assertThat(line).doesNotStartWith("{")
        assertThat(line).contains("INFO")
    }

    private fun initialize(format: String?) {
        format?.let { System.setProperty(LOG_FORMAT, it) }
        loggingSystem.beforeInitialize()
        loggingSystem.initialize(
            LoggingInitializationContext(MockEnvironment()),
            "classpath:logback-spring.xml",
            null,
        )
    }

    private companion object {
        const val LOG_FORMAT = "LOG_FORMAT"
    }
}
