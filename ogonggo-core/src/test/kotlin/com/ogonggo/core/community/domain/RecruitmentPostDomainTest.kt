package com.ogonggo.core.community.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import com.ogonggo.core.community.implement.RecruitmentPostUpdateCommand
import java.time.LocalDateTime

@DisplayName("모집글 도메인")
class RecruitmentPostDomainTest {

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
    @DisplayName("임시저장 모집글은 게시 전 필드를 입력하지 않아도 생성할 수 있다")
    fun createDraftPostWithoutPublishedFields() {
        // when
        val post = RecruitmentPost(
            authorUserId = 1L,
            title = "작성 중인 모집글",
            recruitmentType = null,
            capacity = null,
            progressMethod = null,
            activityDurationMonths = null,
            technologyStacks = emptyList(),
            summary = null,
            content = null,
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = null,
            recruitmentEndDate = null,
            positions = emptyList(),
            contactMethod = null,
            contactValue = null,
            publicationStatus = PublicationStatus.DRAFT,
        )

        // then
        assertEquals(PublicationStatus.DRAFT, post.publicationStatus)
        assertEquals(null, post.recruitmentType)
        assertEquals(null, post.capacity)
        assertEquals(null, post.content)
    }

    @Test
    @DisplayName("모집글을 복사하면 내용은 유지하고 새 임시저장 상태로 시작한다")
    fun copyAsDraft() {
        // given
        val source = createPostFixture()
        source.close(LocalDateTime.of(2026, 9, 30, 23, 59))

        // when
        val copied = source.copyAsDraft()

        // then
        assertEquals(source.title, copied.title)
        assertEquals(source.recruitmentType, copied.recruitmentType)
        assertEquals(source.capacity, copied.capacity)
        assertEquals(source.content, copied.content)
        assertEquals(PublicationStatus.DRAFT, copied.publicationStatus)
        assertEquals(RecruitmentStatus.RECRUITING, copied.recruitmentStatus)
        assertEquals(null, copied.closedAt)
        assertEquals(null, copied.id)
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
    @DisplayName("컬렉션 조회 결과를 변경해도 모집글 내부 상태는 바뀌지 않는다")
    fun protectCollections() {
        // given
        val post = createPostFixture(positions = listOf(RecruitmentPosition.BACKEND))

        // when
        post.technologyStacks.add("JPA")
        post.positions.clear()

        // then
        assertEquals(listOf("Kotlin", "Spring"), post.technologyStacks)
        assertEquals(listOf(RecruitmentPosition.BACKEND), post.positions)
    }

    @Test
    @DisplayName("연락 수단과 맞지 않는 연락처는 생성할 수 없다")
    fun validateContactValue() {
        // when
        val emailException = assertThrows(IllegalArgumentException::class.java) {
            createPostFixture(contactMethod = ContactMethod.EMAIL, contactValue = "not-an-email")
        }
        val kakaoException = assertThrows(IllegalArgumentException::class.java) {
            createPostFixture(contactMethod = ContactMethod.OPEN_KAKAO, contactValue = "not-a-url")
        }

        // then
        assertEquals("이메일 형식으로 입력해 주세요.", emailException.message)
        assertEquals("카카오톡 오픈채팅 링크를 입력해 주세요.", kakaoException.message)
    }

    @Test
    @DisplayName("에디터 JSON이 아닌 본문은 생성할 수 없다")
    fun rejectNonJsonContent() {
        // when
        val exception = assertThrows(IllegalArgumentException::class.java) {
            createPostFixture(content = "<p>HTML 본문</p>")
        }

        // then
        assertEquals("모집글 본문은 올바른 에디터 JSON이어야 합니다.", exception.message)
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
    @DisplayName("마감한 모집글을 재모집하면 모집 중 상태와 마감 시각을 초기화한다")
    fun reopenPost() {
        // given
        val post = createPostFixture()
        val closedAt = LocalDateTime.of(2026, 9, 30, 23, 59)
        post.close(closedAt)

        // when
        post.reopen()

        // then
        assertEquals(RecruitmentStatus.RECRUITING, post.recruitmentStatus)
        assertEquals(null, post.closedAt)
    }

    @Test
    @DisplayName("모집 중인 글을 재모집해도 상태가 바뀌지 않는다")
    fun reopenRecruitingPostIsIdempotent() {
        // given
        val post = createPostFixture()

        // when
        post.reopen()

        // then
        assertEquals(RecruitmentStatus.RECRUITING, post.recruitmentStatus)
        assertEquals(null, post.closedAt)
    }

    @Test
    @DisplayName("임시저장 모집글은 제목을 제외한 필드를 비워서 전체 수정할 수 있다")
    fun updateDraftWithNullableFields() {
        // given
        val post = RecruitmentPost(
            authorUserId = 1L,
            title = "작성 중인 모집글",
            recruitmentType = RecruitmentType.SIDE_PROJECT,
            capacity = 4,
            progressMethod = ProgressMethod.ONLINE,
            activityDurationMonths = 3,
            technologyStacks = listOf("Kotlin"),
            summary = "요약",
            content = "{\"root\":{\"children\":[]}}",
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = LocalDate.of(2026, 9, 1),
            recruitmentEndDate = LocalDate.of(2026, 9, 30),
            positions = listOf(RecruitmentPosition.BACKEND),
            contactMethod = ContactMethod.EMAIL,
            contactValue = "team@example.com",
            publicationStatus = PublicationStatus.DRAFT,
        )

        // when
        post.updateDraft(
            title = "작성 중인 모집글",
            recruitmentType = null,
            capacity = null,
            progressMethod = null,
            activityDurationMonths = null,
            technologyStacks = emptyList(),
            summary = null,
            content = null,
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = null,
            recruitmentEndDate = null,
            positions = emptyList(),
            contactMethod = null,
            contactValue = null,
        )

        // then
        assertEquals(PublicationStatus.DRAFT, post.publicationStatus)
        assertEquals(null, post.recruitmentType)
        assertEquals(null, post.content)
        assertEquals(emptyList<RecruitmentPosition>(), post.positions)
    }

    @Test
    @DisplayName("임시저장 모집글 게시 시 필수값이 없으면 게시하지 않는다")
    fun rejectPublishingIncompleteDraft() {
        // given
        val post = RecruitmentPost(
            authorUserId = 1L,
            title = "작성 중인 모집글",
            recruitmentType = null,
            capacity = null,
            progressMethod = null,
            activityDurationMonths = null,
            technologyStacks = emptyList(),
            summary = null,
            content = null,
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = null,
            recruitmentEndDate = null,
            positions = emptyList(),
            contactMethod = null,
            contactValue = null,
            publicationStatus = PublicationStatus.DRAFT,
        )

        // when
        val exception = assertThrows(IllegalArgumentException::class.java) { post.publish() }

        // then
        assertEquals("모집 구분은 필수입니다.", exception.message)
        assertEquals(PublicationStatus.DRAFT, post.publicationStatus)
    }

    @Test
    @DisplayName("필수값을 채운 임시저장 모집글은 공개 상태로 전환한다")
    fun publishCompletedDraft() {
        // given
        val post = RecruitmentPost(
            authorUserId = 1L,
            title = "작성 중인 모집글",
            recruitmentType = RecruitmentType.SIDE_PROJECT,
            capacity = 4,
            progressMethod = ProgressMethod.ONLINE,
            activityDurationMonths = 3,
            technologyStacks = listOf("Kotlin"),
            summary = "요약",
            content = "{\"root\":{\"children\":[]}}",
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = LocalDate.of(2026, 9, 1),
            recruitmentEndDate = LocalDate.of(2026, 9, 30),
            positions = listOf(RecruitmentPosition.BACKEND),
            contactMethod = ContactMethod.EMAIL,
            contactValue = "team@example.com",
            publicationStatus = PublicationStatus.DRAFT,
        )

        // when
        post.publish()

        // then
        assertEquals(PublicationStatus.PUBLISHED, post.publicationStatus)
    }

    @Test
    @DisplayName("이미 공개된 모집글을 다시 게시해도 공개 상태를 유지한다")
    fun publishPublishedPostIsIdempotent() {
        // given
        val post = createPostFixture()

        // when
        post.publish()

        // then
        assertEquals(PublicationStatus.PUBLISHED, post.publicationStatus)
    }

    @Test
    @DisplayName("모집글을 삭제하면 최초 삭제 시각을 유지한다")
    fun deletePost() {
        // given
        val post = createPostFixture()
        val firstDeletedAt = LocalDateTime.of(2026, 9, 11, 9, 0)

        // when
        post.delete(firstDeletedAt)
        post.delete(firstDeletedAt.plusDays(1))

        // then
        assertEquals(firstDeletedAt, post.deletedAt)
    }

    @Test
    @DisplayName("모집글을 수정하면 입력한 내용만 바뀌고 모집 상태는 유지된다")
    fun updatePost() {
        // given
        val post = createPostFixture()
        val updateCommand = RecruitmentPostUpdateCommand(
            title = "수정된 스터디 모집",
            recruitmentType = RecruitmentType.STUDY,
            capacity = 6,
            progressMethod = ProgressMethod.HYBRID,
            activityDurationMonths = 4,
            technologyStacks = listOf("Kotlin", "Spring Boot"),
            summary = "수정된 한 줄 소개입니다.",
            content = "{\"root\":{\"children\":[]}}",
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
            today = LocalDate.of(2026, 9, 11),
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
                today = LocalDate.of(2026, 9, 11),
            )
        }

        // then
        assertEquals("모집 시작일은 마감일보다 늦을 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("마감된 모집글의 종료일을 미래로 변경하면 재모집한다")
    fun reopenClosedPostWhenEndDateMovesToFuture() {
        // given
        val post = createPostFixture(recruitmentEndDate = LocalDate.of(2026, 9, 10))
        post.close(LocalDateTime.of(2026, 9, 11, 9, 0))

        // when
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
            recruitmentStartDate = post.recruitmentStartDate,
            recruitmentEndDate = LocalDate.of(2026, 9, 30),
            positions = post.positions,
            contactMethod = post.contactMethod,
            contactValue = post.contactValue,
            today = LocalDate.of(2026, 9, 11),
        )

        // then
        assertEquals(RecruitmentStatus.RECRUITING, post.recruitmentStatus)
        assertEquals(null, post.closedAt)
    }

    @Test
    @DisplayName("마감일을 바꾸지 않은 마감 모집글은 수정해도 마감 상태를 유지한다")
    fun keepClosedWhenEndDateDoesNotChange() {
        // given
        val post = createPostFixture(recruitmentEndDate = LocalDate.of(2026, 9, 30))
        val closedAt = LocalDateTime.of(2026, 9, 11, 9, 0)
        post.close(closedAt)

        // when
        post.update(
            title = "내용만 수정한 모집글",
            recruitmentType = post.recruitmentType,
            capacity = post.capacity,
            progressMethod = post.progressMethod,
            activityDurationMonths = post.activityDurationMonths,
            technologyStacks = post.technologyStacks,
            summary = post.summary,
            content = post.content,
            eligibilityAndSelectionProcess = post.eligibilityAndSelectionProcess,
            recruitmentStartDate = post.recruitmentStartDate,
            recruitmentEndDate = post.recruitmentEndDate,
            positions = post.positions,
            contactMethod = post.contactMethod,
            contactValue = post.contactValue,
            today = LocalDate.of(2026, 9, 11),
        )

        // then
        assertEquals(RecruitmentStatus.CLOSED, post.recruitmentStatus)
        assertEquals(closedAt, post.closedAt)
    }

    private fun createPostFixture(
        recruitmentStartDate: LocalDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate: LocalDate = LocalDate.of(2026, 9, 30),
        positions: List<RecruitmentPosition> = listOf(RecruitmentPosition.BACKEND),
        contactMethod: ContactMethod = ContactMethod.EMAIL,
        contactValue: String = "team@example.com",
        content: String = "{\"root\":{\"children\":[]}}",
    ): RecruitmentPost = RecruitmentPost(
        authorUserId = 1L,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = content,
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = contactMethod,
        contactValue = contactValue,
    )
}
