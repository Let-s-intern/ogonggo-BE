package com.ogonggo.core.job.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.TodayJob
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.TodayJobDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.job.persistence.TodayJobJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class TodayJobManager internal constructor(
    private val todayJobRepository: TodayJobJpaRepository,
    private val jobRepository: JobJpaRepository,
    private val jobQueryRepository: JobQueryRepository,
) {

    /**
     * 오늘의 공고를 넘어온 공고와 추천 문구로 모두 바꾼다. 목록의 순서가 노출 순서이고, 빈 목록이면 오늘의 공고를 비운다.
     * 게시 상태는 가리지 않는다. 고른 뒤에도 공고가 숨겨질 수 있어 노출 여부는 조회할 때 거른다.
     * 기존 행을 빼고 새 행을 넣는 두 쓰기가 함께 반영돼야 하므로 호출하는 쪽의 트랜잭션 안에서 실행한다.
     * 바꾼 PICK의 직군에 맞춰 공개 목록 정렬 키의 PICK 직군 비트도 같은 트랜잭션에서 다시 쓴다.
     */
    fun replace(todayJobs: List<TodayJobDto.Request>, now: LocalDateTime) {
        val jobIds = todayJobs.map(TodayJobDto.Request::jobId)
        require(jobIds.distinct().size == jobIds.size) { "같은 공고를 오늘의 공고에 두 번 넣을 수 없습니다." }
        if (jobIds.isNotEmpty() && jobRepository.countByIdInAndDeletedAtIsNull(jobIds) != jobIds.size.toLong()) {
            throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)
        }
        todayJobRepository.softDeleteAllActive(now)
        todayJobRepository.saveAll(
            todayJobs.mapIndexed { index, todayJob ->
                TodayJob(
                    jobId = todayJob.jobId,
                    displayOrder = index,
                    recommendationTitle = todayJob.recommendationTitle,
                    recommendationDescription = todayJob.recommendationDescription,
                )
            },
        )
        alignListSortKeys()
    }

    /**
     * 게시 중인 PICK 공고의 직군과 같은 공고는 PICK 직군 비트를 켜고, 나머지는 끈다([com.ogonggo.core.job.domain.JobListSortKey]).
     * 비트가 이미 맞는 공고는 고치지 않으므로 직군이 그대로면 쓰기가 거의 없다.
     * 공고를 고친 뒤 직군이 바뀌거나 PICK 공고를 숨겨도 바로 맞추지 않는다. 다음에 PICK을 바꿀 때 맞춰진다.
     */
    private fun alignListSortKeys() {
        val pickFields = jobQueryRepository.findPublishedTodayJobFields()
        if (pickFields.isEmpty()) {
            jobRepository.unmarkAllPickField()
            return
        }
        jobRepository.unmarkPickFieldExcept(pickFields)
        jobRepository.markPickField(pickFields)
    }
}
