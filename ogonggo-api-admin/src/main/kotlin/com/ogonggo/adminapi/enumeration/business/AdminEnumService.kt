package com.ogonggo.adminapi.enumeration.business

import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.toEnumOptions
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserStatus
import org.springframework.stereotype.Service

/**
 * 관리자 API의 요청·응답에 나오는 업무 enum의 선택지를 enum 이름별로 제공한다.
 *
 * 어떤 enum을 내보낼지는 관리자 API의 계약이 정하므로 목록을 여기서 관리한다. 사용자 API의 목록과 겹쳐도 합치지 않는다.
 * 관리자 API 요청·응답에 새 업무 enum을 쓰면 이 목록에도 추가한다.
 * 값은 enum의 전체 값이며, 목록 필터처럼 일부 값만 받는 곳의 범위는 각 API 명세를 따른다.
 */
@Service
class AdminEnumService {

    fun getEnums(): Map<String, List<EnumOption>> = ENUMS

    private companion object {
        val ENUMS: Map<String, List<EnumOption>> = listOf(
            // 채용공고
            options<EmploymentType>(),
            options<ExperienceType>(),
            options<EducationLevel>(),
            options<JobRecruitmentType>(),
            options<JobRecruitmentStatus>(),
            options<JobApplicationMethod>(),
            options<JobContentField>(),
            // 직군·직무. JobRole의 parent는 속한 JobField다.
            options<JobField>(),
            options<JobRole>(),
            // 근무 지역. SubRegion의 parent는 속한 Region이다.
            options<Region>(),
            options<SubRegion>(),
            // 부트캠프
            options<BootcampStatus>(),
            options<BootcampRecruitmentType>(),
            options<OperationType>(),
            options<TuitionType>(),
            options<ApplicationMethod>(),
            options<BootcampContentField>(),
            // 채용공고·부트캠프 공통 관리
            options<AdminContentSortType>(),
            options<AdminContentVisibility>(),
            // 기업회원 등록 콘텐츠의 검수
            options<ReviewContentType>(),
            options<ReviewStatus>(),
            options<ContentSource>(),
            // 모집글
            options<RecruitmentPostType>(),
            options<RecruitmentPostPosition>(),
            options<RecruitmentPostProgressMethod>(),
            options<RecruitmentPostRecruitmentStatus>(),
            // 회원
            options<UserStatus>(),
            options<UserGrade>(),
        ).also { entries ->
            val duplicated = entries.groupBy { it.first }.filterValues { it.size > 1 }.keys
            check(duplicated.isEmpty()) { "enum 이름이 겹칩니다: $duplicated" }
        }.toMap()

        inline fun <reified E> options(): Pair<String, List<EnumOption>> where E : Enum<E>, E : EnumField =
            E::class.java.simpleName to enumValues<E>().asIterable().toEnumOptions()
    }
}
