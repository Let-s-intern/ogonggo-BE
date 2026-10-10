package com.ogonggo.core.letscareercontent.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.error.LetsCareerContentErrorCode
import com.ogonggo.core.letscareercontent.persistence.LetsCareerContentJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class LetsCareerContentReader internal constructor(
    private val letsCareerContentRepository: LetsCareerContentJpaRepository,
) {

    /** 지금 렛츠커리어 목록에 있는 콘텐츠 전부다. 많아야 수백 건이라 한 번에 읽는다. */
    fun readAll(): List<LetsCareerContent> = letsCareerContentRepository.findAllByDeletedAtIsNull()

    fun read(contentId: Long): LetsCareerContent =
        letsCareerContentRepository.findByIdAndDeletedAtIsNull(contentId)
            ?: throw EntityNotFoundException(LetsCareerContentErrorCode.LETS_CAREER_CONTENT_NOT_FOUND)

    fun readTaggingTargets(limit: Int): List<LetsCareerContent> {
        require(limit in 1..MAX_TAGGING_TARGETS) { "태그 대상은 1개 이상 ${MAX_TAGGING_TARGETS}개 이하로 읽습니다." }
        return letsCareerContentRepository.findTaggingTargets(PageRequest.of(0, limit))
    }

    companion object {
        const val MAX_TAGGING_TARGETS = 200
    }
}
