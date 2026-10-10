package com.ogonggo.core.schedule.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.schedule.domain.ScheduledJob
import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import com.ogonggo.core.schedule.persistence.ScheduledJobJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 등록기는 트랜잭션 밖에서 DB를 읽고 쓰므로 테스트 트랜잭션을 열지 않는다. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(ScheduledJobReader::class, ScheduledJobAppender::class)
internal class ScheduledJobRegistrarTest @Autowired constructor(
    private val scheduledJobReader: ScheduledJobReader,
    private val scheduledJobAppender: ScheduledJobAppender,
    private val scheduledJobRepository: ScheduledJobJpaRepository,
) {

    private val registrars = mutableListOf<ScheduledJobRegistrar>()

    @AfterEach
    fun tearDown() {
        registrars.forEach(ScheduledJobRegistrar::destroy)
        scheduledJobRepository.deleteAllInBatch()
    }

    @Test
    fun `기동할 때 없는 작업만 기본값으로 만들고 DB에서 바꾼 값은 지킨다`() {
        // given
        scheduledJobRepository.saveAndFlush(ScheduledJob("existing", "0 0 5 * * *", false, "운영자가 바꾼 작업"))

        // when
        val registrar = registrar(definition("existing", "0 0 4 * * *"), definition("new", "0 0 3 * * *"))
        registrar.start()

        // then
        val existing = scheduledJobRepository.findByName("existing")!!
        assertEquals("0 0 5 * * *", existing.cron)
        assertFalse(existing.enabled)
        assertEquals("0 0 5 * * *", registrar.scheduledCron("existing"))
        assertTrue(scheduledJobRepository.findByName("new")!!.enabled)
        assertEquals("0 0 3 * * *", registrar.scheduledCron("new"))
    }

    @Test
    fun `DB의 cron이 바뀌면 다시 예약하고 잘못된 cron이면 기존 예약을 유지한다`() {
        // given
        val registrar = registrar(definition("job", "0 0 4 * * *"))
        registrar.start()

        // when
        updateCron("job", "0 30 2 * * *")
        registrar.refresh()

        // then
        assertEquals("0 30 2 * * *", registrar.scheduledCron("job"))

        updateCron("job", "매일 새벽")
        registrar.refresh()
        assertEquals("0 30 2 * * *", registrar.scheduledCron("job"))
    }

    @Test
    fun `잘못된 cron으로 기동하면 코드의 기본 cron으로 예약한다`() {
        scheduledJobRepository.saveAndFlush(ScheduledJob("job", "잘못된 값", true, "작업"))

        val registrar = registrar(definition("job", "0 0 4 * * *"))
        registrar.start()

        assertEquals("0 0 4 * * *", registrar.scheduledCron("job"))
    }

    @Test
    fun `꺼진 작업은 예약 시각이 와도 실행하지 않는다`() {
        // given
        scheduledJobRepository.saveAndFlush(ScheduledJob("disabled", EVERY_SECOND, false, "꺼진 작업"))
        val enabledRuns = CountDownLatch(1)
        val disabledRuns = CountDownLatch(1)

        // when
        registrar(
            definition("enabled", EVERY_SECOND) { enabledRuns.countDown() },
            definition("disabled", EVERY_SECOND) { disabledRuns.countDown() },
        ).start()

        // then
        assertTrue(enabledRuns.await(3, TimeUnit.SECONDS), "켜진 작업이 실행되지 않았습니다.")
        assertFalse(disabledRuns.await(1, TimeUnit.SECONDS), "꺼진 작업이 실행되었습니다.")
    }

    private fun registrar(vararg definitions: ScheduledJobDefinition): ScheduledJobRegistrar {
        val beanFactory = DefaultListableBeanFactory()
        definitions.forEach { beanFactory.registerSingleton(it.name, it) }
        return ScheduledJobRegistrar(
            beanFactory.getBeanProvider(ScheduledJobDefinition::class.java),
            scheduledJobReader,
            scheduledJobAppender,
        ).also(registrars::add)
    }

    private fun definition(name: String, cron: String, action: () -> Unit = {}) =
        ScheduledJobDefinition(name = name, defaultCron = cron, description = "$name 작업", action = action)

    private fun updateCron(name: String, cron: String) {
        val job = scheduledJobRepository.findByName(name)!!
        job.cron = cron
        scheduledJobRepository.saveAndFlush(job)
    }

    private companion object {
        const val EVERY_SECOND = "* * * * * *"
    }
}
