package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostImageDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostUpdateDto
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    RecruitmentPostImageManager::class,
    RecruitmentPostImageReader::class,
)
internal class RecruitmentPostImplementPersistenceTest @Autowired constructor(
    private val postAppender: RecruitmentPostAppender,
    private val postManager: RecruitmentPostManager,
    private val postRepository: RecruitmentPostJpaRepository,
    private val imageManager: RecruitmentPostImageManager,
    private val imageReader: RecruitmentPostImageReader,
    private val entityManager: EntityManager,
) {

    @Test
    fun `첨부 사진을 교체하면 새 순서대로 읽히고 뺀 사진은 소프트 삭제한다`() {
        // given
        imageManager.replace(12L, listOf(image("a"), image("b"), image("c")), IMAGE_NOW)
        imageManager.replace(13L, listOf(image("x")), IMAGE_NOW)
        entityManager.flush()

        // when
        imageManager.replace(12L, listOf(image("c"), image("a")), IMAGE_NOW.plusMinutes(1))
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(listOf("c", "a"), imageReader.read(12L).map(RecruitmentPostImageDto::imageId))
        assertEquals(listOf("x"), imageReader.read(13L).map(RecruitmentPostImageDto::imageId))
        val deletedCount = entityManager.createQuery(
            "select count(image) from RecruitmentPostImage image where image.postId = :id and image.deletedAt is not null",
            java.lang.Long::class.java,
        ).setParameter("id", 12L).singleResult
        assertEquals(3L, deletedCount.toLong())
    }

    @Test
    fun `목록 썸네일은 남아 있는 첫 번째 첨부 사진이고 사진이 없는 모집글은 빠진다`() {
        // given
        imageManager.replace(12L, listOf(image("a"), image("b")), IMAGE_NOW)
        imageManager.replace(13L, listOf(image("x")), IMAGE_NOW)
        entityManager.flush()
        imageManager.replace(12L, listOf(image("b"), image("a")), IMAGE_NOW.plusMinutes(1))
        entityManager.flush()
        entityManager.clear()

        // when
        val thumbnailUrls = imageReader.readThumbnailUrls(listOf(12L, 13L, 14L))

        // then
        assertEquals(
            mapOf(
                12L to "https://cdn.example.com/ogonggo/side/b.png",
                13L to "https://cdn.example.com/ogonggo/side/x.png",
            ),
            thumbnailUrls,
        )
    }

    private fun image(id: String) = RecruitmentPostImageDto(imageId = id, url = "https://cdn.example.com/ogonggo/side/$id.png")

    @Test
    fun `모집글과 기술 스택 및 포지션을 함께 저장한다`() {
        val savedPost = postAppender.append(createCommand())
        val postId = checkNotNull(savedPost.id)
        val reloadedPost = postRepository.findById(postId).orElseThrow()

        assertNotNull(savedPost.id)
        assertEquals("사이드 프로젝트 팀원 모집", reloadedPost.title)
        assertEquals(listOf("Kotlin", "Spring"), reloadedPost.technologyStacks)
        assertEquals(listOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN), reloadedPost.positions)
    }

    @Test
    fun `모집글 수정 시 기본 정보와 컬렉션을 함께 갱신한다`() {
        val savedPost = postAppender.append(createCommand())
        val postId = checkNotNull(savedPost.id)

        postManager.update(
            savedPost,
            RecruitmentPostUpdateDto(
                title = "수정된 모집글",
                recruitmentType = RecruitmentPostType.STUDY,
                capacity = 6,
                progressMethod = RecruitmentPostProgressMethod.HYBRID,
                activityDurationMonths = 5,
                technologyStacks = listOf("Java"),
                summary = "수정된 소개",
                content = "{\"root\":{\"children\":[]}}",
                eligibilityAndSelectionProcess = "수정된 자격",
                recruitmentStartDate = LocalDate.of(2026, 9, 2),
                recruitmentEndDate = LocalDate.of(2026, 10, 1),
                positions = listOf(RecruitmentPostPosition.FRONTEND),
                contactMethod = RecruitmentPostContactMethod.OPEN_KAKAO,
                contactValue = "https://open.kakao.com/o/updated",
            ),
            LocalDate.of(2026, 9, 11),
        )
        postRepository.flush()
        val reloadedPost = postRepository.findById(postId).orElseThrow()

        assertEquals("수정된 모집글", reloadedPost.title)
        assertEquals(RecruitmentPostType.STUDY, reloadedPost.recruitmentType)
        assertEquals(listOf("Java"), reloadedPost.technologyStacks)
        assertEquals(listOf(RecruitmentPostPosition.FRONTEND), reloadedPost.positions)
        assertEquals("https://open.kakao.com/o/updated", reloadedPost.contactValue)
    }

    @Test
    fun `모집글 삭제 시 행을 보존하고 삭제 시각을 기록한다`() {
        val savedPost = postAppender.append(createCommand())
        val postId = checkNotNull(savedPost.id)
        val firstDeletedAt = LocalDateTime.of(2026, 9, 11, 9, 0)

        postManager.delete(savedPost, firstDeletedAt)
        postManager.delete(savedPost, firstDeletedAt.plusDays(1))
        postRepository.flush()

        val reloadedPost = postRepository.findById(postId).orElseThrow()

        assertEquals(firstDeletedAt, reloadedPost.deletedAt)
        assertEquals(1, postRepository.count())
    }

    @Test
    fun `모집 종료일이 지난 공개 모집글만 자동 마감한다`() {
        val expiredPost = postAppender.append(createCommand())
        val notExpiredPost = postAppender.append(
            createCommand().copy(recruitmentEndDate = LocalDate.of(2026, 10, 1)),
        )
        val closedAt = LocalDateTime.of(2026, 10, 1, 0, 0)

        val closedCount = postManager.closeExpired(
            today = LocalDate.of(2026, 10, 1),
            closedAt = closedAt,
        )
        postRepository.flush()

        val reloadedExpiredPost = postRepository.findById(checkNotNull(expiredPost.id)).orElseThrow()
        val reloadedNotExpiredPost = postRepository.findById(checkNotNull(notExpiredPost.id)).orElseThrow()
        assertEquals(1, closedCount)
        assertEquals(RecruitmentPostRecruitmentStatus.CLOSED, reloadedExpiredPost.recruitmentStatus)
        assertEquals(closedAt, reloadedExpiredPost.closedAt)
        assertEquals(RecruitmentPostRecruitmentStatus.RECRUITING, reloadedNotExpiredPost.recruitmentStatus)
        assertEquals(null, reloadedNotExpiredPost.closedAt)
    }

    private fun createCommand() = RecruitmentPostAppendDto(
        authorUserId = 1L,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentPostType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = RecruitmentPostProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "{\"root\":{\"children\":[]}}",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN),
        contactMethod = RecruitmentPostContactMethod.EMAIL,
        contactValue = "team@example.com",
    )
}

private val IMAGE_NOW: LocalDateTime = LocalDateTime.of(2026, 10, 12, 10, 0)
