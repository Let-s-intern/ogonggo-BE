package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostErrorCode
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostPageDto

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(RecruitmentPostReader::class, RecruitmentPostManager::class, RecruitmentPostQueryRepository::class)
internal class RecruitmentPostConsoleReaderPersistenceTest @Autowired constructor(
    private val postReader: RecruitmentPostReader,
    private val postManager: RecruitmentPostManager,
    private val postRepository: RecruitmentPostJpaRepository,
) {

    @Test
    fun `콘솔 목록은 공개와 숨긴 모집글을 싣고 임시저장과 삭제한 모집글은 뺀다`() {
        // given
        val published = postRepository.save(createPost("공개 모집"))
        val hidden = postRepository.save(createPost("숨긴 모집", RecruitmentPostPublicationStatus.HIDDEN))
        postRepository.save(createPost("작성 중 모집").copyAsDraft())
        postManager.delete(postRepository.save(createPost("삭제한 모집")), LocalDateTime.of(2026, 9, 20, 10, 0))

        // when
        val all = readConsolePage(RecruitmentPostConsoleSearchCondition())
        val hiddenOnly = readConsolePage(RecruitmentPostConsoleSearchCondition(published = false))

        // then
        assertEquals(listOf(hidden.id, published.id), all.posts.map(RecruitmentPost::id))
        assertEquals(2, all.totalElements)
        assertEquals(listOf(hidden.id), hiddenOnly.posts.map(RecruitmentPost::id))
    }

    @Test
    fun `콘솔 목록은 모집 구분과 제목 검색어를 함께 건다`() {
        // given
        val match = postRepository.save(createPost("Kotlin 스터디", recruitmentType = RecruitmentPostType.STUDY))
        postRepository.save(createPost("Kotlin 사이드 프로젝트", recruitmentType = RecruitmentPostType.SIDE_PROJECT))
        postRepository.save(createPost("자바 스터디", recruitmentType = RecruitmentPostType.STUDY))

        // when
        val result = readConsolePage(
            RecruitmentPostConsoleSearchCondition(recruitmentType = RecruitmentPostType.STUDY, keyword = "kotlin"),
        )

        // then
        assertEquals(listOf(match.id), result.posts.map(RecruitmentPost::id))
    }

    @Test
    fun `노출 변경 대상에 임시저장이나 삭제한 모집글이 있으면 그 식별자를 담아 찾을 수 없다고 알린다`() {
        // given
        val published = postRepository.save(createPost("공개 모집"))
        val draft = postRepository.save(createPost("작성 중 모집").copyAsDraft())
        val deleted = postRepository.save(createPost("삭제한 모집"))
        postManager.delete(deleted, LocalDateTime.of(2026, 9, 20, 10, 0))

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) {
            postReader.readAllPostedForUpdate(listOf(checkNotNull(published.id), checkNotNull(deleted.id), checkNotNull(draft.id)))
        }

        // then
        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND, exception.errorCode)
        val missingIds = listOf(checkNotNull(draft.id), checkNotNull(deleted.id)).sorted().joinToString()
        assertEquals("모집글을 찾을 수 없습니다. (id: $missingIds)", exception.message)
    }

    private fun readConsolePage(condition: RecruitmentPostConsoleSearchCondition): RecruitmentPostPageDto =
        postReader.readConsolePage(condition, RecruitmentPostSortType.LATEST, page = 0, size = 10)

    private fun createPost(
        title: String,
        publicationStatus: RecruitmentPostPublicationStatus = RecruitmentPostPublicationStatus.PUBLISHED,
        recruitmentType: RecruitmentPostType = RecruitmentPostType.STUDY,
    ) = RecruitmentPost(
        authorUserId = 1L,
        title = title,
        recruitmentType = recruitmentType,
        capacity = 4,
        progressMethod = RecruitmentPostProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "{\"root\":{\"children\":[]}}",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPostPosition.BACKEND),
        contactMethod = RecruitmentPostContactMethod.EMAIL,
        contactValue = "team@example.com",
        publicationStatus = publicationStatus,
    )
}
