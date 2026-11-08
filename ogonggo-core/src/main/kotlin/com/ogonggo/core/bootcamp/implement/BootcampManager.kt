package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractManager
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampCurriculum
import com.ogonggo.core.bootcamp.domain.BootcampPartner
import com.ogonggo.core.bootcamp.implement.dto.BootcampContentEditDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import com.ogonggo.core.bootcamp.persistence.BootcampCurriculumJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampPartnerJpaRepository
import com.ogonggo.core.contentreview.domain.ContentReviewTargetType
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class BootcampManager internal constructor(
    private val bootcampRepository: BootcampJpaRepository,
    private val bootcampPartnerRepository: BootcampPartnerJpaRepository,
    private val bootcampCurriculumRepository: BootcampCurriculumJpaRepository,
    private val contentRejectionManager: ContentRejectionManager,
    private val advertisementContractManager: AdvertisementContractManager,
    private val clock: Clock,
) {

    fun update(bootcamp: Bootcamp, command: BootcampUpdateDto) {
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
            logoUrl = command.logoUrl,
            instructorInfo = command.instructorInfo,
            programFeatures = command.programFeatures,
            completionRequirements = command.completionRequirements,
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

    fun editContent(bootcamp: Bootcamp, command: BootcampContentEditDto) =
        change(bootcamp) { editContent(command.title, command.contents) }

    fun startRecruitment(bootcamp: Bootcamp) = change(bootcamp) { startRecruitment() }

    fun close(bootcamp: Bootcamp, now: LocalDateTime) = change(bootcamp) { close(now) }

    fun publish(bootcamp: Bootcamp) = change(bootcamp) { publish() }

    fun hide(bootcamp: Bootcamp) = change(bootcamp) { hide() }

    fun changePriorityExposed(bootcamp: Bootcamp, priorityExposed: Boolean) =
        change(bootcamp) { changePriorityExposed(priorityExposed) }

    fun delete(bootcamp: Bootcamp, now: LocalDateTime) = change(bootcamp) { delete(now) }

    fun markAdminRegistered(bootcamp: Bootcamp) = change(bootcamp) { markAdminRegistered() }

    /**
     * 반려가 풀리면 반려 기록도 함께 지운다. 검수 상태와 기록이 어긋나지 않도록 한 곳에서 처리한다.
     * 비즈니스 공고 계약의 상태도 같은 이유로 여기서 검수 상태에 맞춘다.
     */
    fun approveReview(bootcamp: Bootcamp, now: LocalDateTime) {
        change(bootcamp) { approveReview() }
        contentRejectionManager.clear(ContentReviewTargetType.BOOTCAMP, bootcamp.requiredId(), now)
        followReview(bootcamp)
    }

    fun rejectReview(bootcamp: Bootcamp, reason: String, now: LocalDateTime) {
        change(bootcamp) { rejectReview() }
        contentRejectionManager.reject(ContentReviewTargetType.BOOTCAMP, bootcamp.requiredId(), reason, now)
        followReview(bootcamp)
    }

    fun requestReview(bootcamp: Bootcamp, now: LocalDateTime) {
        change(bootcamp) { requestReview() }
        contentRejectionManager.clear(ContentReviewTargetType.BOOTCAMP, bootcamp.requiredId(), now)
        followReview(bootcamp)
    }

    private fun followReview(bootcamp: Bootcamp) {
        val reviewStatus = checkNotNull(bootcamp.reviewStatus) { "검수한 부트캠프의 검수 상태가 없습니다." }
        advertisementContractManager.followReview(AdvertisementContractPromotionTarget.BOOTCAMP, bootcamp.requiredId(), reviewStatus)
    }

    /** 모집 종료 일시가 지난 모집 중 부트캠프를 마감하고 마감한 건수를 돌려준다. */
    fun closeExpired(now: LocalDateTime): Int = bootcampRepository.closeExpired(now)

    private fun change(bootcamp: Bootcamp, change: Bootcamp.() -> Unit) {
        bootcamp.change()
        bootcampRepository.save(bootcamp)
    }
}

private fun Bootcamp.requiredId(): Long = checkNotNull(id) { "부트캠프 식별자가 없습니다." }

private fun BootcampPartnerDto.Request.toPartner(bootcampId: Long): BootcampPartner = BootcampPartner(
    bootcampId = bootcampId,
    partnerName = partnerName,
    displayOrder = displayOrder,
)

private fun BootcampCurriculumDto.Request.toCurriculum(bootcampId: Long): BootcampCurriculum = BootcampCurriculum(
    bootcampId = bootcampId,
    startWeek = startWeek,
    endWeek = endWeek,
    subtitle = subtitle,
    displayOrder = displayOrder,
)
