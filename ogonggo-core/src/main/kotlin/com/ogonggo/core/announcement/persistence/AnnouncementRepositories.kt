package com.ogonggo.core.announcement.persistence

import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.domain.AnnouncementPublicSearchCondition
import com.ogonggo.core.announcement.domain.QAnnouncement.announcement
import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

internal interface AnnouncementJpaRepository : JpaRepository<Announcement, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Announcement?

    fun findByIdAndPublishedIsTrueAndDeletedAtIsNull(id: Long): Announcement?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select announcement from Announcement announcement where announcement.id = :announcementId and announcement.deletedAt is null")
    fun findByIdForUpdate(@Param("announcementId") announcementId: Long): Announcement?

    /** 삭제는 멱등해야 하므로 이미 삭제된 공지도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select announcement from Announcement announcement where announcement.id = :announcementId")
    fun findIncludingDeletedByIdForUpdate(@Param("announcementId") announcementId: Long): Announcement?
}

/** 사용자 목록과 관리자 목록 모두 고정 공지를 먼저 두고 최신순으로 정렬한다. */
@Repository
internal class AnnouncementQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublicPage(condition: AnnouncementPublicSearchCondition, pageable: Pageable): Page<Announcement> = findPage(
        predicates = arrayOf(
            *publicPredicates(),
            condition.category?.let(announcement.category::eq),
            condition.pinned?.let(announcement.pinned::eq),
            condition.keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { keyword ->
                // 본문은 Lexical JSON 이라 글자가 `"text"` 값 안에 있다. 사용자는 한글로 찾으므로 JSON 키(영문)에 걸리는 일은
                // 드물다. 글자만 편 칸을 따로 두지 않는다.
                announcement.title.containsIgnoreCase(keyword)
                    .or(announcement.summary.containsIgnoreCase(keyword))
                    .or(announcement.content.containsIgnoreCase(keyword))
            },
        ),
        pageable = pageable,
    )

    /** 카테고리별 노출 중인 공지 수. 공지가 없는 카테고리는 빠진다. */
    fun countPublicByCategory(): Map<AnnouncementCategory, Long> =
        queryFactory.select(announcement.category, announcement.count())
            .from(announcement)
            .where(*publicPredicates())
            .groupBy(announcement.category)
            .fetch()
            .associate { row -> checkNotNull(row.get(announcement.category)) to (row.get(announcement.count()) ?: 0L) }

    /** 한 카테고리의 노출 중인 최근 공지. 카테고리 칸은 고정 여부와 상관없이 최신순이다. */
    fun findRecentPublic(category: AnnouncementCategory, limit: Int): List<Announcement> =
        queryFactory.selectFrom(announcement)
            .where(*publicPredicates(), announcement.category.eq(category))
            .orderBy(announcement.id.desc())
            .limit(limit.toLong())
            .fetch()

    private fun publicPredicates(): Array<Predicate> = arrayOf(announcement.deletedAt.isNull, announcement.published.isTrue)

    fun findManagementPage(condition: AnnouncementManagementSearchCondition, pageable: Pageable): Page<Announcement> = findPage(
        predicates = arrayOf(
            announcement.deletedAt.isNull,
            condition.published?.let(announcement.published::eq),
            condition.pinned?.let(announcement.pinned::eq),
            condition.keyword?.takeIf { it.isNotBlank() }?.let(announcement.title::containsIgnoreCase),
            condition.category?.let(announcement.category::eq),
        ),
        pageable = pageable,
    )

    private fun findPage(predicates: Array<Predicate?>, pageable: Pageable): Page<Announcement> {
        val content = queryFactory.selectFrom(announcement)
            .where(*predicates)
            .orderBy(announcement.pinned.desc(), announcement.id.desc())
            .paged(pageable)
            .fetch()

        val countQuery = queryFactory.select(announcement.count())
            .from(announcement)
            .where(*predicates)

        return pageOf(content, pageable, countQuery)
    }
}
