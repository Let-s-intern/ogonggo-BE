package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.block
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.date
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.limit
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.number
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.section
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Values.text
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CoursePageDto
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24InstitutionImagesDto
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampImageDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.contentreview.domain.ContentSource
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 고용24 국민내일배움카드 훈련과정 목록 항목(`scn_list`)과 과정·기관정보(`inst_base_info`, `inst_detail_info`)를 부트캠프로 옮긴다.
 *
 * 등록 경로를 고용24로, 과정 ID와 회차(`trprId-trprDegr`)를 외부 식별값으로 두고 모집 중으로 곧바로 게시한다.
 * 고용24에 없는 값은 다음처럼 채운다.
 * - 로고·사진: Open API는 과정 이미지를 주지 않고 훈련기관 로고(`filePath`)는 다운로드 주소라 이미지로 열리지 않는다.
 *   훈련기관 소개 화면의 로고와 사진을 오공고 저장소로 옮긴 주소([Work24InstitutionImagesDto])를 넣는다.
 *   사진은 상세에서만 보여 주는 사진 칸에 넣는다.
 * - 대표 이미지: 비운다. 훈련기관 사진에는 사람이 찍힌 행사 사진이나 로고가 섞여 있어 목록에 자동으로 걸지 않는다.
 *   비어 있으면 클라이언트가 기본 이미지를 그린다.
 * - 모집 기간: 목록에 없어 수집한 시각부터 개강일 끝까지로 둔다.
 * - 수강료: 교육생이 내는 본인부담액(`tgcrGnrlTrneOwepAllt`)을 쓴다. 목록의 수강비(`courseMan`)는 정부 지원금을 포함한 총 훈련비다.
 * - 파트너사: 과정명 앞 괄호의 기업 이름이 [Work24PartnerCompanies] 표에 있으면 그 기업 하나를 넣는다. 고용24에는 연계 기업 항목이 없다.
 *   이런 과정은 기업 연계 과정으로 표시해 공개 목록에서 앞에 오게 한다.
 * - 진행 방식: 훈련방법 코드(`traingMthCd`)로 정한다. M1005 인터넷은 온라인, M1010 혼합·M1014 스마트혼합은 온·오프라인, 그 밖은 오프라인이다.
 *
 * Open API에 없는 값은 과정 상세 화면([Work24CoursePageDto])에서 채운다. 화면을 못 읽었으면 비워 둔다.
 * - 본문: Open API 값 뒤에 훈련목표, 교과목(세부내용·시간), 훈련교재를 덧붙인다.
 * - 지원 자격: 개요 표의 훈련대상 요건. 교육 특징: 훈련과정의 장점. 강사 정보: 훈련강사의 전공과 자격.
 * - 커리큘럼: 시간표에서 교과목마다 수업이 있는 주를 찾아 이어진 주끼리 묶는다. 1주차는 첫 수업이 있는 주(월~일)다.
 */
internal object Work24BootcampMapper {

    fun sourceUrl(item: JsonNode): String? = item.text("titleLink")

    fun institutionId(item: JsonNode): String? = item.text("trainstCstId")

    /** 훈련기관 소개 화면 주소다. */
    fun institutionUrl(item: JsonNode): String? = item.text("subTitleLink")

    fun detailParameters(item: JsonNode): Map<String, String> = mapOf(
        "srchTrprId" to requireNotNull(item.text("trprId")) { "훈련과정 ID가 없습니다." },
        "srchTrprDegr" to requireNotNull(item.text("trprDegr")) { "훈련과정 회차가 없습니다." },
        "srchTorgId" to requireNotNull(item.text("trainstCstId")) { "훈련기관 ID가 없습니다." },
    )


