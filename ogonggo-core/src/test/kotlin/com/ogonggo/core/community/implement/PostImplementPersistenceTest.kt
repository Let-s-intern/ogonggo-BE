package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.PostJpaRepository
import com.ogonggo.core.common.CoreJpaConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(PostAppenderImpl::class, PostManagerImpl::class)
internal class PostImplementPersistenceTest @Autowired constructor(
    private val postAppender: PostAppender,
    private val postManager: PostManager,
    private val postRepository: PostJpaRepository,
) {

    @Test
    fun `모집글과 기술 스택 및 포지션을 함께 저장한다`() {
        val savedPost = postAppender.append(createCommand())
        val postId = checkNotNull(savedPost.id)
        val reloadedPost = postRepository.findById(postId).orElseThrow()

        assertNotNull(savedPost.id)
        assertEquals("사이드 프로젝트 팀원 모집", reloadedPost.title)
        assertEquals(listOf("Kotlin", "Spring"), reloadedPost.technologyStacks)
        assertEquals(listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN), reloadedPost.positions)
    }

    @Test
    fun `모집글 수정 시 기본 정보와 컬렉션을 함께 갱신한다`() {
        val savedPost = postAppender.append(createCommand())
        val postId = checkNotNull(savedPost.id)

        postManager.update(
            savedPost,
            PostUpdateCommand(
                title = "수정된 모집글",
                recruitmentType = RecruitmentType.STUDY,
                capacity = 6,
                progressMethod = ProgressMethod.HYBRID,
                activityDurationMonths = 5,
                technologyStacks = listOf("Java"),
                summary = "수정된 소개",
                content = "<p>수정된 본문</p>",
                eligibilityAndSelectionProcess = "수정된 자격",
                recruitmentStartDate = LocalDate.of(2026, 9, 2),
                recruitmentEndDate = LocalDate.of(2026, 10, 1),
                positions = listOf(RecruitmentPosition.FRONTEND),
                contactMethod = ContactMethod.OPEN_KAKAO,
                contactValue = "https://open.kakao.com/o/updated",
            ),
        )
        postRepository.flush()
        val reloadedPost = postRepository.findById(postId).orElseThrow()

        assertEquals("수정된 모집글", reloadedPost.title)
        assertEquals(RecruitmentType.STUDY, reloadedPost.recruitmentType)
        assertEquals(listOf("Java"), reloadedPost.technologyStacks)
        assertEquals(listOf(RecruitmentPosition.FRONTEND), reloadedPost.positions)
        assertEquals("https://open.kakao.com/o/updated", reloadedPost.contactValue)
    }

    private fun createCommand() = PostAppendCommand(
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
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )
}
