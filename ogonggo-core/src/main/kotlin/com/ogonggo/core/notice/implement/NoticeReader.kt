package com.ogonggo.core.notice.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notice.domain.Notice
import com.ogonggo.core.notice.domain.NoticeManagementSearchCondition
import com.ogonggo.core.notice.error.NoticeErrorCode
import com.ogonggo.core.notice.implement.dto.NoticePageDto
import com.ogonggo.core.notice.persistence.NoticeJpaRepository
import com.ogonggo.core.notice.persistence.NoticeQueryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class NoticeReader internal constructor(
    private val noticeRepository: NoticeJpaRepository,
    private val noticeQueryRepository: NoticeQueryRepository,
) {

    fun read(noticeId: Long): Notice =
        noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
            ?: throw EntityNotFoundException(NoticeErrorCode.NOTICE_NOT_FOUND)

    /** 비노출 공지는 사용자에게 없는 공지와 같다. */
    fun readPublic(noticeId: Long): Notice =
        noticeRepository.findByIdAndPublishedIsTrueAndDeletedAtIsNull(noticeId)
            ?: throw EntityNotFoundException(NoticeErrorCode.NOTICE_NOT_FOUND)

    fun readPublicPage(page: Int, size: Int): NoticePageDto {
        validatePageRequest(page, size)
        return noticeQueryRepository.findPublicPage(PageRequest.of(page, size)).toPageDto()
    }

    fun readManagementPage(condition: NoticeManagementSearchCondition, page: Int, size: Int): NoticePageDto {
        validatePageRequest(page, size)
        return noticeQueryRepository.findManagementPage(condition, PageRequest.of(page, size)).toPageDto()
    }

    fun readForUpdate(noticeId: Long): Notice =
        noticeRepository.findByIdForUpdate(noticeId)
            ?: throw EntityNotFoundException(NoticeErrorCode.NOTICE_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 공지도 잠가 찾는다. */
    fun readForDelete(noticeId: Long): Notice =
        noticeRepository.findIncludingDeletedByIdForUpdate(noticeId)
            ?: throw EntityNotFoundException(NoticeErrorCode.NOTICE_NOT_FOUND)
}

private fun Page<Notice>.toPageDto(): NoticePageDto = NoticePageDto(
    notices = content,
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
