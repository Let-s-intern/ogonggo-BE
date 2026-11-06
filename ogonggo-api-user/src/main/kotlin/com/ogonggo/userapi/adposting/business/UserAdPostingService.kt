package com.ogonggo.userapi.adposting.business

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.adposting.implement.AdPostingReader
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampMetricDto
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.userapi.adposting.implement.UserAdPostingCache
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import com.ogonggo.userapi.job.business.UserJobSummary
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserAdPostingService(
    private val adPostingReader: AdPostingReader,
    private val adPostingTargetReader: AdPostingTargetReader,
    private val jobMetricReader: JobMetricReader,
    private val bootcampMetricReader: BootcampMetricReader,
    private val userAdPostingCache: UserAdPostingCache,
    private val clock: Clock,
) {

    /**
     * 자리의 노출 중인 광고 공고 가운데 대상이 공개이고 모집 중인 것만 자리 안 순서대로 준다.
     * 로그인과 무관한 결과라 자리마다 캐시하고, 대상 요약의 `bookmarked`는 늘 false다(웹이 브라우저의 북마크로 덮는다).
     * 조회 수·북마크 수는 캐시 시간만큼 늦을 수 있다.
     */
    fun getAdPostings(placement: AdPostingPlacement): List<UserAdPostingResult> =
        userAdPostingCache.get(placement) { load(placement) }

    private fun load(placement: AdPostingPlacement): List<UserAdPostingResult> {
        val now = LocalDateTime.now(clock)
        val live = adPostingReader.readAllByPlacement(placement).filter { it.isLive(now) }
        val targets = adPostingTargetReader.read(live, now)
        val shown = live.mapNotNull { adPosting -> targets[adPosting.id]?.takeIf { it.available }?.let { adPosting to it } }
        return when (placement.targetType) {
            AdPostingTargetType.JOB -> {
                val metrics = jobMetricReader.readAll(shown.mapNotNull { (_, target) -> target.job?.id })
                shown.map { (adPosting, target) ->
                    val job = checkNotNull(target.job)
                    UserAdPostingResult(
                        id = checkNotNull(adPosting.id),
                        placement = placement,
                        targetType = placement.targetType,
                        job = UserJobSummary.from(job, bookmarked = false, metric = metrics[job.id] ?: JobMetricDto.EMPTY),
                    )
                }
            }
            AdPostingTargetType.BOOTCAMP -> {
                val metrics = bootcampMetricReader.readAll(shown.mapNotNull { (_, target) -> target.bootcamp?.id })
                shown.map { (adPosting, target) ->
                    val bootcamp = checkNotNull(target.bootcamp)
                    UserAdPostingResult(
                        id = checkNotNull(adPosting.id),
                        placement = placement,
                        targetType = placement.targetType,
                        bootcamp = UserBootcampSummary.from(
                            bootcamp,
                            bookmarked = false,
                            metric = metrics[bootcamp.id] ?: BootcampMetricDto.EMPTY,
                        ),
                    )
                }
            }
        }
    }
}
