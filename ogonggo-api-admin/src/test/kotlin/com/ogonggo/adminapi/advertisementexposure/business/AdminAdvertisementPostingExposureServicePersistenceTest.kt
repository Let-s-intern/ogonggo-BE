package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.dto.JobAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-advertisement-posting-exposure;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminAdvertisementPostingExposureServicePersistenceTest @Autowired constructor(
    private val service: AdminAdvertisementExposureService,
    private val productAppender: AdvertisementProductAppender,
    private val exposureReader: AdvertisementExposureReader,
    private val jobAppender: JobAppender,
    private val bootcampAppender: BootcampAppender,
) {

    @Test
    fun `광고 공고 자리에 있는 채용공고를 등록하면 대상 종류와 식별자를 저장한다`() {
        // given
        val productId = product(AdvertisementProductPosition.JOB_LIST)
        val jobId = job()

        // when
        val exposureId = service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)

        // then
        val saved = exposureReader.readByProduct(productId).single()
        assertEquals(exposureId, saved.id)
        assertEquals(AdvertisementExposureTargetType.JOB, saved.targetType)
        assertEquals(jobId, saved.targetId)
    }

    @Test
    fun `부트캠프 목록 자리에는 부트캠프를 등록한다`() {
        // given
        val productId = product(AdvertisementProductPosition.BOOTCAMP_LIST)
        val bootcampId = bootcamp()

        // when
        service.createExposure(command(productId, AdvertisementExposureTargetType.BOOTCAMP, bootcampId), ADMIN_ID)

        // then
        assertEquals(bootcampId, exposureReader.readByProduct(productId).single().targetId)
    }

    @Test
    fun `광고 공고 상품은 대상이 없으면 등록하지 않는다`() {
        val productId = product(AdvertisementProductPosition.JOB_LIST)

        val exception = assertThrows(InvalidValueException::class.java) {
            service.createExposure(command(productId, targetType = null, targetId = null), ADMIN_ID)
        }

        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED, exception.errorCode)
    }

    @Test
    fun `없는 공고를 대상으로 등록하지 않는다`() {
        val productId = product(AdvertisementProductPosition.JOB_LIST)

        val exception = assertThrows(EntityNotFoundException::class.java) {
            service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, 987_654_321L), ADMIN_ID)
        }

        assertEquals(JobErrorCode.JOB_NOT_FOUND, exception.errorCode)
        assertEquals(emptyList<Long>(), exposureReader.readByProduct(productId).map { it.id })
    }

    @Test
    fun `부트캠프 목록 자리에 채용공고를 등록하지 않는다`() {
        val productId = product(AdvertisementProductPosition.BOOTCAMP_LIST)
        val jobId = job()

        val exception = assertThrows(InvalidValueException::class.java) {
            service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)
        }

        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_TARGET_TYPE_MISMATCH, exception.errorCode)
    }

    @Test
    fun `채용공고 자리에 부트캠프를 등록하지 않는다`() {
        val productId = product(AdvertisementProductPosition.JOB_DETAIL_SIDE)
        val bootcampId = bootcamp()

        val exception = assertThrows(InvalidValueException::class.java) {
            service.createExposure(command(productId, AdvertisementExposureTargetType.BOOTCAMP, bootcampId), ADMIN_ID)
        }

        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_TARGET_TYPE_MISMATCH, exception.errorCode)
    }

    @Test
    fun `광고 공고에 배너 이미지와 링크를 함께 보내면 등록하지 않는다`() {
        val productId = product(AdvertisementProductPosition.JOB_LIST)
        val jobId = job()

        val exception = assertThrows(InvalidValueException::class.java) {
            service.createExposure(
                command(productId, AdvertisementExposureTargetType.JOB, jobId)
                    .copy(imageUrl = "https://example.com/banner.png", linkUrl = "/jobs/1"),
                ADMIN_ID,
            )
        }

        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED, exception.errorCode)
    }

    @Test
    fun `같은 자리에 같은 공고를 두 번 등록하지 않는다`() {
        // given
        val first = product(AdvertisementProductPosition.JOB_LIST)
        val second = product(AdvertisementProductPosition.JOB_LIST)
        val jobId = job()
        service.createExposure(command(first, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)

        // when
        val sameProduct = assertThrows(ConflictException::class.java) {
            service.createExposure(command(first, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)
        }
        val samePosition = assertThrows(ConflictException::class.java) {
            service.createExposure(command(second, AdvertisementExposureTargetType.JOB, jobId).copy(exposed = false), ADMIN_ID)
        }

        // then
        listOf<BusinessException>(sameProduct, samePosition).forEach {
            assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_TARGET_DUPLICATED, it.errorCode)
        }
    }

    @Test
    fun `다른 자리에는 같은 공고를 등록할 수 있다`() {
        // given
        val list = product(AdvertisementProductPosition.JOB_LIST)
        val side = product(AdvertisementProductPosition.JOB_DETAIL_SIDE)
        val jobId = job()
        service.createExposure(command(list, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)

        // when
        service.createExposure(command(side, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)

        // then
        assertEquals(jobId, exposureReader.readByProduct(side).single().targetId)
    }

    @Test
    fun `지운 노출의 공고는 같은 자리에 다시 등록할 수 있다`() {
        // given
        val productId = product(AdvertisementProductPosition.JOB_LIST)
        val jobId = job()
        val deleted = service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)
        service.deleteExposure(deleted)

        // when
        val again = service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, jobId), ADMIN_ID)

        // then
        assertEquals(listOf(again), exposureReader.readByProduct(productId).map { it.id })
    }

    @Test
    fun `노출을 고칠 때 자기 자신은 중복으로 보지 않고 다른 노출의 공고로는 바꾸지 않는다`() {
        // given
        val productId = product(AdvertisementProductPosition.JOB_LIST)
        val firstJob = job()
        val secondJob = job()
        val first = service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, firstJob), ADMIN_ID)
        service.createExposure(command(productId, AdvertisementExposureTargetType.JOB, secondJob), ADMIN_ID)

        // when
        service.updateExposure(updateCommand(first, firstJob).copy(adLabel = false), ADMIN_ID)
        val exception = assertThrows(ConflictException::class.java) {
            service.updateExposure(updateCommand(first, secondJob), ADMIN_ID)
        }

        // then
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_TARGET_DUPLICATED, exception.errorCode)
        assertEquals(firstJob, exposureReader.readByProduct(productId).first { it.id == first }.targetId)
    }

    private fun product(position: AdvertisementProductPosition): Long = productAppender.append(
        AdvertisementProductSaveDto(
            category = AdvertisementProductCategory.JOB_POSTING,
            position = position,
            name = "광고 공고 ${position.desc}",
            price = 500_000,
        ),
    )

    private fun job(
        title: String = "백엔드 개발자",
        publicationStatus: JobPublicationStatus = JobPublicationStatus.PUBLISHED,
    ): Long = checkNotNull(
        jobAppender.append(
            JobAppendDto(
                companyName = "오공고",
                title = title,
                publicationStatus = publicationStatus,
                employmentType = JobEmploymentType.FULL_TIME,
                experienceType = JobExperienceType.EXPERIENCED,
                recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
                responsibilities = "서버 개발",
            ),
        ).id,
    )

    private fun bootcamp(): Long = checkNotNull(
        bootcampAppender.append(
            BootcampAppendDto(
                companyName = "오공고 교육사",
                title = "백엔드 부트캠프",
                programType = "개발",
                operationType = BootcampOperationType.ONLINE,
                recruitmentType = BootcampRecruitmentType.PERIOD,
                recruitmentStartAt = LocalDateTime.of(2026, 8, 1, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
                programStartDate = LocalDate.of(2026, 9, 1),
                programEndDate = LocalDate.of(2026, 12, 1),
                tuitionType = BootcampTuitionType.FREE,
                representativeImageUrl = null,
                shortDescription = "백엔드 개발자로 성장하는 12주",
                content = "부트캠프 상세 내용",
                applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
                applicationUrl = "https://example.com/apply",
            ),
        ).id,
    )

    private fun command(productId: Long, targetType: AdvertisementExposureTargetType?, targetId: Long?) =
        CreateAdminAdvertisementExposureCommand(
            productId = productId,
            contractId = null,
            name = null,
            imageUrl = null,
            mobileImageUrl = null,
            linkUrl = null,
            targetType = targetType,
            targetId = targetId,
            adLabel = true,
            startAt = null,
            endAt = null,
            exposed = true,
        )

    private fun updateCommand(exposureId: Long, jobId: Long) = UpdateAdminAdvertisementExposureCommand(
        exposureId = exposureId,
        contractId = null,
        name = null,
        imageUrl = null,
        mobileImageUrl = null,
        linkUrl = null,
        targetType = AdvertisementExposureTargetType.JOB,
        targetId = jobId,
        adLabel = true,
        startAt = null,
        endAt = null,
    )

    private companion object {
        const val ADMIN_ID = 7L
    }
}
