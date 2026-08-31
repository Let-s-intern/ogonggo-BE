package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampCurriculum
import com.ogonggo.core.bootcamp.domain.BootcampPartner
import com.ogonggo.core.bootcamp.persistence.BootcampCurriculumJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampPartnerJpaRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

interface BootcampManager {
    fun update(bootcamp: Bootcamp, command: BootcampUpdateCommand)
    fun startRecruitment(bootcamp: Bootcamp)
    fun close(bootcamp: Bootcamp, now: LocalDateTime)
    fun delete(bootcamp: Bootcamp, now: LocalDateTime)
}

@Component
internal class BootcampManagerImpl(
    private val bootcampRepository: BootcampJpaRepository,
    private val bootcampPartnerRepository: BootcampPartnerJpaRepository,
    private val bootcampCurriculumRepository: BootcampCurriculumJpaRepository,
    private val clock: Clock,
) : BootcampManager {

    override fun update(bootcamp: Bootcamp, command: BootcampUpdateCommand) {
        val bootcampId = checkNotNull(bootcamp.id) { "부트캠프 식별자가 없습니다." }
        require(command.partners.map { it.partnerName }.distinct().size == command.partners.size) {
            "중복된 파트너사명은 등록할 수 없습니다."
        }
        val now = LocalDateTime.now(clock)
        val existingPartners = bootcampPartnerRepository.findAllByBootcampId(bootcampId)
        existingPartners.filter { it.deletedAt == null }.forEach { it.delete(now) }
        val existingPartnerByName = existingPartners.associateBy { it.partnerName }
        val partners = command.partners.map { partnerCommand ->
            existingPartnerByName[partnerCommand.partnerName]
                ?.apply { restore(partnerCommand.displayOrder) }
                ?: partnerCommand.toPartner(bootcampId)
        }
        val existingCurriculums =
            bootcampCurriculumRepository.findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
                .onEach { it.delete(now) }
        val curriculums = command.curriculums.map { it.toCurriculum(bootcampId) }

        bootcamp.update(
            companyName = command.companyName,
            title = command.title,
            programType = command.programType,
            operationType = command.operationType,
            recruitmentType = command.recruitmentType,
            recruitmentStartAt = command.recruitmentStartAt,
            recruitmentEndAt = command.recruitmentEndAt,
            programStartDate = command.programStartDate,
            programEndDate = command.programEndDate,
            capacity = command.capacity,
            tuitionType = command.tuitionType,
            tuitionAmount = command.tuitionAmount,
            representativeImageUrl = command.representativeImageUrl,
            shortDescription = command.shortDescription,
            content = command.content,
            eligibilityAndSelectionProcess = command.eligibilityAndSelectionProcess,
            applicationMethod = command.applicationMethod,
            applicationUrl = command.applicationUrl,
            managerEmail = command.managerEmail,
            inquiryUrl = command.inquiryUrl,
            publicationStartAt = command.publicationStartAt,
            publicationEndAt = command.publicationEndAt,
            sourceUrl = command.sourceUrl,
        )
        bootcampRepository.save(bootcamp)
        bootcampPartnerRepository.saveAll(existingPartners)
        bootcampCurriculumRepository.saveAll(existingCurriculums)
        bootcampPartnerRepository.saveAll(partners)
        bootcampCurriculumRepository.saveAll(curriculums)
    }

    override fun startRecruitment(bootcamp: Bootcamp) = change(bootcamp) { startRecruitment() }

    override fun close(bootcamp: Bootcamp, now: LocalDateTime) = change(bootcamp) { close(now) }

    override fun delete(bootcamp: Bootcamp, now: LocalDateTime) = change(bootcamp) { delete(now) }

    private fun change(bootcamp: Bootcamp, change: Bootcamp.() -> Unit) {
        bootcamp.change()
        bootcampRepository.save(bootcamp)
    }
}

private fun BootcampPartnerCommand.toPartner(bootcampId: Long): BootcampPartner = BootcampPartner(
    bootcampId = bootcampId,
    partnerName = partnerName,
    displayOrder = displayOrder,
)

private fun BootcampCurriculumCommand.toCurriculum(bootcampId: Long): BootcampCurriculum = BootcampCurriculum(
    bootcampId = bootcampId,
    startWeek = startWeek,
    endWeek = endWeek,
    subtitle = subtitle,
    displayOrder = displayOrder,
)
