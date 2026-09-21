package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import com.ogonggo.core.error.ConflictException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class CrawlerBootcampService(
    private val bootcampReader: BootcampReader,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val bootcampContentReader: BootcampContentReader,
    private val clock: Clock,
) {

    /**
     * 크롤러가 수집한 부트캠프를 모집 중으로 곧바로 게시한다. 검수는 기업회원이 올린 부트캠프만 거친다.
     *
     * 같은 원문을 다시 등록하면 거절하고, 크롤러는 등록 응답의 식별자로 부트캠프를 교체한다.
     */
    @Transactional
    fun register(command: CrawlerBootcampCommand): Long {
        if (bootcampReader.existsBySourceUrl(command.sourceUrl)) {
            throw ConflictException(BootcampErrorCode.BOOTCAMP_ALREADY_EXISTS)
        }

        val bootcamp = bootcampAppender.append(command.toAppendDto())
        return checkNotNull(bootcamp.id) { "저장된 부트캠프 식별자가 없습니다." }
    }

    /**
     * 다시 수집한 값으로 부트캠프 전체를 바꾸고 커리큘럼은 지운 뒤 새로 넣는다.
     * 운영자가 관리자 콘솔에서 고친 내용도 크롤러 값으로 덮는다. 게시·모집 상태와 공개 기간, 파트너사는 크롤러가 보내는 값이 아니므로 그대로 둔다.
     */
    @Transactional
    fun replace(bootcampId: Long, command: CrawlerBootcampCommand) {
        val bootcamp = bootcampReader.readCrawledForUpdate(bootcampId)
        if (command.sourceUrl != bootcamp.sourceUrl && bootcampReader.existsBySourceUrl(command.sourceUrl)) {
            throw ConflictException(BootcampErrorCode.BOOTCAMP_ALREADY_EXISTS)
        }

        val partners = bootcampContentReader.readPartners(bootcampId)
            .map { BootcampPartnerDto.Request(partnerName = it.name, displayOrder = it.displayOrder) }
        bootcampManager.update(bootcamp, command.toUpdateDto(bootcamp, partners))
    }

    /** 더는 쓰지 않는 수집 부트캠프를 지운다. 이미 지운 부트캠프를 다시 지워도 성공한다. */
    @Transactional
    fun delete(bootcampId: Long) {
        bootcampManager.delete(bootcampReader.readCrawledForDelete(bootcampId), LocalDateTime.now(clock))
    }

    /** 크롤러가 등록 응답의 식별자를 잃고 409를 받았을 때 원문 URL로 식별자를 되찾는다. */
    fun getBootcampId(sourceUrl: String): Long =
        checkNotNull(bootcampReader.readCrawledBySourceUrl(sourceUrl).id) { "부트캠프 식별자가 없습니다." }
}

private fun CrawlerBootcampCommand.toAppendDto(): BootcampAppendDto = BootcampAppendDto(
    companyName = companyName,
    title = title,
    programType = programType,
    operationType = operationType,
    recruitmentType = recruitmentType,
    recruitmentStartAt = recruitmentStartAt,
    recruitmentEndAt = recruitmentEndAt,
    programStartDate = programStartDate,
    programEndDate = programEndDate,
    capacity = capacity,
    tuitionType = tuitionType,
    tuitionAmount = tuitionAmount,
    representativeImageUrl = representativeImageUrl,
    shortDescription = shortDescription,
    content = content,
    eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
    applicationMethod = applicationMethod,
    applicationUrl = applicationUrl,
    managerEmail = managerEmail,
    inquiryUrl = inquiryUrl,
    sourceUrl = sourceUrl,
    curriculums = curriculumDtos(),
    status = BootcampStatus.RECRUITING,
    publicationStatus = BootcampPublicationStatus.PUBLISHED,
)

private fun CrawlerBootcampCommand.toUpdateDto(
    bootcamp: Bootcamp,
    partners: List<BootcampPartnerDto.Request>,
): BootcampUpdateDto = BootcampUpdateDto(
    companyName = companyName,
    title = title,
    programType = programType,
    operationType = operationType,
    recruitmentType = recruitmentType,
    recruitmentStartAt = recruitmentStartAt,
    recruitmentEndAt = recruitmentEndAt,
    programStartDate = programStartDate,
    programEndDate = programEndDate,
    capacity = capacity,
    tuitionType = tuitionType,
    tuitionAmount = tuitionAmount,
    representativeImageUrl = representativeImageUrl,
    shortDescription = shortDescription,
    content = content,
    eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
    applicationMethod = applicationMethod,
    applicationUrl = applicationUrl,
    managerEmail = managerEmail,
    inquiryUrl = inquiryUrl,
    publicationStartAt = bootcamp.publicationStartAt,
    publicationEndAt = bootcamp.publicationEndAt,
    sourceUrl = sourceUrl,
    partners = partners,
    curriculums = curriculumDtos(),
)

private fun CrawlerBootcampCommand.curriculumDtos(): List<BootcampCurriculumDto.Request> =
    curriculums.mapIndexed { index, curriculum ->
        BootcampCurriculumDto.Request(
            startWeek = curriculum.startWeek,
            endWeek = curriculum.endWeek,
            subtitle = curriculum.subtitle,
            displayOrder = index,
        )
    }
