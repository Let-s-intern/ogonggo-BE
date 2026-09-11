package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.userapi.error.InvalidRequestParameterException
import com.ogonggo.userapi.job.business.UserJobService
import com.ogonggo.userapi.job.presentation.response.UserJobCalendarItemResponse
import com.ogonggo.userapi.job.presentation.response.UserJobDetailResponse
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
class UserJobController(
    private val userJobService: UserJobService,
) : UserJobApi {

    @GetMapping
    override fun getJobs(
        @AuthenticationPrincipal userId: Long?,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "sort", defaultValue = "LATEST") sortType: JobSortType,
        @RequestParam(name = "employmentType", required = false) employmentType: EmploymentType?,
        @RequestParam(name = "experienceType", required = false) experienceType: ExperienceType?,
        @RequestParam(name = "keyword", required = false) keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>> {
        val result = userJobService.getJobs(
            userId = userId,
            condition = JobSearchCondition(
                employmentType = employmentType,
                experienceType = experienceType,
                keyword = keyword,
            ),
            sortType = sortType,
            page = page - 1,
            size = size,
        )
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

    @GetMapping("/popular")
    override fun getPopularJobs(
        @AuthenticationPrincipal userId: Long?,
    ): ResponseEntity<SuccessResponse<List<UserJobSummaryResponse>>> =
        SuccessResponse.ok(userJobService.getPopularJobs(userId).map(UserJobSummaryResponse::from))

    @GetMapping("/similar")
    override fun getSimilarJobs(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SuccessResponse<List<UserJobSummaryResponse>>> =
        SuccessResponse.ok(userJobService.getSimilarJobs(userId).map(UserJobSummaryResponse::from))

    @GetMapping("/{jobId}")
    override fun getJob(
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<UserJobDetailResponse>> =
        SuccessResponse.ok(UserJobDetailResponse.from(userJobService.getJob(userId, jobId)))

    @PostMapping("/{jobId}/source-url-clicks")
    override fun recordSourceUrlClick(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userJobService.recordSourceUrlClick(userId, jobId)
        return SuccessResponse.ok()
    }

    @GetMapping("/calendar")
    override fun getJobCalendar(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
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
