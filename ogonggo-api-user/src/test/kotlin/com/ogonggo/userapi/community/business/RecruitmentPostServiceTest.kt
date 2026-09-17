package com.ogonggo.userapi.community.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.community.implement.dto.RecruitmentPostDraftAppendDto
import com.ogonggo.core.community.implement.RecruitmentPostAppender
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.PostMetricDto
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.community.implement.RecruitmentPostPage
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.implement.dto.RecruitmentPostUpdateDto
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecruitmentPostServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val postAppender = Mockito.mock(RecruitmentPostAppender::class.java)
    private val postManager = Mockito.mock(RecruitmentPostManager::class.java)
    private val postReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val postBookmarkReader = Mockito.mock(RecruitmentPostBookmarkReader::class.java)
    private val postMetricReader = Mockito.mock(PostMetricReader::class.java)
    private val postMetricManager = Mockito.mock(PostMetricManager::class.java)
    private val applicationReader = Mockito.mock(RecruitmentPostApplicationReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val contentValidator = LexicalEditorStateValidator(ObjectMapper())
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZONE)
    private val service = RecruitmentPostService(
        userReader,
        postAppender,
        postManager,
        postReader,
        postBookmarkReader,
        postMetricReader,
        postMetricManager,
        contentValidator,
        imageAssetManager,
        eventPublisher,
        clock,
        userProfileReader,
        applicationReader,
    )

    @Test
    fun `공개 모집글 상세 조회 결과를 변환한다`() {
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(postReader.readPublished(12L)).thenReturn(post)
        stubPublicPost(post)
        Mockito.`when`(postMetricReader.read(12L)).thenReturn(PostMetricDto(viewCount = 0, commentCount = 0, bookmarkCount = 7))

        val result = service.getRecruitmentPost(12L)

        assertEquals(12L, result.id)
        assertEquals(USER_ID, result.author.userId)
        assertEquals(listOf(RecruitmentPosition.BACKEND), result.positions)
        assertEquals(ContactMethod.EMAIL, result.contact.method)
        assertEquals(EDITOR_STATE_JSON, result.content)
        assertEquals(null, result.author.nickname)
        assertEquals(null, result.author.profileImageUrl)
        assertFalse(result.bookmarked)
        assertEquals(0L, result.bookmarkCount)
        Mockito.verify(postReader).readPublished(12L)
        Mockito.verifyNoInteractions(postBookmarkReader)
        Mockito.verify(eventPublisher).publishEvent(RecruitmentPostViewedEvent(12L))
    }

    @Test
    fun `로그인 사용자가 북마크한 모집글 상세에는 bookmarked true를 반환한다`() {
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(postReader.readPublished(12L)).thenReturn(post)
        Mockito.`when`(postMetricReader.read(12L)).thenReturn(PostMetricDto(viewCount = 0, commentCount = 0, bookmarkCount = 7))
        Mockito.`when`(postBookmarkReader.readBookmarkedPostIds(USER_ID, listOf(12L))).thenReturn(setOf(12L))
        stubPublicPost(post)

        val result = service.getRecruitmentPost(USER_ID, 12L)

        assertTrue(result.bookmarked)
        assertEquals(7L, result.bookmarkCount)
        Mockito.verify(postBookmarkReader).readBookmarkedPostIds(USER_ID, listOf(12L))
    }

    @Test
    fun `모집글 목록의 작성자 프로필을 한 번에 조회하고 북마크 여부를 표시한다`() {
        val firstPost = Mockito.mock(RecruitmentPost::class.java)
        val secondPost = Mockito.mock(RecruitmentPost::class.java)
        val query = RecruitmentPostListQuery(
            page = 0,
            size = 10,
            sortType = RecruitmentPostSortType.LATEST,
            filter = RecruitmentPostListFilter(),
        )
        Mockito.`when`(
            postReader.readPublishedPage(
                page = 0,
                size = 10,
                filter = RecruitmentPostListFilter(),
                sortType = RecruitmentPostSortType.LATEST,
            ),
        ).thenReturn(
            RecruitmentPostPage(
                posts = listOf(firstPost, secondPost),
                page = 0,
                size = 10,
                totalElements = 2,
                totalPages = 1,
            ),
        )
        Mockito.`when`(postMetricReader.readAll(listOf(12L, 13L))).thenReturn(
            mapOf(12L to PostMetricDto(viewCount = 0, commentCount = 0, bookmarkCount = 4)),
        )
        Mockito.`when`(applicationReader.countByPostIds(listOf(12L, 13L))).thenReturn(mapOf(12L to 3L))
        Mockito.`when`(postBookmarkReader.readBookmarkedPostIds(USER_ID, listOf(12L, 13L))).thenReturn(setOf(12L))
        Mockito.`when`(userProfileReader.readAll(listOf(USER_ID, OTHER_AUTHOR_ID))).thenReturn(
            mapOf(
                USER_ID to profile(nickname = "홍길동", profileImageUrl = "https://cdn.example.com/17.png"),
                OTHER_AUTHOR_ID to profile(nickname = "김길동", profileImageUrl = null),
            ),
        )
        stubPublicPost(firstPost, postId = 12L, authorUserId = USER_ID)
        stubPublicPost(secondPost, postId = 13L, authorUserId = OTHER_AUTHOR_ID)

        val result = service.getRecruitmentPosts(USER_ID, query)

        assertEquals(2, result.items.size)
        assertTrue(result.items.first().bookmarked)
        assertEquals(4L, result.items.first().bookmarkCount)
        assertEquals(3L, result.items.first().applicationCount)
        assertFalse(result.items.last().bookmarked)
        assertEquals("홍길동", result.items.first().author.nickname)
        assertEquals("https://cdn.example.com/17.png", result.items.first().author.profileImageUrl)
        assertEquals("김길동", result.items.last().author.nickname)
        assertEquals(null, result.items.last().author.profileImageUrl)
        Mockito.verify(userProfileReader).readAll(listOf(USER_ID, OTHER_AUTHOR_ID))
        Mockito.verify(userProfileReader, Mockito.never()).read(Mockito.anyLong())
        Mockito.verify(postBookmarkReader).readBookmarkedPostIds(USER_ID, listOf(12L, 13L))
    }

    @Test
    fun `활성 사용자가 생성한 모집글의 식별자를 반환한다`() {
        val command = createCommand(authorUserId = 999L)
        val savedPost = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postAppender.append(command.copy(authorUserId = USER_ID))).thenReturn(savedPost)
        Mockito.`when`(savedPost.id).thenReturn(12L)

        val postId = service.create(USER_ID, command)

        assertEquals(12L, postId)
        Mockito.verify(postAppender).append(command.copy(authorUserId = USER_ID))
        Mockito.verify(postMetricManager).initialize(12L)
    }

    @Test
    fun `모집글 생성 전에 Lexical EditorState JSON을 검증한다`() {
        val command = createCommand(authorUserId = USER_ID)
        val savedPost = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        val anyCommand = Mockito.any(RecruitmentPostAppendDto::class.java) ?: command
        Mockito.`when`(postAppender.append(anyCommand)).thenReturn(savedPost)
        Mockito.`when`(savedPost.id).thenReturn(12L)

        service.create(USER_ID, command)

        Mockito.verify(postAppender).append(
            command.copy(
                authorUserId = USER_ID,
                content = EDITOR_STATE_JSON,
            ),
        )
    }

    @Test
    fun `제목만 입력한 임시저장 모집글을 생성한다`() {
        // given
        val command = RecruitmentPostDraftAppendDto(
            authorUserId = USER_ID,
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
        val savedPost = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postAppender.appendDraft(command)).thenReturn(savedPost)
        Mockito.`when`(savedPost.id).thenReturn(12L)

        // when
        val postId = service.createDraft(USER_ID, command)

        // then
        assertEquals(12L, postId)
        Mockito.verify(postAppender).appendDraft(command)
        Mockito.verify(imageAssetManager).syncPostImages(USER_ID, 12L, null, null, NOW)
    }

    @Test
    fun `저장 명령이 임시저장 분기로 전달된다`() {
        val command = RecruitmentPostDraftAppendDto(
            authorUserId = USER_ID,
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
        val savedPost = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postAppender.appendDraft(command)).thenReturn(savedPost)
        Mockito.`when`(savedPost.id).thenReturn(12L)

        val postId = service.save(USER_ID, RecruitmentPostSaveCommand.Draft(command))

        assertEquals(12L, postId)
        Mockito.verify(postAppender).appendDraft(command)
    }

    @Test
    fun `임시저장 모집글을 게시 저장하면 갱신 후 게시하고 지표를 초기화한다`() {
        val post = Mockito.mock(RecruitmentPost::class.java)
        val command = updateCommand()
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)
        Mockito.`when`(post.publicationStatus).thenReturn(PublicationStatus.DRAFT)

        service.update(
            userId = USER_ID,
            postId = 12L,
            command = command,
            saveMode = RecruitmentPostSaveMode.PUBLISH,
        )

        Mockito.verify(postManager).updateDraft(post, command)
        Mockito.verify(postManager).publish(post)
        Mockito.verify(postMetricManager).initialize(12L)
    }

    @Test
    fun `작성자가 모집글을 수정하면 본문을 검증하고 기존 모집글을 갱신한다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        val command = updateCommand()
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)

        // when
        service.update(USER_ID, 12L, command)

        // then
        Mockito.verify(postManager).update(
            post,
            command.copy(content = EDITOR_STATE_JSON),
            LocalDate.of(2026, 9, 11),
        )
        Mockito.verify(postReader).readOwnedForUpdate(USER_ID, 12L)
    }

    @Test
    fun `작성자가 임시저장 모집글을 수정하면 임시저장 전용 갱신을 수행한다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        val command = updateCommand().copy(content = null)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)
        Mockito.`when`(post.publicationStatus).thenReturn(PublicationStatus.DRAFT)

        // when
        service.update(USER_ID, 12L, command)

        // then
        Mockito.verify(postManager).updateDraft(post, command)
        Mockito.verifyNoMoreInteractions(postManager)
        Mockito.verify(imageAssetManager).syncPostImages(USER_ID, 12L, "", null, NOW)
    }

    @Test
    fun `작성자가 모집글을 삭제하면 삭제용 소유 조회와 삭제를 수행한다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForDelete(USER_ID, 12L)).thenReturn(post)

        // when
        service.delete(USER_ID, 12L)

        // then
        Mockito.verify(postReader).readOwnedForDelete(USER_ID, 12L)
        Mockito.verify(postManager).delete(post, NOW)
    }

    @Test
    fun `작성자가 모집글을 마감하면 소유 모집글을 잠금 조회하고 마감한다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)

        // when
        service.close(USER_ID, 12L)

        // then
        Mockito.verify(postReader).readOwnedForUpdate(USER_ID, 12L)
        Mockito.verify(postManager).close(post, NOW)
    }

    @Test
    fun `작성자가 모집글을 재모집하면 소유 모집글을 잠금 조회하고 재모집한다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.CLOSED)
        Mockito.`when`(post.recruitmentEndDate).thenReturn(LocalDate.of(2026, 9, 30))

        // when
        service.reopen(USER_ID, 12L)

        // then
        Mockito.verify(postReader).readOwnedForUpdate(USER_ID, 12L)
        Mockito.verify(postManager).reopen(post)
    }

    @Test
    fun `이미 모집 중인 글은 종료일이 지나도 재모집 요청에 성공한다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.RECRUITING)
        Mockito.`when`(post.recruitmentEndDate).thenReturn(LocalDate.of(2026, 9, 10))

        // when
        service.reopen(USER_ID, 12L)

        // then
        Mockito.verify(postManager).reopen(post)
    }

    @Test
    fun `종료일이 현재보다 미래가 아니면 명시적으로 재모집할 수 없다`() {
        // given
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(post)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.CLOSED)
        Mockito.`when`(post.recruitmentEndDate).thenReturn(LocalDate.of(2026, 9, 11))

        // when
        val exception = assertThrows(ConflictException::class.java) {
            service.reopen(USER_ID, 12L)
        }

        // then
        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_REOPEN_END_DATE_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(postManager)
    }

    private fun activeUser(): UserAccountDto = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    private fun stubPublicPost(
        post: RecruitmentPost,
        postId: Long = 12L,
        authorUserId: Long = USER_ID,
    ) {
        Mockito.`when`(post.id).thenReturn(postId)
        Mockito.`when`(post.authorUserId).thenReturn(authorUserId)
        Mockito.`when`(post.title).thenReturn("스터디 모집")
        Mockito.`when`(post.recruitmentType).thenReturn(RecruitmentType.STUDY)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.RECRUITING)
        Mockito.`when`(post.recruitmentStartDate).thenReturn(LocalDate.of(2026, 9, 1))
        Mockito.`when`(post.recruitmentEndDate).thenReturn(LocalDate.of(2026, 9, 30))
        Mockito.`when`(post.progressMethod).thenReturn(ProgressMethod.ONLINE)
        Mockito.`when`(post.capacity).thenReturn(6)
        Mockito.`when`(post.activityDurationMonths).thenReturn(3)
        Mockito.`when`(post.technologyStacks).thenReturn(mutableListOf("Kotlin", "Spring"))
        Mockito.`when`(post.positions).thenReturn(mutableListOf(RecruitmentPosition.BACKEND))
        Mockito.`when`(post.contactMethod).thenReturn(ContactMethod.EMAIL)
        Mockito.`when`(post.contactValue).thenReturn("team@example.com")
        Mockito.`when`(post.summary).thenReturn("함께 공부할 분을 모집합니다.")
        Mockito.`when`(post.content).thenReturn(EDITOR_STATE_JSON)
        Mockito.`when`(post.eligibilityAndSelectionProcess).thenReturn(null)
    }

    private fun profile(nickname: String?, profileImageUrl: String?) = UserProfileDto(
        name = null,
        email = null,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
        university = null,
        major = null,
        grade = null,
        wishField = null,
        wishJob = null,
        wishIndustry = null,
        wishEmploymentType = null,
        wishCompany = null,
    )

    private fun createCommand(authorUserId: Long): RecruitmentPostAppendDto = RecruitmentPostAppendDto(
        authorUserId = authorUserId,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = EDITOR_STATE_JSON,
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )

    private fun updateCommand() = RecruitmentPostUpdateDto(
        title = "수정된 모집글",
        recruitmentType = RecruitmentType.STUDY,
        capacity = 6,
        progressMethod = ProgressMethod.HYBRID,
        activityDurationMonths = 4,
        technologyStacks = listOf("Kotlin"),
        summary = "수정된 소개",
        content = EDITOR_STATE_JSON,
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 2),
        recruitmentEndDate = LocalDate.of(2026, 10, 1),
        positions = listOf(RecruitmentPosition.FRONTEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "updated@example.com",
    )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_AUTHOR_ID = 18L
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 11, 9, 0)
        private val EDITOR_STATE_JSON = """
            {"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"모집 상세 내용입니다.","type":"text","version":1}],"direction":null,"format":"","indent":0,"textFormat":0,"type":"paragraph","version":1}],"direction":null,"format":"","indent":0,"type":"root","version":1}}
        """.trimIndent()
    }
}
