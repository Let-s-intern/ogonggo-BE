package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampBookmark
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.persistence.BootcampBookmarkJpaRepository
import com.ogonggo.core.error.ConflictException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface BootcampBookmarkManager {
    fun append(userId: Long, bootcampId: Long)
    fun delete(userId: Long, bootcampId: Long, now: LocalDateTime)
}

@Component
internal class BootcampBookmarkManagerImpl(
    private val bootcampBookmarkRepository: BootcampBookmarkJpaRepository,
) : BootcampBookmarkManager {

    override fun append(userId: Long, bootcampId: Long) {
        val bookmark = bootcampBookmarkRepository.findByBootcampIdAndUserId(bootcampId, userId)

        if (bookmark?.isActive == true) {
            throw ConflictException(BootcampErrorCode.BOOTCAMP_BOOKMARK_ALREADY_EXISTS)
        }

        try {
            if (bookmark == null) {
                bootcampBookmarkRepository.saveAndFlush(
                    BootcampBookmark(bootcampId = bootcampId, userId = userId),
                )
            } else {
                bookmark.restore()
                bootcampBookmarkRepository.saveAndFlush(bookmark)
            }
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(BootcampErrorCode.BOOTCAMP_BOOKMARK_ALREADY_EXISTS)
        }
    }

    override fun delete(userId: Long, bootcampId: Long, now: LocalDateTime) {
        val bookmark = bootcampBookmarkRepository.findByBootcampIdAndUserId(bootcampId, userId)
            ?.takeIf(BootcampBookmark::isActive)
            ?: return

        bookmark.delete(now)
        bootcampBookmarkRepository.saveAndFlush(bookmark)
    }
}
