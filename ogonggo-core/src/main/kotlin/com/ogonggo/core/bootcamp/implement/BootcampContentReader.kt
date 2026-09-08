package com.ogonggo.core.bootcamp.implement

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
            .map {
                BootcampCurriculumDto.Response(
                    startWeek = it.startWeek,
                    endWeek = it.endWeek,
                    subtitle = it.subtitle,
                    displayOrder = it.displayOrder,
                )
            }
    }
}
