package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampCurriculum
import com.ogonggo.core.bootcamp.domain.BootcampPartner
import com.ogonggo.core.bootcamp.persistence.BootcampCurriculumJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampPartnerJpaRepository
import org.springframework.stereotype.Component

interface BootcampAppender {
    fun append(command: BootcampAppendCommand): Bootcamp
}

@Component
internal class BootcampAppenderImpl(
    private val bootcampRepository: BootcampJpaRepository,
    private val bootcampPartnerRepository: BootcampPartnerJpaRepository,
    private val bootcampCurriculumRepository: BootcampCurriculumJpaRepository,
) : BootcampAppender {

    override fun append(command: BootcampAppendCommand): Bootcamp {
        val bootcamp = bootcampRepository.save(
            Bootcamp(
                ownerUserId = command.ownerUserId,
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
                status = command.status,
                closedAt = command.closedAt,
            ),
        )
        val bootcampId = checkNotNull(bootcamp.id) { "저장된 부트캠프 식별자가 없습니다." }

        bootcampPartnerRepository.saveAll(command.partners.map { it.toEntity(bootcampId) })
        bootcampCurriculumRepository.saveAll(command.curriculums.map { it.toEntity(bootcampId) })

        return bootcamp
    }
}

private fun BootcampPartnerCommand.toEntity(bootcampId: Long): BootcampPartner = BootcampPartner(
    bootcampId = bootcampId,
    partnerName = partnerName,
    displayOrder = displayOrder,
)

private fun BootcampCurriculumCommand.toEntity(bootcampId: Long): BootcampCurriculum = BootcampCurriculum(
    bootcampId = bootcampId,
    startWeek = startWeek,
    endWeek = endWeek,
    subtitle = subtitle,
    displayOrder = displayOrder,
)
