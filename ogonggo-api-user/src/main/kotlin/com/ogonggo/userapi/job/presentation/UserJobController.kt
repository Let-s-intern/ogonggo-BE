package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.userapi.error.InvalidRequestParameterException
import com.ogonggo.userapi.job.business.UserJobService
import com.ogonggo.userapi.job.presentation.response.UserJobCalendarItemResponse
import com.ogonggo.userapi.job.presentation.response.UserJobDetailResponse
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Validated
@RestController
class UserJobController(
    private val userJobService: UserJobService,
) : UserJobApi {

    override fun getJobs(
        userId: Long,
        page: Int,
        size: Int,
        sortType: JobSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>> {
        val result = userJobService.getJobs(userId, page - 1, size, sortType)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(UserJobSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    override fun getJob(
        userId: Long,
        jobId: Long,
    ): ResponseEntity<SuccessResponse<UserJobDetailResponse>> =
        SuccessResponse.ok(UserJobDetailResponse.from(userJobService.getJob(userId, jobId)))

    override fun recordSourceUrlClick(
        userId: Long,
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobService.recordSourceUrlClick(userId, jobId)
        return SuccessResponse.ok()
    }

    override fun getJobCalendar(
        from: LocalDate,
        to: LocalDate,
    ): ResponseEntity<SuccessResponse<List<UserJobCalendarItemResponse>>> {
        validateCalendarRange(from, to)
        return SuccessResponse.ok(
            userJobService.getJobCalendar(from, to).map(UserJobCalendarItemResponse::from),
        )
    }

    /**
     * 달력 응답은 페이지네이션이 없어 조회 기간이 곧 응답 크기가 된다.
     * 두 파라미터의 관계와 기간 길이는 단일 필드 제약으로 선언할 수 없어 여기서 검증한다.
     */
    private fun validateCalendarRange(from: LocalDate, to: LocalDate) {
        if (from.isAfter(to)) {
            throw InvalidRequestParameterException("from", "조회 시작일은 종료일보다 늦을 수 없습니다.")
        }

        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_CALENDAR_RANGE_DAYS) {
            throw InvalidRequestParameterException(
                "to",
                "조회 기간은 최대 ${MAX_CALENDAR_RANGE_DAYS}일까지 가능합니다.",
            )
        }
    }

    companion object {
        private const val MAX_CALENDAR_RANGE_DAYS = 92L
    }
}