    fun toAppendDto(
        target: Work24CollectionTarget,
        item: JsonNode,
        detail: JsonNode,
        page: Work24CoursePageDto?,
        images: Work24InstitutionImagesDto?,
        sourceUrl: String,
        externalId: String,
        now: LocalDateTime,
    ): BootcampAppendDto {
        val base = detail.path("inst_base_info")
        val detailInfo = detail.path("inst_detail_info")

        val title = requireNotNull(base.text("trprNm") ?: item.text("title")) { "훈련과정명이 없습니다." }
        val programStartDate = requireNotNull(date(item.text("traStartDate"))) { "훈련 시작일이 없습니다." }
        val programEndDate = requireNotNull(date(item.text("traEndDate"))) { "훈련 종료일이 없습니다." }
        val recruitmentEndAt = programStartDate.atTime(LocalTime.of(23, 59, 59))
        val hasPeriod = !now.isAfter(recruitmentEndAt)
        val totalHours = base.text("trtm") ?: detailInfo.text("totTraingTime")
        val partner = Work24PartnerCompanies.of(title)

        return BootcampAppendDto(
            companyName = requireNotNull(base.text("inoNm") ?: item.text("subTitle")) { "훈련기관명이 없습니다." }
                .limit(COMPANY_NAME_MAX),
            title = title.limit(TITLE_MAX),
            programType = (item.text("trainTarget") ?: target.api.service.desc).limit(PROGRAM_TYPE_MAX),
            operationType = operationType(base.text("traingMthCd")),
            recruitmentType = if (hasPeriod) BootcampRecruitmentType.PERIOD else BootcampRecruitmentType.ALWAYS_OPEN,
            recruitmentStartAt = if (hasPeriod) now else null,
            recruitmentEndAt = if (hasPeriod) recruitmentEndAt else null,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            // 정원을 0으로 주는 과정은 정원 없음으로 본다.
            capacity = number(item.text("yardMan"))?.takeIf { it in 1..Int.MAX_VALUE }?.toInt(),
            tuitionType = BootcampTuitionType.GOVERNMENT_FUNDED,
            tuitionAmount = number(detailInfo.text("tgcrGnrlTrneOwepAllt")),
            representativeImageUrl = null,
            shortDescription = listOfNotNull(base.text("ncsNm"), totalHours?.let { "총 ${it}시간" })
                .joinToString(" · ")
                .ifBlank { title }
                .limit(SHORT_DESCRIPTION_MAX),
            content = listOfNotNull(
                section(
                    "훈련기관" to (base.text("inoNm") ?: item.text("subTitle")),
                    "주소" to listOfNotNull(base.text("addr1"), base.text("addr2")).joinToString(" ").ifBlank { null },
                    "훈련 기간" to "$programStartDate ~ $programEndDate",
                    "총 훈련일수" to (base.text("trDcnt") ?: detailInfo.text("totTraingDyct"))?.let { "${it}일" },
                    "총 훈련시간" to totalHours?.let { "${it}시간" },
                    "훈련 분야" to (base.text("ncsNm") ?: detailInfo.text("govBusiNm")),
                    "훈련 대상" to item.text("trainTarget"),
                    "본인 부담액" to detailInfo.text("tgcrGnrlTrneOwepAllt")?.let { "${it}원" },
                    "총 훈련비" to (base.text("instPerTrco") ?: item.text("realMan"))?.let { "${it}원" },
                    "만족도" to item.text("stdgScor"),
                    "3개월 취업률" to item.text("eiEmplRate3")?.let { "$it%" },
                    "담당자" to base.text("trprChap"),
                    "담당자 전화" to (base.text("trprChapTel") ?: item.text("telNo")),
                ),
                block("훈련목표", page?.overview?.entries?.firstOrNull { GOAL in it.key }?.value),
                block("교과목", page?.let(::subjects)),
                block("훈련교재", page?.textbooks?.joinToString("\n") { "- $it" }),
            ).joinToString("\n\n"),
            eligibilityAndSelectionProcess = page?.let(::eligibility),
            logoUrl = images?.logoUrl,
            instructorInfo = page?.instructors?.joinToString("\n", transform = ::instructor)?.ifBlank { null },
            programFeatures = page?.overview?.entries?.firstOrNull { it.key.isStrength() }?.value,
            applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
            applicationUrl = sourceUrl,
            managerEmail = base.text("trprChapEmail")?.limit(EMAIL_MAX),
            inquiryUrl = base.text("hpAddr"),
            sourceUrl = sourceUrl,
            status = BootcampRecruitmentStatus.RECRUITING,
            publicationStatus = BootcampPublicationStatus.PUBLISHED,
            source = ContentSource.WORK24,
            externalId = externalId,
            partners = listOfNotNull(partner).map { BootcampPartnerDto.Request(partnerName = it) },
            enterpriseLinked = partner != null,
            curriculums = page?.lessons?.let(::curriculums).orEmpty(),
            images = images?.photos.orEmpty().mapIndexed { index, photo ->
                BootcampImageDto.Request(url = photo.url, caption = photo.caption?.limit(IMAGE_CAPTION_MAX), displayOrder = index)
            },
        )
    }

