package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampContentEditDto
import com.ogonggo.core.review.domain.ReviewStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdminBootcampServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampManager = Mockito.mock(BootcampManager::class.java)
    private val bootcampMetricReader = Mockito.mock(BootcampMetricReader::class.java)
    private val bootcampContentReader = Mockito.mock(BootcampContentReader::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminBootcampService(
        bootcampReader,
        bootcampManager,
        bootcampMetricReader,
        bootcampContentReader,
        clock,
    )

    @Test
    fun `승인과 숨김을 함께 보내면 승인한 뒤 숨기고 내용을 고친다`() {
        val bootcamp = lockedBootcamp(reviewStatus = ReviewStatus.REJECTED)
        val contents = mapOf(BootcampContentField.ELIGIBILITY_AND_SELECTION_PROCESS to null)

        service.updateBootcamp(
            BOOTCAMP_ID,
            AdminBootcampUpdateCommand(
                visibility = AdminContentVisibility.HIDDEN,
                reviewStatus = ReviewStatus.APPROVED,
                contents = contents,
            ),
        )

        val order = Mockito.inOrder(bootcampManager)
        order.verify(bootcampManager).approveReview(bootcamp, NOW)
        order.verify(bootcampManager).hide(bootcamp)
        order.verify(bootcampManager).editContent(bootcamp, BootcampContentEditDto(contents = contents))
        Mockito.verifyNoMoreInteractions(bootcampManager)
    }

    @Test
    fun `이미 같은 검수 상태면 다시 전이하지 않는다`() {
        lockedBootcamp(reviewStatus = ReviewStatus.PENDING)

        service.updateBootcamp(BOOTCAMP_ID, AdminBootcampUpdateCommand(reviewStatus = ReviewStatus.PENDING))

        Mockito.verifyNoInteractions(bootcampManager)
    }

    @Test
    fun `삭제는 이미 삭제된 부트캠프까지 잠가 찾아 멱등하게 처리한다`() {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readForDelete(BOOTCAMP_ID)).thenReturn(bootcamp)

        service.deleteBootcamp(BOOTCAMP_ID)

        Mockito.verify(bootcampManager).delete(bootcamp, NOW)
    }

    private fun lockedBootcamp(reviewStatus: ReviewStatus?): Bootcamp {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcamp.reviewStatus).thenReturn(reviewStatus)
        Mockito.`when`(bootcampReader.readForUpdate(BOOTCAMP_ID)).thenReturn(bootcamp)
        return bootcamp
    }

    companion object {
        private const val BOOTCAMP_ID = 1L
        private val NOW = LocalDateTime.of(2026, 8, 27, 12, 0)
    }
}
