package com.ogonggo.adminapi.letscareercontent.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTags
import com.ogonggo.core.letscareercontent.error.LetsCareerContentErrorCode
import com.ogonggo.core.letscareercontent.implement.LetsCareerContentManager
import com.ogonggo.core.letscareercontent.implement.LetsCareerContentReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * AI 서버가 렛츠커리어 콘텐츠에 추천용 태그를 붙여 돌려주는 흐름이다.
 * 사본은 사용자 API가 렛츠커리어 목록으로 채우고, AI 서버는 태그가 없거나 내용이 바뀐 콘텐츠를 받아 AI로 태그한다.
 */
@Service
class AiLetsCareerContentTagService(
    private val letsCareerContentReader: LetsCareerContentReader,
    private val letsCareerContentManager: LetsCareerContentManager,
    private val clock: Clock,
) {

    fun getTargets(size: Int): List<LetsCareerContent> = letsCareerContentReader.readTaggingTargets(size)

    /** 렛츠커리어 목록에 있는 콘텐츠 전부. AI 서버 화면에서 어떤 태그가 붙었는지 본다. */
    fun getContents(): List<LetsCareerContent> = letsCareerContentReader.readAll().sortedBy { it.id }

    /** 태그를 저장한다. 대상을 받은 뒤 제목·설명이 바뀌었으면 저장하지 않고 충돌로 알린다. */
    @Transactional
    fun saveTags(contentId: Long, contentHash: String, tags: LetsCareerContentTags) {
        val content = letsCareerContentReader.read(contentId)
        if (content.contentHash != contentHash) {
            throw ConflictException(LetsCareerContentErrorCode.LETS_CAREER_CONTENT_TAGS_OUTDATED)
        }
        letsCareerContentManager.tag(content, tags, LocalDateTime.now(clock))
    }
}