    /** 시간표가 있으면 수업 순서대로, 없으면 교과편성 화면 순서대로 적는다. 교과편성 화면은 수업 순서가 아니다. */
    private fun subjects(page: Work24CoursePageDto): String {
        val lessonOrder = page.lessons.map { it.subject.subjectName() }.distinct()
            .withIndex().associate { (index, name) -> name to index }
        return page.subjects
            .sortedBy { lessonOrder[it.name.subjectName()] ?: Int.MAX_VALUE }
            .joinToString("\n") { subject ->
                listOfNotNull(
                    "■ ${subject.name}" + subject.hours?.let { " ($it)" }.orEmpty(),
                    subject.detail?.takeIf { it != subject.name },
                ).joinToString("\n")
            }
    }

    /**
     * 개요 표의 훈련대상 요건을 모은다. 과정에 따라 `훈련대상자요건` 한 칸이거나
     * `훈련대상 요건 선수학습`·`직무경력`·`기취득자격` 세 칸이다. 같은 머리말을 쓰는 훈련과정의 장점은 교육 특징으로 간다.
     */
    private fun eligibility(page: Work24CoursePageDto): String? =
        page.overview.filterKeys { TARGET in it && !it.isStrength() }
            .map { (title, body) ->
                val label = title.removePrefix(TARGET_PREFIX).trim()
                if (label == title || label.isEmpty()) body else "$label: $body"
            }
            .joinToString("\n")
            .ifBlank { null }

    private fun instructor(instructor: Work24CoursePageDto.Instructor): String =
        listOfNotNull(
            instructor.name + instructor.major?.let { " ($it)" }.orEmpty(),
            instructor.qualifications,
        ).joinToString(": ")

    private fun curriculums(lessons: List<Work24CoursePageDto.Lesson>): List<BootcampCurriculumDto.Request> {
        val firstWeek = lessons.minOfOrNull { it.date }
            ?.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ?: return emptyList()

        return lessons
            .groupBy({ it.subject.subjectName() }) { ChronoUnit.WEEKS.between(firstWeek, it.date).toInt() + 1 }
            .flatMap { (subject, weeks) -> weekRanges(weeks).map { range -> range to subject } }
            .sortedWith(compareBy({ it.first.first }, { it.first.last }))
            .mapIndexed { index, (range, subject) ->
                BootcampCurriculumDto.Request(
                    startWeek = range.first,
                    endWeek = range.last,
                    subtitle = subject.limit(CURRICULUM_SUBTITLE_MAX),
                    displayOrder = index,
                )
            }
    }

    /** 이어진 주를 한 구간으로 묶는다. 중간에 수업이 없는 주가 있으면 구간을 나눈다. */
    private fun weekRanges(weeks: List<Int>): List<IntRange> =
        weeks.distinct().sorted().fold(mutableListOf<IntRange>()) { ranges, week ->
            val last = ranges.lastOrNull()
            if (last != null && week == last.last + 1) {
                ranges[ranges.lastIndex] = last.first..week
            } else {
                ranges += week..week
            }
            ranges
        }

    /** 시간표는 교과목 앞에 `(비NCS)`, `(전공)` 같은 교과 구분을 붙인다. 떼고 공백을 하나로 맞춰 교과편성의 이름과 같게 한다. */
    private fun String.subjectName(): String = replace(SUBJECT_KIND, "").replace(BLANK, " ").trim()

    private fun String.isStrength(): Boolean = "장점" in this || "강점" in this

    private fun operationType(trainingMethodCode: String?): BootcampOperationType = when (trainingMethodCode) {
        "M1005" -> BootcampOperationType.ONLINE
        "M1010", "M1014" -> BootcampOperationType.HYBRID
        else -> BootcampOperationType.OFFLINE
    }

    private const val COMPANY_NAME_MAX = 150
    private const val TITLE_MAX = 255
    private const val PROGRAM_TYPE_MAX = 50
    private const val SHORT_DESCRIPTION_MAX = 500
    private const val EMAIL_MAX = 320
    private const val CURRICULUM_SUBTITLE_MAX = 255
    private const val IMAGE_CAPTION_MAX = 255
    private const val GOAL = "훈련목표"
    private const val TARGET = "훈련대상"
    private const val TARGET_PREFIX = "훈련대상 요건"

    /** 실제 시간표에서 확인한 구분은 `비NCS`와 `전공`이다. `소양`은 교과편성의 NCS 소양교과에 맞춰 넣었다. */
    private val SUBJECT_KIND = Regex("""^\((비NCS|전공|소양)\)""")
    private val BLANK = Regex("""\s+""")
}
