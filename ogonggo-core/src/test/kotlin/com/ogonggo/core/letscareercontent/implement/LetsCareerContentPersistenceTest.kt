package com.ogonggo.core.letscareercontent.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentSource
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTags
import com.ogonggo.core.letscareercontent.implement.dto.LetsCareerContentSyncDto
import com.ogonggo.core.letscareercontent.implement.dto.LetsCareerContentSyncResultDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(LetsCareerContentReader::class, LetsCareerContentManager::class)
internal class LetsCareerContentPersistenceTest @Autowired constructor(
    private val letsCareerContentReader: LetsCareerContentReader,
    private val letsCareerContentManager: LetsCareerContentManager,
) {

    @Test
    fun `렛츠커리어 목록으로 덮어쓰면 없던 것은 만들고 빠진 것은 지우고 다시 나온 것은 되살린다`() {
        // given
        letsCareerContentManager.replaceAll(listOf(sync(LetsCareerContentKind.CHALLENGE, 1), sync(LetsCareerContentKind.BLOG, 1)), NOW)

        // when
        val first = letsCareerContentManager.replaceAll(listOf(sync(LetsCareerContentKind.BLOG, 1), sync(LetsCareerContentKind.MATERIAL, 2)), NOW)
        val second = letsCareerContentManager.replaceAll(listOf(sync(LetsCareerContentKind.CHALLENGE, 1)), NOW)

        // then
        assertEquals(LetsCareerContentSyncResultDto(created = 1, updated = 1, deleted = 1), first)
        assertEquals(LetsCareerContentSyncResultDto(created = 0, updated = 1, deleted = 2), second)
        assertEquals(
            listOf(LetsCareerContentKind.CHALLENGE to 1L),
            letsCareerContentReader.readAll().map { it.kind to it.externalId },
        )
    }

    @Test
    fun `태그 대상은 태그가 없거나 태그한 뒤 내용이 바뀐 콘텐츠다`() {
        // given
        letsCareerContentManager.replaceAll(
            listOf(sync(LetsCareerContentKind.BLOG, 1), sync(LetsCareerContentKind.BLOG, 2), sync(LetsCareerContentKind.BLOG, 3)),
            NOW,
        )
        letsCareerContentReader.readAll().forEach { letsCareerContentManager.tag(it, EMPTY_TAGS, NOW) }
        letsCareerContentManager.replaceAll(
            listOf(sync(LetsCareerContentKind.BLOG, 1), sync(LetsCareerContentKind.BLOG, 2, title = "새 제목"), sync(LetsCareerContentKind.BLOG, 4)),
            NOW,
        )

        // when
        val targets = letsCareerContentReader.readTaggingTargets(10)

        // then
        assertEquals(listOf(2L, 4L), targets.map { it.externalId })
    }

    private fun sync(kind: LetsCareerContentKind, externalId: Long, title: String = "제목 $externalId") = LetsCareerContentSyncDto(
        kind = kind,
        externalId = externalId,
        source = LetsCareerContentSource(
            category = null,
            title = title,
            description = null,
            thumbnailUrl = null,
            path = "/blog/$externalId",
            labels = emptyList(),
            recruitmentStartAt = null,
            recruitmentEndAt = null,
        ),
    )

    companion object {
        private val NOW = LocalDateTime.of(2026, 10, 9, 12, 0)
        private val EMPTY_TAGS = LetsCareerContentTags(emptySet(), emptySet(), emptySet())
    }
}
