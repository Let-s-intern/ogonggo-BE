package com.ogonggo.core.work24jobfield.implement

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.work24jobfield.persistence.Work24JobFieldJpaRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(Work24JobFieldReader::class, Work24JobFieldManager::class)
internal class Work24JobFieldPersistenceTest @Autowired constructor(
    private val work24JobFieldReader: Work24JobFieldReader,
    private val work24JobFieldManager: Work24JobFieldManager,
    private val work24JobFieldRepository: Work24JobFieldJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `바꾼 적이 없으면 설정 전에 받던 다섯 직군만 켜져 있고 모든 직군을 직군 순서로 준다`() {
        val settings = work24JobFieldReader.findAll()

        assertEquals(JobField.entries, settings.keys.toList())
        assertEquals(
            setOf(
                JobField.IT_DEVELOPMENT,
                JobField.PLANNING_STRATEGY,
                JobField.MARKETING_ADVERTISING,
                JobField.SALES,
                JobField.HR_GENERAL_AFFAIRS,
            ),
            work24JobFieldReader.findEnabled(),
        )
    }

    @Test
    fun `직군을 끄거나 켜면 그 직군만 바뀌고 같은 직군은 한 행만 남는다`() {
        // when
        work24JobFieldManager.replace(JobField.SALES, false)
        work24JobFieldManager.replace(JobField.GAME, true)
        work24JobFieldManager.replace(JobField.SALES, true)
        work24JobFieldManager.replace(JobField.SALES, false)
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(
            setOf(
                JobField.IT_DEVELOPMENT,
                JobField.GAME,
                JobField.PLANNING_STRATEGY,
                JobField.MARKETING_ADVERTISING,
                JobField.HR_GENERAL_AFFAIRS,
            ),
            work24JobFieldReader.findEnabled(),
        )
        assertEquals(2L, work24JobFieldRepository.count())
    }

    @Test
    fun `운영 DB에 SQL로 넣은 행을 읽는다`() {
        entityManager.createNativeQuery(
            """
            insert into work24_job_fields (job_field, enabled, created_at, updated_at)
            values ('SALES', false, current_timestamp, current_timestamp)
            """,
        ).executeUpdate()

        assertEquals(false, work24JobFieldReader.findAll().getValue(JobField.SALES))
    }
}
