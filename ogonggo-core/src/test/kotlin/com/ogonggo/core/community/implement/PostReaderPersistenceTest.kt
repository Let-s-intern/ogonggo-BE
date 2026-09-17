package com.ogonggo.core.community.implement

import com.ogonggo.core.community.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostQueryRepository
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    RecruitmentPostAppender::class,
    RecruitmentPostManager::class,
    RecruitmentPostReader::class,
    RecruitmentPostQueryRepository::class,
    PostMetricManager::class,
    PostMetricRegistrar::class,
)
internal class PostReaderPersistenceTest @Autowired constructor(
    private val postAppender: RecruitmentPostAppender,
    private val postManager: RecruitmentPostManager,
    private val postReader: RecruitmentPostReader,
    private val postRepository: RecruitmentPostJpaRepository,
    private val postMetricManager: PostMetricManager,
) {

    @Test
    fun `공개 모집글을 식별자로 조회한다`() {
        val savedPost = postAppender.append(createCommand(title = "상세 모집글", recruitmentType = RecruitmentType.STUDY))

        val result = postReader.readPublished(checkNotNull(savedPost.id))

        assertEquals("상세 모집글", result.title)
        assertEquals(PublicationStatus.PUBLISHED, result.publicationStatus)
    }

    @Test
    fun `존재하지 않거나 비공개인 모집글은 찾을 수 없음 예외를 던진다`() {
        val hiddenPost = postRepository.save(createPost(title = "비공개 모집", publicationStatus = PublicationStatus.HIDDEN))

        val missingException = assertThrows(EntityNotFoundException::class.java) {
            postReader.readPublished(999L)
        }
        val hiddenException = assertThrows(EntityNotFoundException::class.java) {
            postReader.readPublished(checkNotNull(hiddenPost.id))
        }

        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND, missingException.errorCode)
        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND, hiddenException.errorCode)
    }

    @Test
    fun `공개 모집글을 모집 구분으로 필터링하고 페이지로 조회한다`() {
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentType.STUDY))
        postAppender.append(createCommand(title = "사이드 프로젝트 모집", recruitmentType = RecruitmentType.SIDE_PROJECT))
        postRepository.save(createPost(title = "비공개 모집", publicationStatus = PublicationStatus.HIDDEN))

        val result = postReader.readPublishedPage(
            page = 0,
            size = 1,
            filter = RecruitmentPostListFilter(recruitmentTypes = setOf(RecruitmentType.STUDY)),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(1, result.posts.size)
        assertEquals("스터디 모집", result.posts.single().title)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `모집 포지션 필터는 하나라도 일치하는 모집글을 반환한다`() {
        postAppender.append(
            createCommand(
                title = "백엔드 모집",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
            ),
        )
        postAppender.append(
            createCommand(
                title = "프론트엔드 모집",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                positions = listOf(RecruitmentPosition.FRONTEND),
            ),
        )

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(positions = setOf(RecruitmentPosition.DESIGN)),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(listOf("백엔드 모집"), result.posts.map(RecruitmentPost::title))
    }

    @Test
    fun `필터를 선택하지 않으면 공개 모집글 전체를 반환한다`() {
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentType.STUDY))
        postAppender.append(createCommand(title = "사이드 프로젝트 모집", recruitmentType = RecruitmentType.SIDE_PROJECT))
        postRepository.save(createPost(title = "비공개 모집", publicationStatus = PublicationStatus.HIDDEN))

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(setOf("스터디 모집", "사이드 프로젝트 모집"), result.posts.map(RecruitmentPost::title).toSet())
        assertEquals(2, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `삭제된 모집글은 공개 단건과 목록에서 조회하지 않는다`() {
        val deletedPost = postAppender.append(createCommand("삭제될 모집글", RecruitmentType.STUDY))
        postManager.delete(deletedPost, LocalDateTime.of(2026, 9, 11, 9, 0))
        postRepository.flush()

        assertThrows(EntityNotFoundException::class.java) {
            postReader.readPublished(checkNotNull(deletedPost.id))
        }

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(emptyList<RecruitmentPost>(), result.posts)
        assertEquals(0, result.totalElements)
        assertEquals(0, result.totalPages)
    }

    @Test
    fun `삭제용 조회는 삭제된 본인 모집글을 찾고 다른 사용자의 글은 찾지 않는다`() {
        val deletedPost = postAppender.append(createCommand("삭제 대상", RecruitmentType.STUDY))
        val postId = checkNotNull(deletedPost.id)
        postManager.delete(deletedPost, LocalDateTime.of(2026, 9, 11, 9, 0))
        postRepository.flush()

        assertEquals(postId, postReader.readOwnedForDelete(1L, postId).id)
        assertThrows(EntityNotFoundException::class.java) {
            postReader.readOwnedForDelete(2L, postId)
        }
    }

    @Test
    fun `최신순 페이지로 다음 목록을 중복 없이 조회한다`() {
        postAppender.append(createCommand(title = "첫 번째", recruitmentType = RecruitmentType.STUDY))
        postAppender.append(createCommand(title = "두 번째", recruitmentType = RecruitmentType.STUDY))
        postAppender.append(createCommand(title = "세 번째", recruitmentType = RecruitmentType.STUDY))

        val first = postReader.readPublishedPage(
            page = 0,
            size = 2,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )
        val second = postReader.readPublishedPage(
            page = 1,
            size = 2,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(0, first.page)
        assertEquals(3, first.totalElements)
        assertEquals(2, first.totalPages)
        assertEquals(listOf("첫 번째"), second.posts.map(RecruitmentPost::title))
        assertEquals(1, second.page)
        assertEquals(3, second.totalElements)
        assertEquals(2, second.totalPages)
    }

    @Test
    fun `조회수순 목록은 조회수가 높은 모집글부터 중복 없이 반환한다`() {
        // given
        val zeroViewPost = postAppender.append(
            createCommand(
                title = "조회수 0",
                recruitmentType = RecruitmentType.STUDY,
                positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
            ),
        )
        val oneViewPost = postAppender.append(
            createCommand(
                title = "조회수 1",
                recruitmentType = RecruitmentType.STUDY,
                positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
            ),
        )
        val twoViewPost = postAppender.append(
            createCommand(
                title = "조회수 2",
                recruitmentType = RecruitmentType.STUDY,
                positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
            ),
        )
        val now = LocalDateTime.of(2026, 9, 17, 13, 0)
        postMetricManager.initialize(checkNotNull(oneViewPost.id))
        postMetricManager.initialize(checkNotNull(twoViewPost.id))
        postMetricManager.increaseViewCount(checkNotNull(oneViewPost.id), now)
        postMetricManager.increaseViewCount(checkNotNull(twoViewPost.id), now)
        postMetricManager.increaseViewCount(checkNotNull(twoViewPost.id), now.plusSeconds(1))

        // when
        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(
                positions = setOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
            ),
            sortType = RecruitmentPostSortType.VIEW_COUNT,
        )

        // then
        assertEquals(listOf("조회수 2", "조회수 1", "조회수 0"), result.posts.map(RecruitmentPost::title))
        assertEquals(3, result.posts.map(RecruitmentPost::id).distinct().size)
        assertEquals(checkNotNull(zeroViewPost.id), result.posts.last().id)
    }

    @Test
    fun `댓글수순 목록은 댓글수가 높은 모집글부터 반환한다`() {
        // given
        val zeroCommentPost = postAppender.append(createCommand("댓글수 0", RecruitmentType.STUDY))
        val oneCommentPost = postAppender.append(createCommand("댓글수 1", RecruitmentType.STUDY))
        val twoCommentPost = postAppender.append(createCommand("댓글수 2", RecruitmentType.STUDY))
        val now = LocalDateTime.of(2026, 9, 17, 13, 0)
        postMetricManager.initialize(checkNotNull(oneCommentPost.id))
        postMetricManager.initialize(checkNotNull(twoCommentPost.id))
        postMetricManager.increaseCommentCount(checkNotNull(oneCommentPost.id), now)
        postMetricManager.increaseCommentCount(checkNotNull(twoCommentPost.id), now)
        postMetricManager.increaseCommentCount(checkNotNull(twoCommentPost.id), now.plusSeconds(1))

        // when
        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.COMMENT_COUNT,
        )

        // then
        assertEquals(listOf("댓글수 2", "댓글수 1", "댓글수 0"), result.posts.map(RecruitmentPost::title))
        assertEquals(checkNotNull(zeroCommentPost.id), result.posts.last().id)
    }

    @Test
    fun `페이지 번호가 음수면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            postReader.readPublishedPage(
                page = -1,
                size = 10,
                filter = RecruitmentPostListFilter(),
                sortType = RecruitmentPostSortType.LATEST,
            )
        }
    }

    private fun createCommand(
        title: String,
        recruitmentType: RecruitmentType,
        positions: List<RecruitmentPosition> = listOf(RecruitmentPosition.BACKEND),
    ) = RecruitmentPostAppendDto(
        authorUserId = 1L,
        title = title,
        recruitmentType = recruitmentType,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "{\"root\":{\"children\":[]}}",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = positions,
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )

    private fun createPost(
        title: String,
        publicationStatus: PublicationStatus,
    ) = RecruitmentPost(
        authorUserId = 1L,
        title = title,
        recruitmentType = RecruitmentType.STUDY,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "{\"root\":{\"children\":[]}}",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
        publicationStatus = publicationStatus,
    )
}
