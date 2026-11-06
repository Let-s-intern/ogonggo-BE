package com.ogonggo.core.adchannel.implement

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannel.domain.AdChannelProfile
import com.ogonggo.core.adchannel.error.AdChannelErrorCode
import com.ogonggo.core.adchannel.implement.dto.AdChannelAppendDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AdChannelReader::class, AdChannelAppender::class, AdChannelManager::class)
internal class AdChannelPersistenceTest @Autowired constructor(
    private val reader: AdChannelReader,
    private val appender: AdChannelAppender,
    private val manager: AdChannelManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `채널 목록은 모든 플랫폼을 등록 순으로 두고 지운 채널은 뺀다`() {
        // given
        val instagram = append(AdChannelPlatform.INSTAGRAM, "letscareer.job")
        val openChat = append(AdChannelPlatform.KAKAO_OPENCHAT, null)
        val deleted = append(AdChannelPlatform.INSTAGRAM, "letscareer.qna")
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()
        entityManager.clear()

        // when
        val channels = reader.readAll()

        // then
        assertEquals(listOf(instagram, openChat), channels.map { it.id })
        assertEquals(setOf(instagram), reader.readAllByIds(listOf(instagram, deleted)).keys)
        val exception = assertThrows(EntityNotFoundException::class.java) { reader.read(deleted) }
        assertEquals(AdChannelErrorCode.AD_CHANNEL_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `계정 이름은 대소문자를 무시하고 비교하며 자기 자신과 지운 채널은 보지 않는다`() {
        // given
        val id = append(AdChannelPlatform.INSTAGRAM, "letscareer.job")
        val deleted = append(AdChannelPlatform.INSTAGRAM, "letscareer.univ")
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()

        // then
        assertTrue(reader.existsHandle("LetsCareer.JOB"))
        assertFalse(reader.existsHandle("letscareer.job", exceptChannelId = id))
        assertFalse(reader.existsHandle("letscareer.univ"))
    }

    private fun append(platform: AdChannelPlatform, handle: String?): Long {
        val channel = appender.append(
            AdChannelAppendDto(
                platform = platform,
                profile = AdChannelProfile(
                    name = "채널",
                    handle = handle,
                    url = "https://example.com/channel",
                    profileImageUrl = null,
                    memberCount = 120,
                    enabled = true,
                    memo = null,
                ),
            ),
        )
        entityManager.flush()
        return checkNotNull(channel.id)
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
