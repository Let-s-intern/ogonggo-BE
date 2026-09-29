package com.ogonggo.userapi.enumeration.business

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentApplicationSortType
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.toEnumOptions
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.business.AdvertisementPromotionChannel
import org.springframework.stereotype.Service

/**
 * 사용자 API의 요청·응답에 나오는 업무 enum의 선택지를 enum 이름별로 제공한다.
 *
 * 어떤 enum을 내보낼지는 사용자 API의 계약이 정하므로 목록을 여기서 관리한다.
 * 사용자 API 요청·응답에 새 업무 enum을 쓰면 이 목록에도 추가한다.
 * 값은 enum의 전체 값이며, 목록 필터처럼 일부 값만 받는 곳의 범위는 각 API 명세를 따른다.
 */
@Service
class UserEnumService {

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
            options<JobApplicationStatus>(),
            options<JobPublicationStatus>(),
            options<JobSortType>(),
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
            options<BootcampApplicationStatus>(),
            options<BootcampPublicationStatus>(),
            options<BootcampSortType>(),
            // 기업회원 등록 콘텐츠의 검수
            options<ReviewStatus>(),
            // 북마크
            options<BookmarkSortType>(),
            // 커뮤니티 모집글
            options<RecruitmentType>(),
            options<RecruitmentPosition>(),
            options<ProgressMethod>(),
            options<ContactMethod>(),
            options<RecruitmentStatus>(),
            options<RecruitmentPostSortType>(),
            options<RecruitmentPostManagementStatus>(),
            options<RecruitmentPostApplicationStatus>(),
            options<RecruitmentPostManagementSortType>(),
            options<RecruitmentApplicationProgressStatus>(),
            options<RecruitmentApplicationSortType>(),
            // 회원
            options<UserRole>(),
            options<UserStatus>(),
            options<UserGrade>(),
            options<LetsCareerAuthProvider>(),
            // 광고 문의
            options<AdvertisementInquiryType>(),
            options<AdvertisementPromotionChannel>(),
        ).also { entries ->
            val duplicated = entries.groupBy { it.first }.filterValues { it.size > 1 }.keys
            check(duplicated.isEmpty()) { "enum 이름이 겹칩니다: $duplicated" }
        }.toMap()

        inline fun <reified E> options(): Pair<String, List<EnumOption>> where E : Enum<E>, E : EnumField =
            E::class.java.simpleName to enumValues<E>().asIterable().toEnumOptions()
    }
}
