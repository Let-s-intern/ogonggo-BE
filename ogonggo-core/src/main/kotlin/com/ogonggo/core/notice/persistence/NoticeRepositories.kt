package com.ogonggo.core.notice.persistence

import com.ogonggo.core.notice.domain.Notice
import com.ogonggo.core.notice.domain.NoticeManagementSearchCondition
import com.ogonggo.core.notice.domain.QNotice.notice
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

internal interface NoticeJpaRepository : JpaRepository<Notice, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Notice?

    fun findByIdAndPublishedIsTrueAndDeletedAtIsNull(id: Long): Notice?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notice from Notice notice where notice.id = :noticeId and notice.deletedAt is null")
    fun findByIdForUpdate(@Param("noticeId") noticeId: Long): Notice?

    /** 삭제는 멱등해야 하므로 이미 삭제된 공지도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notice from Notice notice where notice.id = :noticeId")
    fun findIncludingDeletedByIdForUpdate(@Param("noticeId") noticeId: Long): Notice?
}

/** 사용자 목록과 관리자 목록 모두 고정 공지를 먼저 두고 최신순으로 정렬한다. */
@Repository
internal class NoticeQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPublicPage(pageable: Pageable): Page<Notice> = findPage(
        predicates = arrayOf(notice.deletedAt.isNull, notice.published.isTrue),
        pageable = pageable,
    )

    fun findManagementPage(condition: NoticeManagementSearchCondition, pageable: Pageable): Page<Notice> = findPage(
        predicates = arrayOf(
            notice.deletedAt.isNull,
            condition.published?.let(notice.published::eq),
            condition.pinned?.let(notice.pinned::eq),
            condition.keyword?.takeIf { it.isNotBlank() }?.let(notice.title::containsIgnoreCase),
        ),
        pageable = pageable,
    )

    private fun findPage(predicates: Array<Predicate?>, pageable: Pageable): Page<Notice> {
        val content = queryFactory.selectFrom(notice)
            .where(*predicates)
            .orderBy(notice.pinned.desc(), notice.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory.select(notice.count())
            .from(notice)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }
}
