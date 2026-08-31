package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampAppendCommand
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.BootcampUpdateCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminBootcampService(
    private val bootcampReader: BootcampReader,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val clock: Clock,
) {

    @Transactional
    fun create(command: BootcampAppendCommand): Long {
        val bootcamp = bootcampAppender.append(command)
        return checkNotNull(bootcamp.id) { "저장된 부트캠프 식별자가 없습니다." }
    }

    @Transactional
    fun update(bootcampId: Long, command: BootcampUpdateCommand) {
        val bootcamp = bootcampReader.readForUpdate(bootcampId)
        bootcampManager.update(bootcamp, command)
    }

    @Transactional
    fun startRecruitment(bootcampId: Long) {
        val bootcamp = bootcampReader.readForUpdate(bootcampId)
        bootcampManager.startRecruitment(bootcamp)
    }

    @Transactional
    fun close(bootcampId: Long, now: LocalDateTime = LocalDateTime.now(clock)) {
        val bootcamp = bootcampReader.readForUpdate(bootcampId)
        bootcampManager.close(bootcamp, now)
    }

    @Transactional
    fun delete(bootcampId: Long, now: LocalDateTime = LocalDateTime.now(clock)) {
        val bootcamp = bootcampReader.readForUpdate(bootcampId)
        bootcampManager.delete(bootcamp, now)
    }
}
