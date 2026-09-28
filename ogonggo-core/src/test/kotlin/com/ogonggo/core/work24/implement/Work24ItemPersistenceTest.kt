package com.ogonggo.core.work24.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.implement.dto.Work24ItemAppendDto
import com.ogonggo.core.work24.persistence.Work24ItemJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** 저장기는 바깥 트랜잭션 없이 불리므로 테스트 트랜잭션도 열지 않는다. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(Work24ItemAppender::class)
internal class Work24ItemPersistenceTest @Autowired constructor(
    private val work24ItemAppender: Work24ItemAppender,
    private val work24ItemRepository: Work24ItemJpaRepository,
) {

    @Test
    fun `이미 받은 항목은 내용이 달라도 건너뛰고 새 항목만 저장한다`() {
        // given
        work24ItemAppender.appendNew(Work24Api.RECRUITMENTS, listOf(item("K1", "처음")))

        // when
        val appendedCount = work24ItemAppender.appendNew(
            Work24Api.RECRUITMENTS,
            listOf(item("K1", "바뀐 내용"), item("K2", "새 공고"), item("K2", "같은 페이지 중복")),
        )

        // then
        assertEquals(1, appendedCount)
        val items = work24ItemRepository.findAll().filter { it.api == Work24Api.RECRUITMENTS }.associateBy { it.externalId }
        assertEquals(setOf("K1", "K2"), items.keys)
        assertEquals("""{"v":"처음"}""", items.getValue("K1").payload)
    }

    @Test
    fun `식별값은 API 안에서만 겹치지 않으면 된다`() {
        work24ItemAppender.appendNew(Work24Api.SMALL_GIANT_COMPANIES, listOf(item("123", "강소기업")))

        val appendedCount = work24ItemAppender.appendNew(
            Work24Api.YOUTH_FRIENDLY_SMALL_GIANT_COMPANIES,
            listOf(item("123", "청년친화강소기업")),
        )

        assertEquals(1, appendedCount)
    }

    private fun item(externalId: String, value: String) =
        Work24ItemAppendDto(externalId = externalId, payload = """{"v":"$value"}""")
}
