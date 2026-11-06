package com.ogonggo.core.adchannelformat.implement

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormatTerms
import com.ogonggo.core.adchannelformat.implement.dto.AdChannelFormatAppendDto
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AdChannelFormatReader::class, AdChannelFormatAppender::class, AdChannelFormatManager::class)
internal class AdChannelFormatPersistenceTest @Autowired constructor(
    private val reader: AdChannelFormatReader,
    private val appender: AdChannelFormatAppender,
    private val manager: AdChannelFormatManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `새 유형은 그 플랫폼의 맨 뒤 순서이고 목록은 플랫폼 순, 그 안에서 순서대로다`() {
        // given
        val kakaoFirst = append(AdChannelPlatform.KAKAO_OPENCHAT, "일반 글")
        val feed = append(AdChannelPlatform.INSTAGRAM, "피드 게시물")
        val reels = append(AdChannelPlatform.INSTAGRAM, "릴스(숏츠)")
        entityManager.clear()

        // when
        val formats = reader.readAll()

        // then
        assertEquals(listOf(feed, reels, kakaoFirst), formats.map { it.id })
        assertEquals(listOf(1, 2, 1), formats.map { it.displayOrder })
    }

    @Test
    fun `순서를 통째로 바꾸고 지우면 같은 플랫폼의 남은 순서를 1부터 다시 매긴다`() {
        // given
        val first = append(AdChannelPlatform.KAKAO_OPENCHAT, "일반 글")
        val second = append(AdChannelPlatform.KAKAO_OPENCHAT, "이미지 글")
        val third = append(AdChannelPlatform.KAKAO_OPENCHAT, "공지 등록")

        // when
        manager.reorder(AdChannelPlatform.KAKAO_OPENCHAT, listOf(third, first, second))
        manager.delete(reader.readForDelete(first), NOW)
        entityManager.flush()
        entityManager.clear()

        // then
        val formats = reader.readAll()
        assertEquals(listOf(third, second), formats.map { it.id })
        assertEquals(listOf(1, 2), formats.map { it.displayOrder })
    }

    @Test
    fun `지운 채널은 그 채널을 둔 유형의 올릴 수 있는 채널에서 빠진다`() {
        // given
        val carousel = append(AdChannelPlatform.INSTAGRAM, "캐러셀", channelIds = listOf(1, 2, 5))
        val story = append(AdChannelPlatform.INSTAGRAM, "스토리", channelIds = listOf(5))

        // when
        manager.removeChannel(5)
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(listOf(1L, 2L), reader.read(carousel).channelIds)
        assertEquals(emptyList<Long>(), reader.read(story).channelIds)
    }

    @Test
    fun `같은 이름은 같은 플랫폼 안에서만 보고 자기 자신과 지운 유형은 보지 않는다`() {
        // given
        val id = append(AdChannelPlatform.INSTAGRAM, "스토리")
        val deleted = append(AdChannelPlatform.INSTAGRAM, "캐러셀")
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()

        // then
        assertTrue(reader.existsName(AdChannelPlatform.INSTAGRAM, " 스토리 "))
        assertFalse(reader.existsName(AdChannelPlatform.KAKAO_OPENCHAT, "스토리"))
        assertFalse(reader.existsName(AdChannelPlatform.INSTAGRAM, "스토리", exceptFormatId = id))
        assertFalse(reader.existsName(AdChannelPlatform.INSTAGRAM, "캐러셀"))
    }

    private fun append(platform: AdChannelPlatform, name: String, channelIds: List<Long> = emptyList()): Long {
        val format = appender.append(
            AdChannelFormatAppendDto(
                platform = platform,
                terms = AdChannelFormatTerms(name, null, true, 100_000, channelIds),
            ),
        )
        entityManager.flush()
        return checkNotNull(format.id)
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
