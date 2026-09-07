package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.persistence.BootcampApplicationUrlClickJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampBookmarkJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampCurriculumJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampMetricJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampQueryRepository
import com.ogonggo.core.bootcamp.persistence.BootcampPartnerJpaRepository
import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    BootcampReaderImpl::class,
    BootcampQueryRepository::class,
    BootcampContentReaderImpl::class,
    BootcampAppenderImpl::class,
    BootcampManagerImpl::class,
    BootcampMetricReaderImpl::class,
    BootcampMetricManagerImpl::class,
    BootcampMetricRegistrar::class,
    BootcampBookmarkManagerImpl::class,
    BootcampBookmarkReaderImpl::class,
    BootcampApplicationUrlClickAppenderImpl::class,
)
internal class BootcampImplementPersistenceTest @Autowired constructor(
    private val bootcampReader: BootcampReader,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val bootcampContentReader: BootcampContentReader,
    private val bootcampMetricReader: BootcampMetricReader,
    private val bootcampMetricManager: BootcampMetricManager,
    private val bootcampBookmarkManager: BootcampBookmarkManager,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
    private val bootcampApplicationUrlClickAppender: BootcampApplicationUrlClickAppender,
    private val bootcampBookmarkRepository: BootcampBookmarkJpaRepository,
    private val bootcampApplicationUrlClickRepository: BootcampApplicationUrlClickJpaRepository,
    private val bootcampMetricRepository: BootcampMetricJpaRepository,
    private val bootcampPartnerRepository: BootcampPartnerJpaRepository,
    private val bootcampCurriculumRepository: BootcampCurriculumJpaRepository,
) {

    @Test
    fun `Appender로 저장하고 Reader로 조회한다`() {
        val savedBootcamp = bootcampAppender.append(createCommand())

        assertNotNull(savedBootcamp.id)
        assertEquals(savedBootcamp.id, bootcampReader.read(checkNotNull(savedBootcamp.id)).id)
    }

    @Test
    fun `Appender는 파트너사와 커리큘럼을 함께 저장한다`() {
        val savedBootcamp = bootcampAppender.append(
            createCommand(
                partners = listOf(BootcampPartnerCommand("오공고 파트너", 0)),
                curriculums = listOf(BootcampCurriculumCommand(1, 4, "Spring 기초", 0)),
            ),
        )
        val bootcampId = checkNotNull(savedBootcamp.id)

        assertEquals(
            listOf("오공고 파트너"),
            bootcampPartnerRepository.findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId).map { it.partnerName },
        )
        assertEquals(
            listOf("Spring 기초"),
            bootcampCurriculumRepository.findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId).map { it.subtitle },
        )
    }

    @Test
    fun `Manager는 파트너사와 커리큘럼을 전체 교체한다`() {
        val savedBootcamp = bootcampAppender.append(
            createCommand(
                partners = listOf(BootcampPartnerCommand("기존 파트너", 0)),
                curriculums = listOf(BootcampCurriculumCommand(1, 2, "기존 과정", 0)),
            ),
        )
        val bootcampId = checkNotNull(savedBootcamp.id)

        bootcampManager.update(
            savedBootcamp,
            updateCommand(
                partners = listOf(BootcampPartnerCommand("신규 파트너", 0)),
                curriculums = listOf(BootcampCurriculumCommand(3, 6, "심화 과정", 0)),
            ),
        )

        assertEquals(
            listOf("신규 파트너"),
            bootcampPartnerRepository.findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
                .map { it.partnerName },
        )
        assertEquals(
            listOf("심화 과정"),
            bootcampCurriculumRepository.findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId)
                .map { it.subtitle },
        )
        assertEquals(2, bootcampPartnerRepository.findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId).size)
        assertEquals(2, bootcampCurriculumRepository.findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId).size)
        assertEquals(
            true,
            bootcampPartnerRepository.findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId)
                .first { it.partnerName == "기존 파트너" }.deletedAt != null,
        )
        assertEquals(
            true,
            bootcampCurriculumRepository.findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId)
                .first { it.subtitle == "기존 과정" }.deletedAt != null,
        )
    }

    @Test
    fun `동일 파트너사를 재등록하면 삭제된 연결을 복구한다`() {
        val savedBootcamp = bootcampAppender.append(
            createCommand(partners = listOf(BootcampPartnerCommand("동일 파트너", 0))),
        )
        val bootcampId = checkNotNull(savedBootcamp.id)
        val partnerId = bootcampPartnerRepository.findAllByBootcampId(bootcampId).single().id

        bootcampManager.update(savedBootcamp, updateCommand(partners = emptyList(), curriculums = emptyList()))
        bootcampManager.update(
            savedBootcamp,
            updateCommand(
                partners = listOf(BootcampPartnerCommand("동일 파트너", 3)),
                curriculums = emptyList(),
            ),
        )

        val partners = bootcampPartnerRepository.findAllByBootcampId(bootcampId)
        assertEquals(1, partners.size)
        assertEquals(partnerId, partners.single().id)
        assertEquals(null, partners.single().deletedAt)
        assertEquals(3, partners.single().displayOrder)
    }

    @Test
    fun `ContentReader는 활성 파트너사와 커리큘럼만 순서대로 조회한다`() {
        val savedBootcamp = bootcampAppender.append(
            createCommand(
                partners = listOf(
                    BootcampPartnerCommand("두 번째", 2),
                    BootcampPartnerCommand("첫 번째", 1),
                ),
                curriculums = listOf(
                    BootcampCurriculumCommand(5, 8, "심화", 2),
                    BootcampCurriculumCommand(1, 4, "기초", 1),
                ),
            ),
        )
        val bootcampId = checkNotNull(savedBootcamp.id)

        bootcampManager.update(
            savedBootcamp,
            updateCommand(
                partners = listOf(BootcampPartnerCommand("첫 번째", 0)),
                curriculums = listOf(BootcampCurriculumCommand(9, 12, "프로젝트", 0)),
            ),
        )

        assertEquals(listOf("첫 번째"), bootcampContentReader.readPartners(bootcampId).map { it.name })
        assertEquals(listOf("프로젝트"), bootcampContentReader.readCurriculums(bootcampId).map { it.subtitle })
    }

    @Test
    fun `사용자 조회는 게시된 부트캠프만 반환한다`() {
        val savedBootcamp = bootcampAppender.append(createCommand())
        val bootcampId = checkNotNull(savedBootcamp.id)

        val exception = assertThrows(EntityNotFoundException::class.java) {
            bootcampReader.readPublic(bootcampId)
        }
        assertEquals(BootcampErrorCode.BOOTCAMP_NOT_FOUND, exception.errorCode)

        val lockedBootcamp = bootcampReader.readForUpdate(bootcampId)
        bootcampManager.startRecruitment(lockedBootcamp)

        assertEquals(
            BootcampStatus.RECRUITING,
            bootcampReader.readPublic(bootcampId).status,
        )
    }

    @Test
    fun `사용자 조회는 공고 공개 기간 안의 부트캠프만 반환한다`() {
        val publicationStartAt = LocalDateTime.of(2026, 9, 1, 0, 0)
        val publicationEndAt = LocalDateTime.of(2026, 9, 30, 23, 59)
        val savedBootcamp = bootcampAppender.append(
            createCommand(
                publicationStartAt = publicationStartAt,
                publicationEndAt = publicationEndAt,
            ),
        )
        val bootcampId = checkNotNull(savedBootcamp.id)
        bootcampManager.startRecruitment(bootcampReader.readForUpdate(bootcampId))

        assertThrows(EntityNotFoundException::class.java) {
            bootcampReader.readPublic(bootcampId, publicationStartAt.minusNanos(1))
        }
        assertEquals(bootcampId, bootcampReader.readPublic(bootcampId, publicationStartAt).id)
        assertEquals(bootcampId, bootcampReader.readPublic(bootcampId, publicationEndAt).id)
        assertThrows(EntityNotFoundException::class.java) {
            bootcampReader.readPublic(bootcampId, publicationEndAt.plusNanos(1))
        }
    }

    @Test
    fun `사용자 부트캠프 목록은 게시 및 공개 기간 조건을 적용해 최신순으로 페이징한다`() {
        val now = LocalDateTime.of(2026, 9, 15, 12, 0)
        val first = bootcampAppender.append(createCommand())
        val latestVisible = bootcampAppender.append(createCommand(publicationEndAt = now.plusDays(1)))
        val future = bootcampAppender.append(createCommand(publicationStartAt = now.plusDays(1)))
        listOf(first, latestVisible, future).forEach(bootcampManager::startRecruitment)

        val firstPage = publicPage(page = 0, size = 1, now = now)
        val secondPage = publicPage(page = 1, size = 1, now = now)

        assertEquals(listOf(latestVisible.id), firstPage.bootcamps.map { it.id })
        assertEquals(listOf(first.id), secondPage.bootcamps.map { it.id })
        assertEquals(2L, firstPage.totalElements)
        assertEquals(true, firstPage.hasNext)
        assertEquals(false, secondPage.hasNext)
    }

    @Test
    fun `수강료 유형과 모집 상태와 검색어로 목록을 좁힌다`() {
        val now = LocalDateTime.of(2026, 9, 15, 12, 0)
        val freeRecruiting = bootcampAppender.append(
            createCommand(companyName = "오공고 교육사", title = "백엔드 부트캠프", tuitionType = TuitionType.FREE),
        )
        val paidRecruiting = bootcampAppender.append(
            createCommand(companyName = "다른 교육사", title = "데이터 부트캠프", tuitionType = TuitionType.PAID),
        )
        val freeClosed = bootcampAppender.append(
            createCommand(companyName = "오공고 교육사", title = "프론트 부트캠프", tuitionType = TuitionType.FREE),
        )
        listOf(freeRecruiting, paidRecruiting, freeClosed).forEach(bootcampManager::startRecruitment)
        bootcampManager.close(bootcampReader.readForUpdate(checkNotNull(freeClosed.id)), now)

        assertEquals(
            listOf(freeClosed.id, freeRecruiting.id),
            publicPage(condition = BootcampSearchCondition(tuitionType = TuitionType.FREE), now = now)
                .bootcamps.map { it.id },
        )
        assertEquals(
            listOf(paidRecruiting.id, freeRecruiting.id),
            publicPage(condition = BootcampSearchCondition(status = BootcampStatus.RECRUITING), now = now)
                .bootcamps.map { it.id },
        )
        assertEquals(
            listOf(freeClosed.id),
            publicPage(condition = BootcampSearchCondition(status = BootcampStatus.CLOSED), now = now)
                .bootcamps.map { it.id },
        )
    }

    @Test
    fun `검색어는 회사명과 프로그램명을 대소문자 없이 찾고 다른 필터와 함께 걸린다`() {
        val now = LocalDateTime.of(2026, 9, 15, 12, 0)
        val byTitle = bootcampAppender.append(
            createCommand(companyName = "다른 교육사", title = "Spring 백엔드 부트캠프", tuitionType = TuitionType.FREE),
        )
        val byCompany = bootcampAppender.append(
            createCommand(companyName = "SPRING 교육사", title = "데이터 부트캠프", tuitionType = TuitionType.PAID),
        )
        val unrelated = bootcampAppender.append(
            createCommand(companyName = "다른 교육사", title = "디자인 부트캠프", tuitionType = TuitionType.FREE),
        )
        listOf(byTitle, byCompany, unrelated).forEach(bootcampManager::startRecruitment)

        assertEquals(
            listOf(byCompany.id, byTitle.id),
            publicPage(condition = BootcampSearchCondition(keyword = "spring"), now = now).bootcamps.map { it.id },
        )
        // 검색어는 필터와 AND로 결합한다.
        assertEquals(
            listOf(byTitle.id),
            publicPage(
                condition = BootcampSearchCondition(tuitionType = TuitionType.FREE, keyword = "spring"),
                now = now,
            ).bootcamps.map { it.id },
        )
        // 공백만 있는 검색어는 검색하지 않은 것과 같다.
        assertEquals(3L, publicPage(condition = BootcampSearchCondition(keyword = " "), now = now).totalElements)
    }

    @Test
    fun `지원 페이지 이동은 사용자와 부트캠프마다 한 행만 남긴다`() {
        val bootcampId = checkNotNull(bootcampAppender.append(createCommand()).id)
        val otherBootcampId = checkNotNull(bootcampAppender.append(createCommand()).id)

        bootcampApplicationUrlClickAppender.append(USER_ID, bootcampId)
        bootcampApplicationUrlClickAppender.append(USER_ID, bootcampId)
        bootcampApplicationUrlClickAppender.append(OTHER_USER_ID, bootcampId)
        bootcampApplicationUrlClickAppender.append(USER_ID, otherBootcampId)

        assertEquals(3L, bootcampApplicationUrlClickRepository.count())
        assertEquals(
            true,
            bootcampApplicationUrlClickRepository.existsByBootcampIdAndUserId(bootcampId, USER_ID),
        )
        assertEquals(
            false,
            bootcampApplicationUrlClickRepository.existsByBootcampIdAndUserId(otherBootcampId, OTHER_USER_ID),
        )
    }

    private fun publicPage(
        condition: BootcampSearchCondition = BootcampSearchCondition.NONE,
        sortType: BootcampSortType = BootcampSortType.LATEST,
        page: Int = 0,
        size: Int = 10,
        now: LocalDateTime,
    ): BootcampPage = bootcampReader.readPublicPage(
        condition = condition,
        sortType = sortType,
        page = page,
        size = size,
        now = now,
    )

    @Test
    fun `사용자 부트캠프 목록의 페이지 범위를 검증한다`() {
        val none = BootcampSearchCondition.NONE
        val latest = BootcampSortType.LATEST
        assertThrows(IllegalArgumentException::class.java) { bootcampReader.readPublicPage(none, latest, -1, 20) }
        assertThrows(IllegalArgumentException::class.java) { bootcampReader.readPublicPage(none, latest, 0, 0) }
        assertThrows(IllegalArgumentException::class.java) { bootcampReader.readPublicPage(none, latest, 0, 101) }
    }

    @Test
    fun `공개 시작과 종료가 각각 없으면 공개 기간을 개방한다`() {
        val boundary = LocalDateTime.of(2026, 9, 1, 0, 0)
        val noStart = bootcampAppender.append(createCommand(publicationEndAt = boundary))
        val noStartId = checkNotNull(noStart.id)
        bootcampManager.startRecruitment(bootcampReader.readForUpdate(noStartId))

        assertEquals(noStartId, bootcampReader.readPublic(noStartId, boundary.minusDays(1)).id)
        assertThrows(EntityNotFoundException::class.java) {
            bootcampReader.readPublic(noStartId, boundary.plusNanos(1))
        }

        val noEnd = bootcampAppender.append(createCommand(publicationStartAt = boundary))
        val noEndId = checkNotNull(noEnd.id)
        bootcampManager.startRecruitment(bootcampReader.readForUpdate(noEndId))

        assertThrows(EntityNotFoundException::class.java) {
            bootcampReader.readPublic(noEndId, boundary.minusNanos(1))
        }
        assertEquals(noEndId, bootcampReader.readPublic(noEndId, boundary.plusDays(1)).id)
    }

    @Test
    fun `삭제된 부트캠프는 Reader에서 조회하지 않는다`() {
        val savedBootcamp = bootcampAppender.append(createCommand())
        val bootcampId = checkNotNull(savedBootcamp.id)
        val lockedBootcamp = bootcampReader.readForUpdate(bootcampId)

        bootcampManager.delete(lockedBootcamp, java.time.LocalDateTime.of(2026, 8, 26, 12, 0))

        assertThrows(EntityNotFoundException::class.java) { bootcampReader.read(bootcampId) }
        assertThrows(EntityNotFoundException::class.java) { bootcampReader.readForUpdate(bootcampId) }
    }

    private fun createCommand(
        publicationStartAt: LocalDateTime? = null,
        publicationEndAt: LocalDateTime? = null,
        partners: List<BootcampPartnerCommand> = emptyList(),
        curriculums: List<BootcampCurriculumCommand> = emptyList(),
        companyName: String = "오공고 교육사",
        title: String = "백엔드 부트캠프",
        tuitionType: TuitionType = TuitionType.FREE,
    ): BootcampAppendCommand = BootcampAppendCommand(
        companyName = companyName,
        title = title,
        programType = "개발",
        operationType = OperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 8, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
        programStartDate = LocalDate.of(2026, 9, 1),
        programEndDate = LocalDate.of(2026, 12, 1),
        capacity = 30,
        tuitionType = tuitionType,
        tuitionAmount = 0,
        representativeImageUrl = "https://example.com/images/bootcamp.png",
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
        publicationStartAt = publicationStartAt,
        publicationEndAt = publicationEndAt,
        partners = partners,
        curriculums = curriculums,
    )

    @Test
    fun `조회수순은 조회 수 내림차순이며 같으면 최신순으로 정렬한다`() {
        val now = LocalDateTime.of(2026, 9, 15, 12, 0)
        val quiet = bootcampAppender.append(createCommand())
        val tied = bootcampAppender.append(createCommand())
        val popular = bootcampAppender.append(createCommand())
        listOf(quiet, tied, popular).forEach(bootcampManager::startRecruitment)
        repeat(2) { bootcampMetricManager.increaseViewCount(checkNotNull(popular.id), NOW) }
        bootcampMetricManager.increaseViewCount(checkNotNull(tied.id), NOW)

        val page = bootcampReader.readPublicPage(
            condition = BootcampSearchCondition.NONE,
            sortType = BootcampSortType.VIEW_COUNT,
            page = 0,
            size = 10,
            now = now,
        )

        assertEquals(listOf(popular.id, tied.id, quiet.id), page.bootcamps.map { it.id })
        assertEquals(3L, page.totalElements)
    }

    @Test
    fun `조회 수는 지표 행이 없으면 만들고 있으면 증가시킨다`() {
        val bootcampId = checkNotNull(bootcampAppender.append(createCommand()).id)

        bootcampMetricManager.increaseViewCount(bootcampId, NOW)

        assertEquals(1L, bootcampMetricReader.read(bootcampId).viewCount)

        bootcampMetricManager.increaseViewCount(bootcampId, NOW.plusMinutes(1))

        assertEquals(2L, bootcampMetricReader.read(bootcampId).viewCount)
        assertEquals(1, bootcampMetricRepository.findAllByBootcampIdIn(listOf(bootcampId)).size)
    }

    @Test
    fun `지표를 한 번에 조회하며 지표 행이 없는 부트캠프는 0으로 채운다`() {
        val viewedId = checkNotNull(bootcampAppender.append(createCommand()).id)
        val untouchedId = checkNotNull(bootcampAppender.append(createCommand()).id)
        bootcampMetricManager.increaseViewCount(viewedId, NOW)

        val metrics = bootcampMetricReader.readAll(listOf(viewedId, untouchedId))

        assertEquals(1L, metrics[viewedId]?.viewCount)
        assertEquals(0L, metrics[untouchedId]?.viewCount)
        assertEquals(emptyMap<Long, BootcampMetricData>(), bootcampMetricReader.readAll(emptyList()))
    }

    @Test
    fun `북마크 등록과 해제와 재등록은 행을 보존하고 지표는 다시 세어 맞춘다`() {
        val bootcampId = checkNotNull(bootcampAppender.append(createCommand()).id)

        bootcampBookmarkManager.append(USER_ID, bootcampId, NOW)
        bootcampMetricManager.syncBookmarkCount(bootcampId, NOW)

        assertEquals(1L, bootcampMetricReader.read(bootcampId).bookmarkCount)
        assertEquals(setOf(bootcampId), bootcampBookmarkReader.readBookmarkedBootcampIds(USER_ID, listOf(bootcampId)))

        bootcampBookmarkManager.delete(USER_ID, bootcampId, NOW.plusMinutes(1))
        bootcampBookmarkManager.delete(USER_ID, bootcampId, NOW.plusMinutes(2))
        bootcampMetricManager.syncBookmarkCount(bootcampId, NOW)

        assertEquals(0L, bootcampMetricReader.read(bootcampId).bookmarkCount)
        assertEquals(emptySet<Long>(), bootcampBookmarkReader.readBookmarkedBootcampIds(USER_ID, listOf(bootcampId)))
        // 이미 해제된 북마크를 다시 해제해도 최초 해제 일시가 덮어써지지 않는다.
        assertEquals(
            NOW.plusMinutes(1),
            bootcampBookmarkRepository.findByBootcampIdAndUserId(bootcampId, USER_ID)?.deletedAt,
        )

        bootcampBookmarkManager.append(USER_ID, bootcampId, NOW.plusMinutes(3))
        bootcampMetricManager.syncBookmarkCount(bootcampId, NOW)

        val restored = bootcampBookmarkRepository.findByBootcampIdAndUserId(bootcampId, USER_ID)
        assertEquals(1L, bootcampMetricReader.read(bootcampId).bookmarkCount)
        assertEquals(null, restored?.deletedAt)
        // 북마크 목록이 수정 일시로 정렬하므로 복구는 벌크 갱신에서도 수정 일시를 남겨야 한다.
        assertEquals(NOW.plusMinutes(3), restored?.updatedAt)
        assertEquals(1L, bootcampBookmarkRepository.count())
    }

    @Test
    fun `이미 등록한 북마크를 다시 등록하면 유니크 제약이 막는다`() {
        val bootcampId = checkNotNull(bootcampAppender.append(createCommand()).id)
        bootcampBookmarkManager.append(USER_ID, bootcampId, NOW)

        val duplicate = assertThrows(ConflictException::class.java) {
            bootcampBookmarkManager.append(USER_ID, bootcampId, NOW.plusMinutes(1))
        }

        assertEquals(BootcampErrorCode.BOOTCAMP_BOOKMARK_ALREADY_EXISTS, duplicate.errorCode)
    }

    @Test
    fun `북마크 목록은 공개된 부트캠프만 최근 북마크 순으로 반환한다`() {
        val olderId = startedRecruitmentBootcampId()
        val newerId = startedRecruitmentBootcampId()
        val draftId = checkNotNull(bootcampAppender.append(createCommand()).id)
        bootcampBookmarkManager.append(USER_ID, olderId, NOW)
        bootcampBookmarkManager.append(USER_ID, newerId, NOW.plusMinutes(1))
        bootcampBookmarkManager.append(USER_ID, draftId, NOW.plusMinutes(2))

        val page = bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, 0, 10, NOW)

        // 게시되지 않은 부트캠프는 북마크가 있어도 목록에서 빠진다.
        assertEquals(listOf(newerId, olderId), page.bootcamps.map { it.id })
        assertEquals(2L, page.totalElements)

        bootcampBookmarkManager.delete(USER_ID, newerId, NOW.plusMinutes(3))

        assertEquals(
            listOf(olderId),
            bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, 0, 10, NOW).bootcamps.map { it.id },
        )
    }

    @Test
    fun `북마크 목록의 페이지 요청 범위를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, -1, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, 0, 0)
        }
        assertEquals(emptySet<Long>(), bootcampBookmarkReader.readBookmarkedBootcampIds(USER_ID, emptyList()))
    }

    private fun startedRecruitmentBootcampId(): Long {
        val bootcampId = checkNotNull(bootcampAppender.append(createCommand()).id)
        bootcampManager.startRecruitment(bootcampReader.readForUpdate(bootcampId))
        return bootcampId
    }

    private fun updateCommand(
        partners: List<BootcampPartnerCommand>,
        curriculums: List<BootcampCurriculumCommand>,
    ): BootcampUpdateCommand = BootcampUpdateCommand(
        companyName = "변경 교육사",
        title = "변경 부트캠프",
        programType = "데이터",
        operationType = OperationType.OFFLINE,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59),
        programStartDate = LocalDate.of(2026, 10, 1),
        programEndDate = LocalDate.of(2027, 1, 1),
        tuitionType = TuitionType.PAID,
        tuitionAmount = 1_000_000,
        representativeImageUrl = "https://example.com/images/updated.png",
        shortDescription = "데이터 분석가로 성장하는 12주",
        content = "변경된 부트캠프 상세 내용",
        applicationMethod = ApplicationMethod.EMAIL,
        partners = partners,
        curriculums = curriculums,
    )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_USER_ID = 23L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
    }
}
