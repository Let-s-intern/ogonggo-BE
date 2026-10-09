package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LetsCareerContentDomainTest {

    @Test
    fun `태그한 뒤로 제목이나 설명이 바뀌면 지금 태그가 없다`() {
        // given
        val content = LetsCareerContent(LetsCareerContentKind.MATERIAL, 57L, source("마케팅 취준 자료집"))
        content.tag(TAGS, NOW)

        // when
        content.replace(source("마케팅 취준 총정리 자료집"))

        // then
        assertNull(content.currentTags())
    }

    @Test
    fun `모집 기간만 바뀌면 태그를 그대로 쓴다`() {
        // given
        val content = LetsCareerContent(LetsCareerContentKind.CHALLENGE, 10L, source("마케팅 챌린지"))
        content.tag(TAGS, NOW)

        // when
        content.replace(source("마케팅 챌린지").copy(recruitmentEndAt = NOW.plusDays(3)))

        // then
        assertEquals(TAGS, content.currentTags())
    }

    @Test
    fun `목록에서 빠졌다가 다시 나오면 되살린다`() {
        val content = LetsCareerContent(LetsCareerContentKind.BLOG, 248L, source("자소서 쓰는 법"))
        content.delete(NOW)
        content.delete(NOW.plusDays(1))
        assertEquals(NOW, content.deletedAt)

        content.replace(source("자소서 쓰는 법"))

        assertNull(content.deletedAt)
    }

    private fun source(title: String) = LetsCareerContentSource(
        category = "MATERIAL",
        title = title,
        description = "설명",
        thumbnailUrl = null,
        path = "/library/57/x",
        labels = listOf("마케팅"),
        recruitmentStartAt = null,
        recruitmentEndAt = null,
    )

    companion object {
        private val NOW = LocalDateTime.of(2026, 10, 9, 12, 0)
        private val TAGS = LetsCareerContentTags(
            jobFields = setOf(JobField.MARKETING_ADVERTISING),
            jobRoles = setOf(JobRole.entries.first { it.jobField == JobField.MARKETING_ADVERTISING }),
            topics = setOf(LetsCareerContentTopic.PERSONAL_STATEMENT),
        )
    }
}
