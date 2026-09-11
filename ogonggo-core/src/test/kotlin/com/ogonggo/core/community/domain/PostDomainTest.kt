package com.ogonggo.core.community.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import com.ogonggo.core.community.implement.PostUpdateCommand

@DisplayName("모집글 도메인")
class PostDomainTest {

    @Test
    @DisplayName("모집글을 생성하면 공개 및 모집 중 상태로 시작한다")
    fun createPost() {
        // given
        val post = createPostFixture()

        // when
        val publicationStatus = post.publicationStatus
        val recruitmentStatus = post.recruitmentStatus

        // then
        assertEquals(PublicationStatus.PUBLISHED, publicationStatus)
        assertEquals(RecruitmentStatus.RECRUITING, recruitmentStatus)
    }

    @Test
    @DisplayName("모집 기간이 뒤집히면 생성할 수 없다")
    fun rejectReversedRecruitmentPeriod() {
        // given
        val startDate = LocalDate.of(2026, 9, 10)
        val endDate = LocalDate.of(2026, 9, 9)

        // when
        val exception = assertThrows(IllegalArgumentException::class.java) {
            createPostFixture(recruitmentStartDate = startDate, recruitmentEndDate = endDate)
        }

        // then
        assertEquals("모집 시작일은 마감일보다 늦을 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("모집 포지션은 하나 이상이고 중복될 수 없다")
    fun validatePositions() {
        // given
        val emptyPositions = emptyList<RecruitmentPosition>()
        val duplicatePositions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.BACKEND)

        // when
        val emptyException = assertThrows(IllegalArgumentException::class.java) {
            createPostFixture(positions = emptyPositions)
        }
        val duplicateException = assertThrows(IllegalArgumentException::class.java) {
            createPostFixture(positions = duplicatePositions)
        }

        // then
        assertEquals("모집 포지션은 하나 이상이어야 합니다.", emptyException.message)
        assertEquals("중복된 모집 포지션은 등록할 수 없습니다.", duplicateException.message)
    }

    @Test
    @DisplayName("모집글을 마감하면 마감 상태와 마감 시각을 기록한다")
    fun closePost() {
        // given
        val post = createPostFixture()
        val closedAt = LocalDateTime.of(2026, 9, 30, 23, 59)

        // when
        post.close(closedAt)

        // then
        assertEquals(RecruitmentStatus.CLOSED, post.recruitmentStatus)
        assertEquals(closedAt, post.closedAt)
    }

    @Test
    @DisplayName("모집글을 수정하면 입력한 내용만 바뀌고 모집 상태는 유지된다")
    fun updatePost() {
        // given
        val post = createPostFixture()
        val updateCommand = PostUpdateCommand(
            title = "수정된 스터디 모집",
            recruitmentType = RecruitmentType.STUDY,
            capacity = 6,
            progressMethod = ProgressMethod.HYBRID,
            activityDurationMonths = 4,
            technologyStacks = listOf("Kotlin", "Spring Boot"),
            summary = "수정된 한 줄 소개입니다.",
            content = "<p>수정된 모집 내용입니다.</p>",
            eligibilityAndSelectionProcess = "수정된 지원 자격",
            recruitmentStartDate = LocalDate.of(2026, 9, 2),
            recruitmentEndDate = LocalDate.of(2026, 10, 1),
            positions = listOf(RecruitmentPosition.FRONTEND),
            contactMethod = ContactMethod.OPEN_KAKAO,
            contactValue = "https://open.kakao.com/o/updated",
        )

        // when
        post.update(
            title = updateCommand.title,
            recruitmentType = updateCommand.recruitmentType,
            capacity = updateCommand.capacity,
            progressMethod = updateCommand.progressMethod,
            activityDurationMonths = updateCommand.activityDurationMonths,
            technologyStacks = updateCommand.technologyStacks,
            summary = updateCommand.summary,
            content = updateCommand.content,
            eligibilityAndSelectionProcess = updateCommand.eligibilityAndSelectionProcess,
            recruitmentStartDate = updateCommand.recruitmentStartDate,
            recruitmentEndDate = updateCommand.recruitmentEndDate,
            positions = updateCommand.positions,
            contactMethod = updateCommand.contactMethod,
            contactValue = updateCommand.contactValue,
        )

        // then
        assertEquals("수정된 스터디 모집", post.title)
        assertEquals(RecruitmentType.STUDY, post.recruitmentType)
        assertEquals(6, post.capacity)
        assertEquals(listOf("Kotlin", "Spring Boot"), post.technologyStacks)
        assertEquals(listOf(RecruitmentPosition.FRONTEND), post.positions)
        assertEquals(RecruitmentStatus.RECRUITING, post.recruitmentStatus)
        assertEquals(null, post.closedAt)
    }

    @Test
    @DisplayName("수정할 모집 기간이 뒤집히면 수정할 수 없다")
    fun rejectReversedRecruitmentPeriodOnUpdate() {
        // given
        val post = createPostFixture()

        // when
        val exception = assertThrows(IllegalArgumentException::class.java) {
            post.update(
                title = post.title,
                recruitmentType = post.recruitmentType,
                capacity = post.capacity,
                progressMethod = post.progressMethod,
                activityDurationMonths = post.activityDurationMonths,
                technologyStacks = post.technologyStacks,
                summary = post.summary,
                content = post.content,
                eligibilityAndSelectionProcess = post.eligibilityAndSelectionProcess,
                recruitmentStartDate = LocalDate.of(2026, 9, 10),
                recruitmentEndDate = LocalDate.of(2026, 9, 9),
                positions = post.positions,
                contactMethod = post.contactMethod,
                contactValue = post.contactValue,
            )
        }

        // then
        assertEquals("모집 시작일은 마감일보다 늦을 수 없습니다.", exception.message)
    }

    private fun createPostFixture(
        recruitmentStartDate: LocalDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate: LocalDate = LocalDate.of(2026, 9, 30),
        positions: List<RecruitmentPosition> = listOf(RecruitmentPosition.BACKEND),
    ): Post = Post(
        authorUserId = 1L,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "<p>모집 상세 내용입니다.</p>",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )
}
