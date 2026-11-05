package com.ogonggo.core.companycontentpriority.implement

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ogonggo.core.companycontentpriority.persistence.CompanyContentPriorityJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.dao.InvalidDataAccessResourceUsageException

internal class CompanyContentPriorityReaderTest {

    private val repository = Mockito.mock(CompanyContentPriorityJpaRepository::class.java)
    private val reader = CompanyContentPriorityReader(repository)

    /** 운영 DB에 테이블을 만들기 전에 배포되면 Hibernate가 SQL 문법 오류를 이 예외로 바꿔 던진다. */
    private val missingTable = InvalidDataAccessResourceUsageException("Table 'company_content_priorities' doesn't exist")

    @Test
    fun `공개 목록은 설정을 읽지 못하면 켜짐으로 보고 오류 로그를 남긴다`() {
        Mockito.`when`(repository.findById(ArgumentMatchers.anyLong())).thenThrow(missingTable)
        val logger = LoggerFactory.getLogger(CompanyContentPriorityReader::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply {
            context = logger.loggerContext
            start()
        }
        logger.addAppender(logAppender)

        try {
            assertTrue(reader.isEnabledOrDefault())

            val event = logAppender.list.single()
            assertEquals(Level.ERROR, event.level)
            assertSame(missingTable, (event.throwableProxy as ch.qos.logback.classic.spi.ThrowableProxy).throwable)
        } finally {
            logger.detachAppender(logAppender)
            logAppender.stop()
        }
    }

    @Test
    fun `관리자 콘솔은 설정을 읽지 못하면 예외를 그대로 받는다`() {
        Mockito.`when`(repository.findById(ArgumentMatchers.anyLong())).thenThrow(missingTable)

        val thrown = assertThrows<InvalidDataAccessResourceUsageException> { reader.isEnabled() }

        assertSame(missingTable, thrown)
    }
}
