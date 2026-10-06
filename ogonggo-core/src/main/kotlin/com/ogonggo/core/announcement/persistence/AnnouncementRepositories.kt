package com.ogonggo.core.announcement.persistence

import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.domain.QAnnouncement.announcement
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
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

    fun findPublicPage(pageable: Pageable): Page<Announcement> = findPage(
        predicates = arrayOf(announcement.deletedAt.isNull, announcement.published.isTrue),
        pageable = pageable,
    )

    fun findManagementPage(condition: AnnouncementManagementSearchCondition, pageable: Pageable): Page<Announcement> = findPage(
        predicates = arrayOf(
            announcement.deletedAt.isNull,
            condition.published?.let(announcement.published::eq),
            condition.pinned?.let(announcement.pinned::eq),
            condition.keyword?.takeIf { it.isNotBlank() }?.let(announcement.title::containsIgnoreCase),
        ),
        pageable = pageable,
    )

    private fun findPage(predicates: Array<Predicate?>, pageable: Pageable): Page<Announcement> {
        val content = queryFactory.selectFrom(announcement)
            .where(*predicates)
            .orderBy(announcement.pinned.desc(), announcement.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(announcement.count())
            .from(announcement)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }
}
