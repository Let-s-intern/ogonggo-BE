package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampContentEditDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.image.implement.ImageAssetManager
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AdminBootcampServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampManager = Mockito.mock(BootcampManager::class.java)
    private val bootcampMetricReader = Mockito.mock(BootcampMetricReader::class.java)
    private val bootcampContentReader = Mockito.mock(BootcampContentReader::class.java)
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminBootcampService(
        bootcampReader,
        bootcampManager,
        bootcampMetricReader,
        bootcampContentReader,
        imageAssetManager,
        clock,
    )

    @Test
    fun `승인과 숨김을 함께 보내면 승인한 뒤 숨기고 내용을 고친다`() {
        val bootcamp = lockedBootcamp(reviewStatus = ContentReviewStatus.REJECTED)
        val contents = mapOf(BootcampContentField.ELIGIBILITY_AND_SELECTION_PROCESS to null)

        service.updateBootcamp(
            BOOTCAMP_ID,
            AdminBootcampUpdateCommand(
                visibility = AdminContentVisibility.HIDDEN,
                reviewStatus = ContentReviewStatus.APPROVED,
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
        lockedBootcamp(reviewStatus = ContentReviewStatus.PENDING)

        service.updateBootcamp(BOOTCAMP_ID, AdminBootcampUpdateCommand(reviewStatus = ContentReviewStatus.PENDING))

        Mockito.verifyNoInteractions(bootcampManager)
    }

    @Test
    fun `노출 일괄 변경은 잠가 읽은 부트캠프를 같은 노출로 바꾸고 이미 같은 노출이면 건드리지 않는다`() {
        val hidden = bootcampWithStatus(BootcampPublicationStatus.HIDDEN)
        val published = bootcampWithStatus(BootcampPublicationStatus.PUBLISHED)
        Mockito.`when`(bootcampReader.readAllForUpdate(listOf(3L, 1L))).thenReturn(listOf(hidden, published))

        service.changeVisibilities(
            AdminBootcampVisibilityChangeCommand(listOf(3L, 1L), AdminContentVisibility.VISIBLE),
        )

        Mockito.verify(bootcampManager).publish(hidden)
        Mockito.verifyNoMoreInteractions(bootcampManager)
    }

    @Test
    fun `삭제는 이미 삭제된 부트캠프까지 잠가 찾아 멱등하게 처리한다`() {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readForDelete(BOOTCAMP_ID)).thenReturn(bootcamp)

        service.deleteBootcamp(BOOTCAMP_ID)

        Mockito.verify(bootcampManager).delete(bootcamp, NOW)
    }

    @Test
    fun `전체 수정은 마감된 대리 등록 과정을 다시 모집 중으로 두고 이미지를 회원과 운영자 소유로 첨부한다`() {
        // given
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcamp.status).thenReturn(BootcampRecruitmentStatus.CLOSED)
        Mockito.`when`(bootcamp.ownerUserId).thenReturn(MEMBER_ID)
        Mockito.`when`(bootcampReader.readForUpdate(BOOTCAMP_ID)).thenReturn(bootcamp)
        val content = replaceContent()

        // when
        service.replaceBootcamp(BOOTCAMP_ID, AdminBootcampReplaceCommand(content, BootcampRecruitmentStatus.RECRUITING), ADMIN_ID)

        // then
        val order = Mockito.inOrder(bootcampManager)
        order.verify(bootcampManager).update(bootcamp, content)
        order.verify(bootcampManager).startRecruitment(bootcamp)
        Mockito.verifyNoMoreInteractions(bootcampManager)
        Mockito.verify(imageAssetManager).attachImageUrls(MEMBER_ID, listOf(LOGO_URL, null))
        Mockito.verify(imageAssetManager).attachImageUrls(ADMIN_ID, listOf(LOGO_URL, null))
    }

    @Test
    fun `전체 수정으로 마감하면 지금을 마감 일시로 남기고 회원 없는 수집 과정은 운영자 소유로만 첨부한다`() {
        // given
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcamp.status).thenReturn(BootcampRecruitmentStatus.RECRUITING)
        Mockito.`when`(bootcamp.ownerUserId).thenReturn(null)
        Mockito.`when`(bootcampReader.readForUpdate(BOOTCAMP_ID)).thenReturn(bootcamp)
        val content = replaceContent()

        // when
        service.replaceBootcamp(BOOTCAMP_ID, AdminBootcampReplaceCommand(content, BootcampRecruitmentStatus.CLOSED), ADMIN_ID)

        // then
        Mockito.verify(bootcampManager).close(bootcamp, NOW)
        Mockito.verify(imageAssetManager).attachImageUrls(ADMIN_ID, listOf(LOGO_URL, null))
        Mockito.verifyNoMoreInteractions(imageAssetManager)
    }

    private fun replaceContent(): BootcampUpdateDto = BootcampUpdateDto(
        companyName = "오공고 교육",
        title = "백엔드 부트캠프",
        programType = "부트캠프",
        operationType = BootcampOperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
        programStartDate = LocalDate.of(2026, 9, 1),
        programEndDate = LocalDate.of(2026, 12, 31),
        tuitionType = BootcampTuitionType.FREE,
        representativeImageUrl = null,
        shortDescription = "한 줄 소개",
        content = "상세 내용",
        logoUrl = LOGO_URL,
        instructorInfo = "강사 정보",
        applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
    )

    private fun bootcampWithStatus(publicationStatus: BootcampPublicationStatus): Bootcamp =
        Mockito.mock(Bootcamp::class.java).also { bootcamp ->
            Mockito.`when`(bootcamp.publicationStatus).thenReturn(publicationStatus)
        }

    private fun lockedBootcamp(reviewStatus: ContentReviewStatus?): Bootcamp {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcamp.reviewStatus).thenReturn(reviewStatus)
        Mockito.`when`(bootcampReader.readForUpdate(BOOTCAMP_ID)).thenReturn(bootcamp)
        return bootcamp
    }

    companion object {
        private const val BOOTCAMP_ID = 1L
        private const val MEMBER_ID = 7L
        private const val ADMIN_ID = 9L
        private const val LOGO_URL = "https://cdn.example.com/logo.png"
        private val NOW = LocalDateTime.of(2026, 8, 27, 12, 0)
    }
}
