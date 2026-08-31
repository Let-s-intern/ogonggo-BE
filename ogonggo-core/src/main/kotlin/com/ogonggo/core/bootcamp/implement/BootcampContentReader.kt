package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.persistence.BootcampCurriculumJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampPartnerJpaRepository
import org.springframework.stereotype.Component

interface BootcampContentReader {
    fun readPartners(bootcampId: Long): List<BootcampPartnerData>
    fun readCurriculums(bootcampId: Long): List<BootcampCurriculumData>
}

@Component
internal class BootcampContentReaderImpl(
    private val bootcampPartnerRepository: BootcampPartnerJpaRepository,
    private val bootcampCurriculumRepository: BootcampCurriculumJpaRepository,
) : BootcampContentReader {

    override fun readPartners(bootcampId: Long): List<BootcampPartnerData> {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        return bootcampPartnerRepository
            .findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
            .map { BootcampPartnerData(name = it.partnerName, displayOrder = it.displayOrder) }
    }

    override fun readCurriculums(bootcampId: Long): List<BootcampCurriculumData> {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        return bootcampCurriculumRepository
            .findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
            .map {
                BootcampCurriculumData(
                    startWeek = it.startWeek,
                    endWeek = it.endWeek,
                    subtitle = it.subtitle,
                    displayOrder = it.displayOrder,
                )
            }
    }
}

data class BootcampPartnerData(
    val name: String,
    val displayOrder: Int,
)

data class BootcampCurriculumData(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
    val displayOrder: Int,
)
