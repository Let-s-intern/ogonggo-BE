package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampCurriculum
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.persistence.BootcampCurriculumJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampPartnerJpaRepository
import org.springframework.stereotype.Component

@Component
class BootcampContentReader internal constructor(
    private val bootcampPartnerRepository: BootcampPartnerJpaRepository,
    private val bootcampCurriculumRepository: BootcampCurriculumJpaRepository,
) {

    fun readPartners(bootcampId: Long): List<BootcampPartnerDto.Response> {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        return bootcampPartnerRepository
            .findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
            .map { BootcampPartnerDto.Response(name = it.partnerName, displayOrder = it.displayOrder) }
    }

    fun readCurriculums(bootcampId: Long): List<BootcampCurriculumDto.Response> {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        return bootcampCurriculumRepository
            .findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
            .map { it.toResponse() }
    }

    /** 여러 부트캠프를 한 번에 보여 줄 때 N+1을 피하려고 한 번에 읽는다. 커리큘럼이 없으면 빈 목록이다. */
    fun readCurriculums(bootcampIds: Collection<Long>): Map<Long, List<BootcampCurriculumDto.Response>> {
        if (bootcampIds.isEmpty()) {
            return emptyMap()
        }
        val curriculums = bootcampCurriculumRepository
            .findAllByBootcampIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampIds.toSet())
            .groupBy({ it.bootcampId }, { it.toResponse() })
        return bootcampIds.associateWith { curriculums[it].orEmpty() }
    }

    private fun BootcampCurriculum.toResponse(): BootcampCurriculumDto.Response = BootcampCurriculumDto.Response(
        startWeek = startWeek,
        endWeek = endWeek,
        subtitle = subtitle,
        displayOrder = displayOrder,
    )
}
