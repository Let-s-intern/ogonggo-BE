package com.ogonggo.core.letscareercontent.persistence

import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

internal interface LetsCareerContentJpaRepository : JpaRepository<LetsCareerContent, Long> {
    fun findAllByDeletedAtIsNull(): List<LetsCareerContent>

    fun findByIdAndDeletedAtIsNull(id: Long): LetsCareerContent?

    /** 태그가 없거나 태그한 뒤로 내용이 바뀐 콘텐츠를 오래된 것부터 고른다. */
    @Query(
        """
        select content from LetsCareerContent content
        where content.deletedAt is null
          and (content.taggedContentHash is null or content.taggedContentHash <> content.contentHash)
        order by content.id asc
        """,
    )
    fun findTaggingTargets(pageable: Pageable): List<LetsCareerContent>
}
