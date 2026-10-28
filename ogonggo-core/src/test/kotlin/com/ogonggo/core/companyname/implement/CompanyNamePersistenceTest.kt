package com.ogonggo.core.companyname.implement

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.companyname.persistence.CompanyNameQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.user.domain.CompanyProfile
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(CompanyNameReader::class, CompanyNameQueryRepository::class)
internal class CompanyNamePersistenceTest @Autowired constructor(
    private val reader: CompanyNameReader,
    private val entityManager: EntityManager,
) {

    @Test
    fun `마감·비노출 공고의 회사명은 나오고 삭제된 공고와 부트캠프의 회사명은 나오지 않는다`() {
        // given
        job("마감고위드").close(NOW)
        job("숨긴고위드", JobPublicationStatus.HIDDEN)
        job("지운고위드").delete(NOW)
        bootcamp("지운고위드캠프").delete(NOW)
        flush()

        // when
        val names = reader.search("고위드")

        // then
        assertEquals(listOf("마감고위드", "숨긴고위드"), names)
    }

    @Test
    fun `세 출처의 같은 이름은 앞뒤 공백을 다듬어 하나로 합치고 표기는 바꾸지 않는다`() {
        // given
        job(" 고위드 ")
        job("고위드")
        bootcamp("고위드")
        companyProfile("고위드  ")
        companyProfile("(주)고위드")
        flush()

        // when
        val names = reader.search("고위드")

        // then
        assertEquals(listOf("고위드", "(주)고위드"), names)
    }

    @Test
    fun `검색어로 시작하는 이름이 먼저이고 같은 묶음에서는 짧은 이름이 먼저이며 최대 10개다`() {
        // given
        job("주식회사 렛츠커리어")
        bootcamp("렛츠커리어 교육")
        companyProfile("렛츠커리어")
        job("LETSCAREER")
        (1..10).forEach { job("오공고 협력사 렛츠커리어 $it") }
        flush()

        // when
        val names = reader.search(" 렛츠커리어 ")

        // then
        assertEquals(
            listOf(
                "렛츠커리어",
                "렛츠커리어 교육",
                "주식회사 렛츠커리어",
                "오공고 협력사 렛츠커리어 1",
                "오공고 협력사 렛츠커리어 2",
                "오공고 협력사 렛츠커리어 3",
                "오공고 협력사 렛츠커리어 4",
                "오공고 협력사 렛츠커리어 5",
                "오공고 협력사 렛츠커리어 6",
                "오공고 협력사 렛츠커리어 7",
            ),
            names,
        )
        assertEquals(listOf("LETSCAREER"), reader.search("letscareer"))
    }

    @Test
    fun `검색어가 비면 빈 목록이다`() {
        job("고위드")
        flush()

        assertEquals(emptyList<String>(), reader.search("  "))
    }

    private fun job(companyName: String, publicationStatus: JobPublicationStatus = JobPublicationStatus.PUBLISHED): Job =
        Job(
            companyName = companyName,
            title = "백엔드 개발자",
            employmentType = JobEmploymentType.FULL_TIME,
            experienceType = JobExperienceType.EXPERIENCED,
            experienceMinYears = 1,
            recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
            publicationStatus = publicationStatus,
            sourceUrl = "https://example.com/jobs/1",
            listSortKey = 0L,
            now = NOW,
        ).also(entityManager::persist)

    private fun bootcamp(companyName: String): Bootcamp =
        Bootcamp(
            companyName = companyName,
            title = "백엔드 부트캠프",
            programType = "개발",
            operationType = BootcampOperationType.ONLINE,
            recruitmentType = BootcampRecruitmentType.PERIOD,
            recruitmentStartAt = LocalDateTime.of(2026, 8, 1, 0, 0),
            recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
            programStartDate = LocalDate.of(2026, 9, 1),
            programEndDate = LocalDate.of(2026, 12, 1),
            tuitionType = BootcampTuitionType.FREE,
            representativeImageUrl = null,
            shortDescription = "백엔드 개발자로 성장하는 12주",
            content = "부트캠프 상세 내용",
            applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
            applicationUrl = "https://example.com/apply",
        ).also(entityManager::persist)

    private fun companyProfile(organizationName: String) {
        entityManager.persist(CompanyProfile(userId = ++userId, organizationName = organizationName, managerName = "김담당"))
    }

    private fun flush() {
        entityManager.flush()
        entityManager.clear()
    }

    private var userId = 0L

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 28, 12, 0)
    }
}
