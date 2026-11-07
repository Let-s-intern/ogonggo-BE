package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractQueryRepository
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureQueryRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductManager
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdvertisementExposureAppender::class,
    AdvertisementExposureManager::class,
    AdvertisementExposureReader::class,
    AdvertisementExposureQueryRepository::class,
    AdvertisementProductAppender::class,
    AdvertisementProductManager::class,
    AdvertisementProductReader::class,
    AdvertisementContractAppender::class,
    AdvertisementContractReader::class,
    AdvertisementContractQueryRepository::class,
)
internal class AdvertisementExposurePersistenceTest @Autowired constructor(
    private val appender: AdvertisementExposureAppender,
    private val manager: AdvertisementExposureManager,
    private val reader: AdvertisementExposureReader,
    private val repository: AdvertisementExposureJpaRepository,
    private val productAppender: AdvertisementProductAppender,
    private val productManager: AdvertisementProductManager,
    private val productReader: AdvertisementProductReader,
    private val contractAppender: AdvertisementContractAppender,
    private val entityManager: EntityManager,
) {

    @Test
    fun `사용자 화면은 켜져 있고 기간 안인 그 자리 노출만 순서대로 준다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val bottom = appendProduct(AdvertisementProductPosition.JOB_HOME_BOTTOM_BANNER)
        val always = append(top)
        val inPeriod = append(top, AdvertisementExposurePeriod(NOW.minusDays(1), NOW.plusDays(1)))
        append(top, AdvertisementExposurePeriod(NOW.plusSeconds(1), NOW.plusDays(1)))
        append(top, AdvertisementExposurePeriod(NOW.minusDays(1), NOW.minusSeconds(1)))
        append(top, exposed = false)
        val deleted = append(top)
        manager.delete(reader.readForUpdate(deleted), NOW)
        append(bottom)
        flushAndClear()

        // when
        val shown = reader.readShown(AdvertisementProductPosition.JOB_HOME_TOP_BANNER, NOW)

        // then
        assertEquals(listOf(always, inPeriod), shown.map { it.id })
    }

    @Test
    fun `지운 상품 자리의 노출은 사용자 화면에 보이지 않는다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        append(top)
        productManager.delete(productReader.read(top), NOW)
        flushAndClear()

        // when & then
        assertEquals(emptyList<Long>(), reader.readShown(AdvertisementProductPosition.JOB_HOME_TOP_BANNER, NOW).map { it.id })
    }

    @Test
    fun `새 노출은 그 자리의 맨 뒤에 두고 위로 아래로 옮기면 순서를 1부터 다시 매긴다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val first = append(top)
        val second = append(top)
        val third = append(top)

        // when
        manager.move(reader.readForUpdate(third), AdvertisementExposureMoveDirection.UP)
        manager.move(reader.readForUpdate(first), AdvertisementExposureMoveDirection.UP)
        flushAndClear()

        // then
        val ordered = reader.readByProduct(top)
        assertEquals(listOf(first, third, second), ordered.map { it.id })
        assertEquals(listOf(1, 2, 3), ordered.map { it.displayOrder })
    }

    @Test
    fun `세팅 화면은 광고주 계약으로 거른다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val contractId = appendContract()
        append(top)
        val sponsored = append(top, contractId = contractId)

        // when & then
        assertEquals(listOf(sponsored), reader.readByProduct(top, contractId).map { it.id })
    }

    @Test
    fun `없는 광고주 계약으로는 노출을 만들 수 없다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) { append(top, contractId = 999L) }

        // then
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND, exception.errorCode)
    }

    private fun appendProduct(position: AdvertisementProductPosition): Long = productAppender.append(
        AdvertisementProductSaveDto(
            category = AdvertisementProductCategory.BANNER,
            position = position,
            name = position.desc,
            price = 300_000,
        ),
    )

    private fun append(
        productId: Long,
        period: AdvertisementExposurePeriod = AdvertisementExposurePeriod.ALWAYS,
        exposed: Boolean = true,
        contractId: Long? = null,
    ): Long = appender.append(
        AdvertisementExposureAppendDto(
            productId = productId,
            contractId = contractId,
            content = AdvertisementExposureContent(
                name = "오늘의 공고 홍보",
                imageUrl = "https://example.com/banner.png",
                linkUrl = "/calendar",
            ),
            adLabel = false,
            period = period,
            exposed = exposed,
        ),
    )

    private fun appendContract(): Long = contractAppender.appendFromForm(
        AdvertisementContractFormAppendDto(
            userId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "코드스테이션",
            recruitmentTitle = "10월 기획전",
            recruitmentUrl = "https://example.com/recruit",
            recruitmentDeadline = LocalDate.of(2026, 12, 20),
            purpose = AdvertisementContractPurpose.BRAND_AWARENESS,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "010-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.PROVIDE_DIRECTLY,
        ),
    )

    private fun flushAndClear() {
        repository.flush()
        entityManager.clear()
    }

    companion object {
        private val NOW = LocalDateTime.of(2026, 11, 7, 12, 0)
    }
}
