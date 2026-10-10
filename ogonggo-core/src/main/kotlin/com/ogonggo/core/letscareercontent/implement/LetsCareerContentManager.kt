package com.ogonggo.core.letscareercontent.implement

import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTags
import com.ogonggo.core.letscareercontent.implement.dto.LetsCareerContentSyncDto
import com.ogonggo.core.letscareercontent.implement.dto.LetsCareerContentSyncResultDto
import com.ogonggo.core.letscareercontent.persistence.LetsCareerContentJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class LetsCareerContentManager internal constructor(
    private val letsCareerContentRepository: LetsCareerContentJpaRepository,
) {

    /**
     * 렛츠커리어 목록 전체로 사본을 덮어쓴다. 목록에 없는 사본은 지운 것으로 표시하고, 지웠던 사본이 다시 나오면 되살린다.
     * 같은 콘텐츠가 목록에 두 번 있으면 뒤의 것을 쓴다.
     */
    fun replaceAll(contents: List<LetsCareerContentSyncDto>, now: LocalDateTime): LetsCareerContentSyncResultDto {
        val incoming = contents.associateBy { it.kind to it.externalId }
        val existing = letsCareerContentRepository.findAll().associateBy { it.kind to it.externalId }

        var created = 0
        var updated = 0
        incoming.forEach { (key, content) ->
            val saved = existing[key]
            if (saved == null) {
                letsCareerContentRepository.save(LetsCareerContent(content.kind, content.externalId, content.source))
                created++
            } else {
                saved.replace(content.source)
                updated++
            }
        }

        val removed = existing.filterKeys { it !in incoming }.values.filter { it.deletedAt == null }
        removed.forEach { it.delete(now) }
        return LetsCareerContentSyncResultDto(created = created, updated = updated, deleted = removed.size)
    }

    fun tag(content: LetsCareerContent, tags: LetsCareerContentTags, now: LocalDateTime) {
        content.tag(tags, now)
    }
}
