package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CollectDto
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.review.domain.ContentSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 고용24 목록을 끝 페이지까지 넘기며 새 항목을 채용공고·부트캠프로 등록한다.
 *
 * 항목마다 고용24 식별값(구인인증번호, 과정 ID+회차)으로 이미 등록됐는지 보고, 있으면 내용이 바뀌었어도 건너뛴다.
 * 운영자가 지운 콘텐츠도 등록한 것으로 봐서 다시 넣지 않는다. 새 항목만 상세 API를 불러 본문을 채운다.
 * 훈련과정(부트캠프, 일학습병행 채용공고)은 고용24 과정 상세 화면도 읽어 Open API에 없는 칸을 채운다. 화면을 못 읽어도 Open API 값만으로 등록한다.
 * 부트캠프는 훈련기관 소개 화면의 로고와 사진도 오공고 이미지 저장소로 옮겨 넣는다.
 * 한 항목이 실패해도 다음 항목으로 넘어가지만, 연달아 [MAX_CONSECUTIVE_FAILURES]번 실패하면 고용24 장애로 보고 이 대상을 멈춘다.
 *
 * 페이지를 넘기다 다음 중 하나면 멈춘다.
 * - 응답의 전체 건수만큼 받았거나, 전체 건수가 없고 한 페이지를 다 채우지 못했다.
 * - 항목이 없거나 이번 수집에서 이미 받은 항목만 왔다. 페이지 파라미터를 무시하는 API가 같은 페이지를 되풀이하는 경우다.
 * - 고용24 페이지 상한(1,000쪽)에 닿았다.
 *
 * 등록은 항목마다 반영되므로 중간에 멈춰도 그 전까지 등록한 항목은 남는다.
 * 저장기가 각자 트랜잭션을 쓰므로 호출자가 트랜잭션을 열어 둔 채로 부르면 안 된다. 수집이 끝날 때까지 커넥션을 잡게 된다.
 */
