package com.ogonggo.userapi.user.implement

import com.ogonggo.core.user.domain.UserGrade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JobProfileResponseTest {

    @Test
    fun `렛츠커리어의 학년을 오공고 학년으로 옮긴다`() {
        assertEquals(UserGrade.GRADUATE, response(grade = "GRADUATE").toResult().grade)
        assertEquals(UserGrade.FIRST, response(grade = "FIRST").toResult().grade)
    }

    /** 렛츠커리어가 학년을 추가해도 가입이 막히지 않아야 하므로 모르는 값은 비운다. */
    @Test
    fun `오공고가 모르는 학년은 비우고 나머지는 그대로 옮긴다`() {
        val result = response(grade = "SEVENTH").toResult()

        assertNull(result.grade)
        assertEquals("오공고대학교", result.university)
        assertEquals("개발", result.wishField)
    }

    @Test
    fun `학년을 주지 않으면 비워 둔다`() {
        assertNull(response(grade = null).toResult().grade)
    }

    @Test
    fun `학력과 희망 조건을 빠뜨리지 않고 옮긴다`() {
        val result = response(grade = "GRADUATE").toResult()

        assertEquals("오공고대학교", result.university)
        assertEquals("컴퓨터공학과", result.major)
        assertEquals("개발", result.wishField)
        assertEquals("백엔드 개발", result.wishJob)
        assertEquals("IT", result.wishIndustry)
        assertEquals("정규직", result.wishEmploymentType)
        assertEquals("오공고", result.wishCompany)
    }

    private fun response(grade: String?): JobProfileResponse = JobProfileResponse(
        userId = 4821L,
        university = "오공고대학교",
        major = "컴퓨터공학과",
        grade = grade,
        wishField = "개발",
        wishJob = "백엔드 개발",
        wishIndustry = "IT",
        wishEmploymentType = "정규직",
        wishCompany = "오공고",
    )
}
