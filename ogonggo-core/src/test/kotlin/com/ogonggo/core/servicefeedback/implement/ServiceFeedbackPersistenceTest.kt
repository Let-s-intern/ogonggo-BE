package com.ogonggo.core.servicefeedback.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.servicefeedback.implement.dto.ServiceFeedbackAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(ServiceFeedbackReader::class, ServiceFeedbackAppender::class)
internal class ServiceFeedbackPersistenceTest @Autowired constructor(
    private val serviceFeedbackReader: ServiceFeedbackReader,
    private val serviceFeedbackAppender: ServiceFeedbackAppender,
) {

    @Test
    fun `목록은 최근에 남긴 의견부터 준다`() {
        // given
        val older = serviceFeedbackAppender.append(ServiceFeedbackAppendDto(userId = 1L, satisfaction = "달력이 편해요", improvement = null))
        val anonymous = serviceFeedbackAppender.append(ServiceFeedbackAppendDto(userId = null, satisfaction = null, improvement = "알림이 필요해요"))
        val newer = serviceFeedbackAppender.append(ServiceFeedbackAppendDto(userId = 1L, satisfaction = "검색이 빨라요", improvement = "필터 추가"))

        // when
        val firstPage = serviceFeedbackReader.readPage(page = 0, size = 2)

        // then
        assertEquals(listOf(newer.id, anonymous.id), firstPage.serviceFeedbacks.map { it.id })
        assertEquals(3, firstPage.totalElements)
        assertEquals(listOf(older.id), serviceFeedbackReader.readPage(page = 1, size = 2).serviceFeedbacks.map { it.id })
    }
}