@Component
class Work24Collector(
    private val work24Client: Work24Client,
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val bootcampReader: BootcampReader,
    private val bootcampAppender: BootcampAppender,
    private val work24CoursePageReader: Work24CoursePageReader,
    private val work24InstitutionImageImporter: Work24InstitutionImageImporter,
) {

    /** 인증키가 설정되어 수집할 수 있는 대상인지 알려 준다. */
    fun isReady(target: Work24CollectionTarget): Boolean = work24Client.isConfigured(target.api.service)

    fun collect(target: Work24CollectionTarget, now: LocalDateTime): Work24CollectDto {
        val baseParameters = target.parameters(now.toLocalDate())
        val collectedIds = mutableSetOf<String>()
        val counter = Counter()
        var receivedCount = 0

        for (page in 1..MAX_PAGE) {
            val response = work24Client.fetch(
                target.api,
                baseParameters + mapOf(
                    target.paging.pageParameter to page.toString(),
                    target.paging.sizeParameter to PAGE_SIZE.toString(),
                ),
            )
            counter.pages++
            val nodes = items(response, target.itemPath)
            receivedCount += nodes.size
            val newNodes = nodes.filter { node -> id(target, node)?.let(collectedIds::add) ?: true }
            if (newNodes.isEmpty()) {
                break
            }

            newNodes.forEach { node -> counter.record(import(target, node, now)) }

            // 식별값이 없는 항목도 받은 것이므로 전체 건수와는 받은 항목 수로 비교한다.
            val total = response.path(target.paging.totalField).asText().toIntOrNull()
            val finished = if (total != null) receivedCount >= total else nodes.size < PAGE_SIZE
            if (finished) {
                break
            }
        }

        return Work24CollectDto(
            target = target,
            pageCount = counter.pages,
            appendedCount = counter.appended,
            skippedCount = counter.skipped,
            excludedCount = counter.excluded,
            failedCount = counter.failed,
        )
    }

    private fun import(target: Work24CollectionTarget, item: JsonNode, now: LocalDateTime): Outcome =
        try {
            if (target.excludes(item)) {
                Outcome.EXCLUDED
            } else {
                when (target.destination) {
                    Work24Destination.JOB -> importJob(target, item)
                    Work24Destination.BOOTCAMP -> importBootcamp(target, item, now)
                    Work24Destination.WORK_STUDY_JOB -> importWorkStudyJob(target, item, now)
                }
            }
        } catch (exception: Exception) {
            log.warn("고용24 항목을 등록하지 못해 건너뜁니다. target={}, id={}", target, id(target, item), exception)
            Outcome.FAILED
        }

    /**
     * 구인인증번호로 이미 등록했으면 건너뛴다. 운영자가 지운 공고도 등록한 것으로 본다.
     * 크롤러가 같은 원문을 먼저 등록했어도 건너뛰어 같은 공고가 두 번 보이지 않게 한다.
     */
    private fun importJob(target: Work24CollectionTarget, item: JsonNode): Outcome {
        val externalId = requireNotNull(id(target, item)) { "구인인증번호가 없습니다." }
        val sourceUrl = requireNotNull(Work24JobMapper.sourceUrl(item)) { "채용정보 URL이 없습니다." }
        if (jobReader.existsByExternalId(ContentSource.WORK24, externalId) || jobReader.existsBySourceUrl(sourceUrl)) {
            return Outcome.SKIPPED
        }

        val detail = work24Client.fetch(target.detailApi, Work24JobMapper.detailParameters(item))
        jobAppender.append(Work24JobMapper.toAppendDto(item, detail, sourceUrl, externalId))
        return Outcome.APPENDED
    }

    /** 일학습병행 훈련과정을 채용공고로 넣는다. 과정 ID와 회차로 이미 등록했으면 건너뛴다. */
    private fun importWorkStudyJob(target: Work24CollectionTarget, item: JsonNode, now: LocalDateTime): Outcome {
        val externalId = requireNotNull(id(target, item)) { "훈련과정 ID나 회차가 없습니다." }
        val sourceUrl = requireNotNull(Work24WorkStudyJobMapper.sourceUrl(item)) { "훈련과정 링크가 없습니다." }
        if (jobReader.existsByExternalId(ContentSource.WORK24, externalId) || jobReader.existsBySourceUrl(sourceUrl)) {
            return Outcome.SKIPPED
        }

        val detail = work24Client.fetch(target.detailApi, Work24WorkStudyJobMapper.detailParameters(item))
        val page = work24CoursePageReader.readWorkStudy(sourceUrl)
        jobAppender.append(Work24WorkStudyJobMapper.toAppendDto(item, detail, page, sourceUrl, externalId, now))
        return Outcome.APPENDED
    }

    /** 과정 ID와 회차로 이미 등록했으면 건너뛴다. 공고와 같은 규칙이다. */
    private fun importBootcamp(target: Work24CollectionTarget, item: JsonNode, now: LocalDateTime): Outcome {
        val externalId = requireNotNull(id(target, item)) { "훈련과정 ID나 회차가 없습니다." }
        val sourceUrl = requireNotNull(Work24BootcampMapper.sourceUrl(item)) { "훈련과정 링크가 없습니다." }
        if (bootcampReader.existsByExternalId(ContentSource.WORK24, externalId) ||
            bootcampReader.existsBySourceUrl(sourceUrl)
        ) {
            return Outcome.SKIPPED
        }

        val detail = work24Client.fetch(target.detailApi, Work24BootcampMapper.detailParameters(item))
        bootcampAppender.append(
            Work24BootcampMapper.toAppendDto(
                target = target,
                item = item,
                detail = detail,
                page = work24CoursePageReader.read(sourceUrl),
                images = work24InstitutionImageImporter.import(
                    institutionId = Work24BootcampMapper.institutionId(item),
                    institutionUrl = Work24BootcampMapper.institutionUrl(item),
                ),
                sourceUrl = sourceUrl,
                externalId = externalId,
                now = now,
            ),
        )
        return Outcome.APPENDED
    }

    private enum class Outcome { APPENDED, SKIPPED, EXCLUDED, FAILED }

    private class Counter {
        var pages = 0
        var appended = 0
        var skipped = 0
        var excluded = 0
        var failed = 0
        private var consecutiveFailures = 0

        fun record(outcome: Outcome) {
            when (outcome) {
                Outcome.APPENDED -> appended++
                Outcome.SKIPPED -> skipped++
                Outcome.EXCLUDED -> excluded++
                Outcome.FAILED -> failed++
            }
            consecutiveFailures = if (outcome == Outcome.FAILED) consecutiveFailures + 1 else 0
            check(consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                "고용24 항목 등록이 ${MAX_CONSECUTIVE_FAILURES}번 연달아 실패해 수집을 멈춥니다."
            }
        }
    }

    companion object {
        /** 고용24 목록 API가 허용하는 최대값이다. */
        private const val PAGE_SIZE = 100
        private const val MAX_PAGE = 1000
        private const val MAX_CONSECUTIVE_FAILURES = 10
        private const val ID_SEPARATOR = "-"
        private val log = LoggerFactory.getLogger(Work24Collector::class.java)

        /** XML을 옮긴 JSON은 항목이 하나면 객체, 여럿이면 배열이다. 항목이 없으면 요소가 없거나 빈 문자열이다. */
        internal fun items(response: JsonNode, itemPath: List<String>): List<JsonNode> {
            val node = itemPath.fold(response) { current, name -> current.path(name) }
            return when {
                node.isArray -> node.toList()
                node.isObject -> listOf(node)
                else -> emptyList()
            }
        }

        private fun id(target: Work24CollectionTarget, item: JsonNode): String? =
            target.idFields.map { item.path(it).asText().trim() }
                .takeIf { values -> values.all { it.isNotEmpty() } }
                ?.joinToString(ID_SEPARATOR)
    }
}
