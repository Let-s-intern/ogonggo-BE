package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.implement.BootcampApplicationUrlClickAppender
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class UserBootcampService(
    private val bootcampReader: BootcampReader,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
    private val bootcampContentReader: BootcampContentReader,
    private val bootcampApplicationUrlClickAppender: BootcampApplicationUrlClickAppender,
    private val bootcampMetricReader: BootcampMetricReader,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /** 로그인 없이 조회할 수 있어 userId가 없을 수 있고, 그때는 북마크가 하나도 없는 것으로 본다. */
    fun getBootcamps(
        userId: Long?,
        condition: BootcampSearchCondition,
        sortType: BootcampSortType,
        page: Int,
        size: Int,
    ): UserBootcampPageResult {
        val result = bootcampReader.readPublicPage(condition, sortType, page, size)
        val bootcampIds = result.bootcamps.map(Bootcamp::requiredId)
        return UserBootcampPageResult.from(
            result = result,
            bookmarkedBootcampIds = readBookmarkedBootcampIds(userId, bootcampIds),
            metrics = bootcampMetricReader.readAll(bootcampIds),
        )
    }

    /**
     * 조회됐다는 사실만 알리고 지표 갱신은 수신자에게 맡긴다.
     * 기록이 비동기이므로 상세 응답의 조회 수에는 이번 조회가 아직 반영되지 않는다.
     */
    fun getBootcamp(userId: Long?, bootcampId: Long): UserBootcampResult {
        val bootcamp = bootcampReader.readPublic(bootcampId)
        val id = bootcamp.requiredId()
        val result = UserBootcampResult.from(
            bootcamp = bootcamp,
            bookmarked = id in readBookmarkedBootcampIds(userId, listOf(id)),
            partners = bootcampContentReader.readPartners(id),
            curriculums = bootcampContentReader.readCurriculums(id),
            metric = bootcampMetricReader.read(id),
        )
        eventPublisher.publishEvent(BootcampViewedEvent(id))
        return result
    }

    /**
     * 외부 지원 페이지로 이동한 사용자를 기록한다.
     * 같은 사용자가 다시 눌러도 실패로 만들지 않고 최초 기록을 유지한다.
     *
     * 쓰기가 한 건뿐이라 묶어야 할 원자성이 없으므로 트랜잭션을 열지 않는다.
     * 열어 두면 동시에 누른 두 요청 중 하나가 유니크 제약에 걸릴 때
     * 그 실패가 트랜잭션을 롤백 대상으로 만들어, 기록은 이미 남았는데도 응답이 실패한다.
     */
    fun recordApplicationUrlClick(userId: Long, bootcampId: Long) {
        bootcampReader.readPublic(bootcampId)
        bootcampApplicationUrlClickAppender.append(userId, bootcampId)
    }

    /** 비로그인 조회에서는 북마크 저장소를 아예 건드리지 않는다. */
    private fun readBookmarkedBootcampIds(userId: Long?, bootcampIds: Collection<Long>): Set<Long> =
        if (userId == null) emptySet() else bootcampBookmarkReader.readBookmarkedBootcampIds(userId, bootcampIds)
}
