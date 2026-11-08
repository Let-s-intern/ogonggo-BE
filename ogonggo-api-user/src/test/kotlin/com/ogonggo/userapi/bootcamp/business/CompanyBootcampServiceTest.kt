package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CompanyBootcampServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampAppender = Mockito.mock(BootcampAppender::class.java)
    private val bootcampManager = Mockito.mock(BootcampManager::class.java)
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private val service = CompanyBootcampService(
        userReader,
        bootcampReader,
        Mockito.mock(BootcampContentReader::class.java),
        bootcampAppender,
        bootcampManager,
        imageAssetManager,
        Mockito.mock(AdvertisementContractAppender::class.java),
        Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZoneId.of("Asia/Seoul")),
        Mockito.mock(ApplicationEventPublisher::class.java),
    )

    @Test
    fun `등록하면 로고와 대표 이미지로 올린 이미지를 첨부해 정리 작업이 지우지 않게 한다`() {
        // given
        givenAccount(UserRole.COMPANY)
        val saved = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(saved.id).thenReturn(BOOTCAMP_ID)
        Mockito.`when`(bootcampAppender.append(command().copy(ownerUserId = USER_ID))).thenReturn(saved)

        // when
        service.create(USER_ID, command())

        // then
        Mockito.verify(imageAssetManager).attachImageUrls(USER_ID, listOf(LOGO_URL, REPRESENTATIVE_URL))
    }

    @Test
    fun `수정하면 바꾼 로고와 대표 이미지를 첨부한다`() {
        // given
        givenAccount(UserRole.COMPANY)
        Mockito.`when`(bootcampReader.readOwnedForUpdate(USER_ID, BOOTCAMP_ID)).thenReturn(Mockito.mock(Bootcamp::class.java))

        // when
        service.update(USER_ID, BOOTCAMP_ID, updateCommand())

        // then
        Mockito.verify(imageAssetManager).attachImageUrls(USER_ID, listOf(LOGO_URL, REPRESENTATIVE_URL))
    }

    @Test
    fun `기업 회원이 아니면 등록을 막고 이미지도 첨부하지 않는다`() {
        // given
        givenAccount(UserRole.USER)

        // when
        assertThrows(ForbiddenException::class.java) { service.create(USER_ID, command()) }

        // then
        Mockito.verifyNoInteractions(bootcampAppender)
        Mockito.verifyNoInteractions(imageAssetManager)
    }

    private fun givenAccount(role: UserRole) {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(
            UserAccountDto(
                userId = USER_ID,
                letsCareerUserId = null,
                email = "company@example.com",
                status = UserStatus.ACTIVE,
                role = role,
                joinedAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            ),
        )
    }

    private fun command(): BootcampAppendDto = BootcampAppendDto(
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = "개발",
        operationType = BootcampOperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
        programStartDate = LocalDate.of(2026, 10, 1),
        programEndDate = LocalDate.of(2026, 12, 1),
        tuitionType = BootcampTuitionType.FREE,
        representativeImageUrl = REPRESENTATIVE_URL,
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        logoUrl = LOGO_URL,
        applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
    )

    private fun updateCommand(): BootcampUpdateDto = BootcampUpdateDto(
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = "개발",
        operationType = BootcampOperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
        programStartDate = LocalDate.of(2026, 10, 1),
        programEndDate = LocalDate.of(2026, 12, 1),
        tuitionType = BootcampTuitionType.FREE,
        representativeImageUrl = REPRESENTATIVE_URL,
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        logoUrl = LOGO_URL,
        applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
    )

    companion object {
        private const val USER_ID = 17L
        private const val BOOTCAMP_ID = 5L
        private const val LOGO_URL = "https://bucket.example.com/ogonggo/etc/logo.png"
        private const val REPRESENTATIVE_URL = "https://bucket.example.com/ogonggo/etc/cover.png"
    }
}
