package com.ogonggo.core.announcement.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.domain.AnnouncementPublicSearchCondition
import com.ogonggo.core.announcement.implement.dto.AnnouncementCategoryDigestDto
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

    fun readPublicPage(condition: AnnouncementPublicSearchCondition, page: Int, size: Int): AnnouncementPageDto {
        validatePageRequest(page, size)
        return announcementQueryRepository.findPublicPage(condition, PageRequest.of(page, size)).toPageDto()
    }

    /**
     * 공지사항 화면의 카테고리 칸들. 모든 카테고리를 정의 순서대로 주고, 공지가 없는 카테고리는 0건과 빈 목록이다.
     * 카테고리가 셋이라 카테고리마다 최근 공지를 따로 읽는다.
     */
    fun readPublicCategoryDigests(recentLimit: Int): List<AnnouncementCategoryDigestDto> {
        val counts = announcementQueryRepository.countPublicByCategory()
        return AnnouncementCategory.entries.map { category ->
            val count = counts[category] ?: 0L
            AnnouncementCategoryDigestDto(
                category = category,
                count = count,
                recentAnnouncements = if (count == 0L) emptyList() else announcementQueryRepository.findRecentPublic(category, recentLimit),
            )
        }
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
