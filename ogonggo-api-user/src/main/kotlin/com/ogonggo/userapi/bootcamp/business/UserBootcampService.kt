package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class UserBootcampService(
    private val bootcampReader: BootcampReader,
    private val bootcampContentReader: BootcampContentReader,
    private val bootcampMetricReader: BootcampMetricReader,
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun getBootcamps(page: Int, size: Int, sortType: BootcampSortType): UserBootcampPageResult {
        val result = bootcampReader.readPublicPage(page, size, sortType)
        val bootcampIds = result.bootcamps.map(Bootcamp::requiredId)
        return UserBootcampPageResult.from(result, bootcampMetricReader.readAll(bootcampIds))
    }

    /**
     * 조회됐다는 사실만 알리고 지표 갱신은 수신자에게 맡긴다.
     * 기록이 비동기이므로 상세 응답의 조회 수에는 이번 조회가 아직 반영되지 않는다.
     */
    fun getBootcamp(bootcampId: Long): UserBootcampResult {
        val bootcamp = bootcampReader.readPublic(bootcampId)
        val id = bootcamp.requiredId()
        val result = UserBootcampResult.from(
            bootcamp = bootcamp,
            partners = bootcampContentReader.readPartners(id),
            curriculums = bootcampContentReader.readCurriculums(id),
            metric = bootcampMetricReader.read(id),
        )
        eventPublisher.publishEvent(BootcampViewedEvent(id))
        return result
    }
}
