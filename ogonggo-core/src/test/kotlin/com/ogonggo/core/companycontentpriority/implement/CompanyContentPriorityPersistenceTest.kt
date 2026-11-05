package com.ogonggo.core.companycontentpriority.implement

import com.ogonggo.core.companycontentpriority.domain.CompanyContentPriority
import com.ogonggo.core.companycontentpriority.persistence.CompanyContentPriorityJpaRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(CompanyContentPriorityReader::class, CompanyContentPriorityManager::class)
internal class CompanyContentPriorityPersistenceTest @Autowired constructor(
    private val priorityReader: CompanyContentPriorityReader,
    private val priorityManager: CompanyContentPriorityManager,
    private val priorityRepository: CompanyContentPriorityJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `바꾼 적이 없으면 켜져 있고 바꾸면 마지막에 바꾼 값이다`() {
        assertTrue(priorityReader.isEnabled())

        priorityManager.replace(false)
        assertFalse(priorityReader.isEnabled())

        priorityManager.replace(true)
        priorityManager.replace(false)
        assertFalse(priorityReader.isEnabled())
        assertEquals(1L, priorityRepository.count())
    }

    @Test
    fun `행이 없을 때 두 요청이 각자 행을 만들어도 한 행만 남고 나중 값을 읽는다`() {
        priorityRepository.saveAndFlush(CompanyContentPriority(true))
        priorityRepository.saveAndFlush(CompanyContentPriority(false))
        entityManager.clear()

        assertEquals(1L, priorityRepository.count())
        assertFalse(priorityReader.isEnabled())
    }

    @Test
    fun `운영 DB에 미리 넣어 둔 기본 행을 읽고 그 행을 바꾼다`() {
        entityManager.createNativeQuery(
            """
            insert into company_content_priorities (id, enabled, created_at, updated_at)
            values (1, false, current_timestamp, current_timestamp)
            """,
        ).executeUpdate()

        assertFalse(priorityReader.isEnabled())

        priorityManager.replace(true)
        entityManager.flush()
        entityManager.clear()

        assertTrue(priorityReader.isEnabled())
        assertEquals(1L, priorityRepository.count())
    }
}
