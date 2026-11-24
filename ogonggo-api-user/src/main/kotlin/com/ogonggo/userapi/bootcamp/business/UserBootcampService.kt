package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.companycontentpriority.implement.CompanyContentPriorityReader
import com.ogonggo.core.sourceurlclick.domain.SourceUrlClickTargetType
import com.ogonggo.core.sourceurlclick.implement.SourceUrlClickAppender
import org.springframework.stereotype.Service

@Service
class UserBootcampService(
    private val bootcampReader: BootcampReader,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
    private val bootcampContentReader: BootcampContentReader,
    private val sourceUrlClickAppender: SourceUrlClickAppender,
    private val bootcampMetricReader: BootcampMetricReader,
    private val companyContentPriorityReader: CompanyContentPriorityReader,
) {

    /**
     * 로그인 없이 조회할 수 있어 userId가 없을 수 있고, 그때는 북마크가 하나도 없는 것으로 본다.
     * 마감 전 기업회원 부트캠프를 맨 앞에 둘지는 운영자가 관리자 콘솔에서 정한 값을 요청마다 읽는다.
     */
    fun getBootcamps(
        userId: Long?,
        condition: BootcampSearchCondition,
        sortType: BootcampSortType,
        page: Int,
        size: Int,
    ): UserBootcampPageResult {
        val result = bootcampReader.readPublicPage(condition, sortType, page, size, companyContentPriorityReader.isEnabledOrDefault())
        val bootcampIds = result.bootcamps.map(Bootcamp::requiredId)
        return UserBootcampPageResult.from(
            result = result,
            bookmarkedBootcampIds = readBookmarkedBootcampIds(userId, bootcampIds),
            metrics = bootcampMetricReader.readAll(bootcampIds),
        )
    }

    /** 읽기만으로는 조회 수를 올리지 않는다. 조회 수는 브라우저가 보내는 조회 이벤트(`POST /api/v1/events`)로 센다. */
    fun getBootcamp(userId: Long?, bootcampId: Long): UserBootcampResult {
        val bootcamp = bootcampReader.readPublic(bootcampId)
        val id = bootcamp.requiredId()
        return UserBootcampResult.from(
            bootcamp = bootcamp,
            bookmarked = id in readBookmarkedBootcampIds(userId, listOf(id)),
            partners = bootcampContentReader.readPartners(id),
            curriculums = bootcampContentReader.readCurriculums(id),
            images = bootcampContentReader.readImages(id),
            metric = bootcampMetricReader.read(id),
        )
    }

    /**
     * 외부 지원 페이지로 이동한 사용자를 기록한다.
     * 같은 사용자가 다시 눌러도 실패로 만들지 않고 최초 기록을 유지한다.
     *
     * 쓰기가 한 건뿐이라 묶어야 할 원자성이 없으므로 트랜잭션을 열지 않는다.
     * 열어 두면 동시에 누른 두 요청 중 하나가 유니크 제약에 걸릴 때
     * 그 실패가 트랜잭션을 롤백 대상으로 만들어, 기록은 이미 남았는데도 응답이 실패한다.
     */
    fun recordSourceUrlClick(userId: Long, bootcampId: Long) {
        bootcampReader.readPublic(bootcampId)
        sourceUrlClickAppender.append(SourceUrlClickTargetType.BOOTCAMP, bootcampId, userId)
    }

    /** 비로그인 조회에서는 북마크 저장소를 아예 건드리지 않는다. */
    private fun readBookmarkedBootcampIds(userId: Long?, bootcampIds: Collection<Long>): Set<Long> =
        if (userId == null) emptySet() else bootcampBookmarkReader.readBookmarkedBootcampIds(userId, bootcampIds)
}
