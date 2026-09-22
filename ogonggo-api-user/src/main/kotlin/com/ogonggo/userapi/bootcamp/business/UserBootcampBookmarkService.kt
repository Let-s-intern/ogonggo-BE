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

    /** 단계 사이에 선후 관계가 없어 어느 단계로든 옮긴다. */
    @Transactional
    fun changeApplicationStatus(userId: Long, bootcampId: Long, status: BootcampApplicationStatus) {
        bootcampBookmarkManager.changeApplicationStatus(userId, bootcampId, status, LocalDateTime.now(clock))
    }
}
