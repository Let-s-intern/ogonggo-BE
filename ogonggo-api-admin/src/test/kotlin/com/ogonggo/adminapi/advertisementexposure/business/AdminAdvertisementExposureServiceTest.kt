package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureManager
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateDto
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateTargetDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.error.InvalidValueException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class AdminAdvertisementExposureServiceTest {

    private val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
    private val exposureAppender = Mockito.mock(AdvertisementExposureAppender::class.java)
    private val exposureManager = Mockito.mock(AdvertisementExposureManager::class.java)
    private val productReader = Mockito.mock(AdvertisementProductReader::class.java)
    private val contractReader = Mockito.mock(AdvertisementContractReader::class.java)
    private val clock = Clock.fixed(LocalDateTime.parse("2026-11-08T09:00:00").atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
    private val service = AdminAdvertisementExposureService(exposureReader, exposureAppender, exposureManager, productReader, contractReader, clock)

    @Test
    fun `관리자 목록은 계약의 현재 회사명을 파생하고 core 정렬 결과를 보존한다`() {
        val exposure = Mockito.mock(AdvertisementExposure::class.java).also {
            Mockito.`when`(it.id).thenReturn(501)
            Mockito.`when`(it.productId).thenReturn(11)
            Mockito.`when`(it.contractId).thenReturn(1001)
            Mockito.`when`(it.name).thenReturn("인턴 채용")
            Mockito.`when`(it.imageUrl).thenReturn("https://example.com/banner.png")
            Mockito.`when`(it.linkUrl).thenReturn("/jobs/5501")
            Mockito.`when`(it.adLabel).thenReturn(true)
            Mockito.`when`(it.exposed).thenReturn(false)
            Mockito.`when`(it.displayOrder).thenReturn(3)
            Mockito.`when`(it.createdAt).thenReturn(LocalDateTime.parse("2026-11-07T18:10:00"))
            Mockito.`when`(it.updatedAt).thenReturn(LocalDateTime.parse("2026-11-08T09:00:00"))
        }
        val contract = Mockito.mock(AdvertisementContract::class.java).also {
            Mockito.`when`(it.companyName).thenReturn("현재 회사명")
        }
        Mockito.`when`(exposureReader.readByProduct(11, 1001)).thenReturn(listOf(exposure))
        Mockito.`when`(contractReader.readAllByIds(setOf(1001))).thenReturn(mapOf(1001L to contract))

        val result = service.getExposures(11, 1001).single()

        assertEquals(501, result.id)
        assertEquals(1001, result.contractId)
        assertEquals("현재 회사명", result.advertiserCompanyName)
        assertEquals(3, result.displayOrder)
        assertEquals(false, result.exposed)
    }

    @Test
    fun `배너 등록은 허용 URL과 평탄화 필드를 core appender에 전달한다`() {
        val product = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(productReader.readForUpdate(11)).thenReturn(product)
        Mockito.`when`(exposureAppender.append(anyExposureAppendDto())).thenReturn(501)

        val result = service.createExposure(
            command(
                imageUrl = "https://example.com/banner.png",
                linkUrl = "/jobs/5501?source=ad",
                startAt = LocalDateTime.parse("2026-11-01T00:00:00"),
                endAt = LocalDateTime.parse("2026-11-30T23:59:59"),
            ),
        )

        assertEquals(CreateAdminAdvertisementExposureResult(501), result)
        val dto = ArgumentCaptor.forClass(AdvertisementExposureAppendDto::class.java)
        Mockito.verify(exposureAppender).append(dto.capture() ?: dummyExposureAppendDto())
        assertEquals(11, dto.value.productId)
        assertEquals(1001, dto.value.contractId)
        assertEquals(AdvertisementExposureContent("인턴 채용", "https://example.com/banner.png", "/jobs/5501?source=ad"), dto.value.content)
        assertEquals(true, dto.value.adLabel)
        assertEquals(LocalDateTime.parse("2026-11-01T00:00:00"), dto.value.period.startAt)
        assertEquals(LocalDateTime.parse("2026-11-30T23:59:59"), dto.value.period.endAt)
        assertEquals(true, dto.value.exposed)
    }

    @Test
    fun `배너 imageUrl은 HTTP(S) 절대 URL이어야 한다`() {
        val product = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(productReader.readForUpdate(11)).thenReturn(product)

        listOf("ftp://example.com/banner.png", "https://", "/images/banner.png", "javascript:alert(1)").forEach { url ->
            val exception = assertThrows(InvalidRequestFieldException::class.java) {
                service.createExposure(command(imageUrl = url))
            }
            assertEquals("imageUrl", exception.fieldName)
        }

        Mockito.verifyNoInteractions(exposureAppender)
    }

    @Test
    fun `배너 linkUrl은 HTTP(S) URL 또는 단일 슬래시 내부 경로만 허용한다`() {
        val product = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(productReader.readForUpdate(11)).thenReturn(product)
        Mockito.`when`(exposureAppender.append(anyExposureAppendDto())).thenReturn(501)

        service.createExposure(command(linkUrl = "https://example.com/jobs/5501"))
        service.createExposure(command(linkUrl = "/jobs/5501"))

        listOf("//example.com/jobs/5501", "javascript:alert(1)", "jobs/5501", "https://").forEach { url ->
            val exception = assertThrows(InvalidRequestFieldException::class.java) {
                service.createExposure(command(linkUrl = url))
            }
            assertEquals("linkUrl", exception.fieldName)
        }
        Mockito.verify(exposureAppender, Mockito.times(2)).append(anyExposureAppendDto())
    }

    @Test
    fun `광고 공고 상품에는 target만 전달하고 노출 불가 상품은 거부한다`() {
        val jobPostingProduct = product(AdvertisementProductCategory.JOB_POSTING)
        Mockito.`when`(productReader.readForUpdate(11)).thenReturn(jobPostingProduct)
        Mockito.`when`(exposureAppender.append(anyExposureAppendDto())).thenReturn(501)

        service.createExposure(
            command(
                name = null,
                imageUrl = null,
                linkUrl = null,
                targetType = com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType.JOB,
                targetId = 5501,
            ),
        )
        val dto = ArgumentCaptor.forClass(AdvertisementExposureAppendDto::class.java)
        Mockito.verify(exposureAppender).append(dto.capture() ?: dummyExposureAppendDto())
        assertEquals(null, dto.value.content.name)
        assertEquals(com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType.JOB, dto.value.content.targetType)
        assertEquals(5501, dto.value.content.targetId)

        val nonExposableProduct = product(AdvertisementProductCategory.INSTAGRAM_1)
        Mockito.`when`(productReader.readForUpdate(11)).thenReturn(nonExposableProduct)
        val exception = assertThrows(InvalidValueException::class.java) { service.createExposure(command()) }
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE, exception.errorCode)
        Mockito.verifyNoMoreInteractions(exposureAppender)
    }

    @Test
    fun `시작 종료 관계는 core의 노출 기간 오류를 반환한다`() {
        val product = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(productReader.readForUpdate(11)).thenReturn(product)

        val exception = assertThrows(InvalidValueException::class.java) {
            service.createExposure(
                command(
                    startAt = LocalDateTime.parse("2026-11-30T00:00:00"),
                    endAt = LocalDateTime.parse("2026-11-01T00:00:00"),
                ),
            )
        }
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_PERIOD_INVALID, exception.errorCode)
        Mockito.verifyNoInteractions(exposureAppender)
    }

    @Test
    fun `노출 전체 수정은 기존 상품 자리에서 변경 가능한 필드만 core에 전달한다`() {
        val exposure = Mockito.mock(AdvertisementExposure::class.java).also {
            Mockito.`when`(it.productId).thenReturn(11)
        }
        val product = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(exposureReader.readForUpdateWithActiveProduct(501))
            .thenReturn(AdvertisementExposureUpdateTargetDto(exposure, product))

        service.updateExposure(
            UpdateAdminAdvertisementExposureCommand(
                exposureId = 501,
                contractId = null,
                name = "새 배너",
                imageUrl = "https://example.com/new.png",
                linkUrl = "/jobs/5502",
                targetType = null,
                targetId = null,
                adLabel = false,
                startAt = null,
                endAt = null,
            ),
        )

        val dto = ArgumentCaptor.forClass(AdvertisementExposureUpdateDto::class.java)
        Mockito.verify(exposureManager).update(Mockito.eq(exposure) ?: exposure, dto.capture() ?: dummyExposureUpdateDto())
        Mockito.verify(exposureReader).readForUpdateWithActiveProduct(501)
        assertEquals(null, dto.value.contractId)
        assertEquals(AdvertisementExposureContent("새 배너", "https://example.com/new.png", "/jobs/5502"), dto.value.content)
        assertEquals(false, dto.value.adLabel)
        assertEquals(AdvertisementExposurePeriod.ALWAYS, dto.value.period)
    }

    @Test
    fun `노출 수정의 상품 URL 계약과 기간 오류는 core 변경 전에 막는다`() {
        val exposure = Mockito.mock(AdvertisementExposure::class.java).also {
            Mockito.`when`(it.productId).thenReturn(11)
        }
        val product = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(exposureReader.readForUpdateWithActiveProduct(501))
            .thenReturn(AdvertisementExposureUpdateTargetDto(exposure, product))

        val urlFailure = assertThrows(InvalidRequestFieldException::class.java) {
            service.updateExposure(updateCommand(imageUrl = "ftp://example.com/banner.png"))
        }
        assertEquals("imageUrl", urlFailure.fieldName)
        val periodFailure = assertThrows(InvalidValueException::class.java) {
            service.updateExposure(updateCommand(startAt = LocalDateTime.parse("2026-11-30T00:00:00"), endAt = LocalDateTime.parse("2026-11-01T00:00:00")))
        }
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_PERIOD_INVALID, periodFailure.errorCode)
        Mockito.verifyNoInteractions(exposureManager)
    }

    @Test
    fun `켜기 끄기 반복 호출은 manager에 명시한 값으로 전달한다`() {
        val exposure = Mockito.mock(AdvertisementExposure::class.java)
        Mockito.`when`(exposureReader.readForUpdate(501)).thenReturn(exposure)

        service.changeExposed(501, false)
        service.changeExposed(501, true)

        Mockito.verify(exposureManager).changeExposed(exposure, false)
        Mockito.verify(exposureManager).changeExposed(exposure, true)
    }

    @Test
    fun `순서 이동은 상품 잠금 후 읽은 노출을 core manager에 전체 자리 이동으로 전달한다`() {
        val exposure = Mockito.mock(AdvertisementExposure::class.java)
        Mockito.`when`(exposureReader.readForMove(501)).thenReturn(exposure)

        service.moveExposure(501, AdvertisementExposureMoveDirection.DOWN)

        Mockito.verify(exposureReader).readForMove(501)
        Mockito.verify(exposureManager).move(exposure, AdvertisementExposureMoveDirection.DOWN)
    }

    @Test
    fun `노출 삭제는 주입된 서울 Clock으로 시각을 전달한다`() {
        val exposure = Mockito.mock(AdvertisementExposure::class.java)
        Mockito.`when`(exposureReader.readForUpdate(501)).thenReturn(exposure)

        service.deleteExposure(501)

        Mockito.verify(exposureManager).delete(exposure, LocalDateTime.parse("2026-11-08T09:00:00"))
    }

    private fun product(category: AdvertisementProductCategory): AdvertisementProduct =
        Mockito.mock(AdvertisementProduct::class.java).also {
            Mockito.`when`(it.category).thenReturn(category)
        }

    private fun anyExposureAppendDto(): AdvertisementExposureAppendDto =
        Mockito.any(AdvertisementExposureAppendDto::class.java)
            ?: dummyExposureAppendDto()

    private fun dummyExposureAppendDto() =
        AdvertisementExposureAppendDto(
            productId = 11,
            contractId = 1001,
            content = AdvertisementExposureContent("인턴 채용", "https://example.com/banner.png", "/jobs/5501"),
            adLabel = true,
            period = AdvertisementExposurePeriod.ALWAYS,
            exposed = true,
        )

    private fun dummyExposureUpdateDto() = AdvertisementExposureUpdateDto(
        contractId = null,
        content = AdvertisementExposureContent("새 배너", "https://example.com/new.png", "/jobs/5502"),
        adLabel = false,
        period = AdvertisementExposurePeriod.ALWAYS,
    )

    private fun updateCommand(
        imageUrl: String? = "https://example.com/banner.png",
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
    ) = UpdateAdminAdvertisementExposureCommand(
        exposureId = 501,
        contractId = 1001,
        name = "인턴 채용",
        imageUrl = imageUrl,
        linkUrl = "/jobs/5501",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = startAt,
        endAt = endAt,
    )

    private fun command(
        name: String? = "인턴 채용",
        imageUrl: String? = "https://example.com/banner.png",
        linkUrl: String? = "/jobs/5501",
        targetType: com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType? = null,
        targetId: Long? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
    ) = CreateAdminAdvertisementExposureCommand(
        productId = 11,
        contractId = 1001,
        name = name,
        imageUrl = imageUrl,
        linkUrl = linkUrl,
        targetType = targetType,
        targetId = targetId,
        adLabel = true,
        startAt = startAt,
        endAt = endAt,
        exposed = true,
    )
}
