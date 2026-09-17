package com.ogonggo.userapi.community.presentation.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.business.RecruitmentPostSaveMode
import com.ogonggo.userapi.error.InvalidRequestFieldException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecruitmentPostRequestsTest {

    @Test
    fun `공개 모집글 생성은 공백 기술 스택을 거부한다`() {
        // given
        val request = publishedRequest().copy(technologyStacks = listOf("Kotlin", " "))

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toSaveCommand(AUTHOR_USER_ID)
        }

        // then
        assertEquals("technologyStacks", exception.fieldName)
    }

    @Test
    fun `공개 모집글 생성은 중복 기술 스택을 거부한다`() {
        // given
        val request = publishedRequest().copy(technologyStacks = listOf("Kotlin", "Kotlin"))

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toSaveCommand(AUTHOR_USER_ID)
        }

        // then
        assertEquals("technologyStacks", exception.fieldName)
    }

    @Test
    fun `공개 모집글 생성은 공백 지원 자격 및 전형을 거부한다`() {
        // given
        val request = publishedRequest().copy(eligibilityAndSelectionProcess = " ")

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toSaveCommand(AUTHOR_USER_ID)
        }

        // then
        assertEquals("eligibilityAndSelectionProcess", exception.fieldName)
    }

    @Test
    fun `공개 모집글 생성은 50자를 초과한 기술 스택을 거부한다`() {
        // given
        val request = publishedRequest().copy(technologyStacks = listOf("K".repeat(51)))

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toSaveCommand(AUTHOR_USER_ID)
        }

        // then
        assertEquals("technologyStacks", exception.fieldName)
    }

    @Test
    fun `생성 API 임시저장은 50자를 초과한 기술 스택을 거부한다`() {
        // given
        val request = CreateRecruitmentPostRequest(
            title = "작성 중인 모집글",
            technologyStacks = listOf("K".repeat(51)),
            saveMode = RecruitmentPostSaveMode.DRAFT,
        )

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toSaveCommand(AUTHOR_USER_ID)
        }

        // then
        assertEquals("technologyStacks", exception.fieldName)
    }

    @Test
    fun `별도 임시저장 API는 50자를 초과한 기술 스택을 거부한다`() {
        // given
        val request = CreateRecruitmentPostDraftRequest(
            title = "작성 중인 모집글",
            technologyStacks = listOf("K".repeat(51)),
        )

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toCommand(AUTHOR_USER_ID)
        }

        // then
        assertEquals("technologyStacks", exception.fieldName)
    }

    @Test
    fun `모집글 수정은 50자를 초과한 기술 스택을 거부한다`() {
        // given
        val request = UpdateRecruitmentPostRequest(
            title = "수정 중인 모집글",
            technologyStacks = listOf("K".repeat(51)),
            saveMode = RecruitmentPostSaveMode.DRAFT,
        )

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            request.toCommand()
        }

        // then
        assertEquals("technologyStacks", exception.fieldName)
    }

    private fun publishedRequest() = CreateRecruitmentPostRequest(
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = OBJECT_MAPPER.readTree("""{"root":{"children":[]}}"""),
        eligibilityAndSelectionProcess = "주 1회 회의에 참여할 수 있는 분",
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
        agreedToPolicy = true,
    )

    private companion object {
        const val AUTHOR_USER_ID = 17L
        val OBJECT_MAPPER = ObjectMapper()
    }
}
