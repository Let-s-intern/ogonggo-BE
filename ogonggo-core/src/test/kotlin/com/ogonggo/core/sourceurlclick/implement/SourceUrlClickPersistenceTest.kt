package com.ogonggo.core.sourceurlclick.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.sourceurlclick.domain.SourceUrlClickTargetType
import com.ogonggo.core.sourceurlclick.persistence.SourceUrlClickJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(SourceUrlClickAppender::class)
internal class SourceUrlClickPersistenceTest @Autowired constructor(
    private val sourceUrlClickAppender: SourceUrlClickAppender,
    private val sourceUrlClickRepository: SourceUrlClickJpaRepository,
) {

    @Test
    fun `외부 링크 이동은 사용자와 콘텐츠마다 한 행만 남는다`() {
        sourceUrlClickAppender.append(SourceUrlClickTargetType.JOB, CONTENT_ID, USER_ID)
        sourceUrlClickAppender.append(SourceUrlClickTargetType.JOB, CONTENT_ID, USER_ID)
        sourceUrlClickAppender.append(SourceUrlClickTargetType.JOB, CONTENT_ID, OTHER_USER_ID)
        sourceUrlClickAppender.append(SourceUrlClickTargetType.JOB, OTHER_CONTENT_ID, USER_ID)

        assertEquals(3L, sourceUrlClickRepository.count())
    }

    @Test
    fun `식별자가 같아도 채용공고와 부트캠프는 다른 기록이다`() {
        sourceUrlClickAppender.append(SourceUrlClickTargetType.JOB, CONTENT_ID, USER_ID)
        sourceUrlClickAppender.append(SourceUrlClickTargetType.BOOTCAMP, CONTENT_ID, USER_ID)

        assertEquals(2L, sourceUrlClickRepository.count())
        assertEquals(
            true,
            sourceUrlClickRepository.existsByTargetTypeAndTargetIdAndUserId(SourceUrlClickTargetType.BOOTCAMP, CONTENT_ID, USER_ID),
        )
        assertEquals(
            false,
            sourceUrlClickRepository.existsByTargetTypeAndTargetIdAndUserId(SourceUrlClickTargetType.BOOTCAMP, OTHER_CONTENT_ID, USER_ID),
        )
    }

    private companion object {
        const val USER_ID = 7L
        const val OTHER_USER_ID = 8L
        const val CONTENT_ID = 11L
        const val OTHER_CONTENT_ID = 12L
    }
}
