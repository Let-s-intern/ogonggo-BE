package com.ogonggo.core.announcement.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.error.AnnouncementErrorCode
import com.ogonggo.core.announcement.implement.dto.AnnouncementPageDto
import com.ogonggo.core.announcement.persistence.AnnouncementJpaRepository
import com.ogonggo.core.announcement.persistence.AnnouncementQueryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class AnnouncementReader internal constructor(
    private val announcementRepository: AnnouncementJpaRepository,
    private val announcementQueryRepository: AnnouncementQueryRepository,
) {

    fun read(announcementId: Long): Announcement =
        announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
            ?: throw EntityNotFoundException(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND)

    /** 비노출 공지는 사용자에게 없는 공지와 같다. */
    fun readPublic(announcementId: Long): Announcement =
        announcementRepository.findByIdAndPublishedIsTrueAndDeletedAtIsNull(announcementId)
            ?: throw EntityNotFoundException(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND)

    fun readPublicPage(page: Int, size: Int): AnnouncementPageDto {
        validatePageRequest(page, size)
        return announcementQueryRepository.findPublicPage(PageRequest.of(page, size)).toPageDto()
    }

    fun readManagementPage(condition: AnnouncementManagementSearchCondition, page: Int, size: Int): AnnouncementPageDto {
        validatePageRequest(page, size)
        return announcementQueryRepository.findManagementPage(condition, PageRequest.of(page, size)).toPageDto()
    }

    fun readForUpdate(announcementId: Long): Announcement =
        announcementRepository.findByIdForUpdate(announcementId)
            ?: throw EntityNotFoundException(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 공지도 잠가 찾는다. */
    fun readForDelete(announcementId: Long): Announcement =
        announcementRepository.findIncludingDeletedByIdForUpdate(announcementId)
            ?: throw EntityNotFoundException(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND)
}

private fun Page<Announcement>.toPageDto(): AnnouncementPageDto = AnnouncementPageDto(
    announcements = content,
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
