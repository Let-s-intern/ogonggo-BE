package com.ogonggo.core.companycontentpriority.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
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
) {

    @Test
    fun `바꾼 적이 없으면 켜져 있고 바꾸면 마지막에 바꾼 값이다`() {
        assertTrue(priorityReader.isEnabled())

        priorityManager.replace(false)
        assertFalse(priorityReader.isEnabled())

        priorityManager.replace(true)
        priorityManager.replace(false)
        assertFalse(priorityReader.isEnabled())
    }
}
