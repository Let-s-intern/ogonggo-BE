package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampBookmarkSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkManager
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserBootcampBookmarkService(
    private val bootcampReader: BootcampReader,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
    private val bootcampBookmarkManager: BootcampBookmarkManager,
    private val bootcampMetricReader: BootcampMetricReader,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {

    fun getBookmarks(
        userId: Long,
        condition: BootcampSearchCondition,
        page: Int,
        size: Int,
        bookmarkCondition: BootcampBookmarkSearchCondition = BootcampBookmarkSearchCondition.NONE,
    ): UserBootcampPageResult {
        val result = bootcampBookmarkReader.readBookmarkedPublicPage(userId, condition, page, size, bookmarkCondition)
        val bootcampIds = result.bootcamps.map(Bootcamp::requiredId)
        return UserBootcampPageResult.from(
            result = result,
            bookmarkedBootcampIds = bootcampIds.toSet(),
            metrics = bootcampMetricReader.readAll(bootcampIds),
        )
    }

    @Transactional
    fun addBookmark(userId: Long, bootcampId: Long) {
        bootcampReader.readPublic(bootcampId)
        bootcampBookmarkManager.append(userId, bootcampId, LocalDateTime.now(clock))
        eventPublisher.publishEvent(BootcampBookmarkChangedEvent(bootcampId))
    }

    @Transactional
    fun deleteBookmark(userId: Long, bootcampId: Long) {
        bootcampReader.readIncludingDeleted(bootcampId)
        bootcampBookmarkManager.delete(userId, bootcampId, LocalDateTime.now(clock))
        eventPublisher.publishEvent(BootcampBookmarkChangedEvent(bootcampId))
    }

    /** 스크랩한 북마크를 신청 전으로 옮긴다. */
    @Transactional
    fun prepare(userId: Long, bootcampId: Long) {
        bootcampBookmarkManager.changeApplicationStatus(userId, bootcampId, BootcampApplicationStatus.PREPARING, LocalDateTime.now(clock))
    }

    /** 신청 전인 북마크를 스크랩으로 되돌린다. */
    @Transactional
    fun cancelPreparation(userId: Long, bootcampId: Long) {
        bootcampBookmarkManager.changeApplicationStatus(userId, bootcampId, BootcampApplicationStatus.SCRAPPED, LocalDateTime.now(clock))
    }
}
