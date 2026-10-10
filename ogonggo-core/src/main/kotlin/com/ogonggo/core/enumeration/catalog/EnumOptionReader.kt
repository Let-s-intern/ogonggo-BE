package com.ogonggo.core.enumeration.catalog

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampCategory
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernPopularSortType
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.enumOptionMapOf
import com.ogonggo.core.enumeration.enumOptionsOf
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicantPresence
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewTargetType
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import org.springframework.stereotype.Component

/**
 * 사용자 API와 관리자 API가 함께 내보내는 업무 enum 선택지다. 두 API의 enum 목록을 한 곳에서 관리한다.
 *
 * 요청·응답에 새 업무 enum을 쓰면 여기에 추가한다. core가 볼 수 없는 API 전용 enum은 각 API의 EnumService가 덧붙인다.
 * 값은 enum의 전체 값이며 선언 순서를 따른다. 목록 필터처럼 일부 값만 받는 곳의 범위는 각 API 명세를 따른다.
 */
@Component
class EnumOptionReader {

    fun readAll(): Map<String, List<EnumOption>> = ENUMS

    private companion object {
        val ENUMS: Map<String, List<EnumOption>> = enumOptionMapOf(
            // 채용공고
            enumOptionsOf<JobEmploymentType>(),
            enumOptionsOf<JobExperienceType>(),
            enumOptionsOf<JobEducationLevel>(),
            enumOptionsOf<JobRecruitmentType>(),
            enumOptionsOf<JobRecruitmentStatus>(),
            enumOptionsOf<JobApplicationMethod>(),
            enumOptionsOf<JobApplicationStatus>(),
            enumOptionsOf<JobPublicationStatus>(),
            enumOptionsOf<JobSortType>(),
            enumOptionsOf<JobContentField>(),
            // 알림
            enumOptionsOf<NotificationChannel>(),
            enumOptionsOf<NotificationStatus>(),
            // 직군·직무. JobRole의 parent는 속한 JobField다.
            enumOptionsOf<JobField>(),
            enumOptionsOf<JobRole>(),
            // 근무 지역. SubRegion의 parent는 속한 Region이다.
            enumOptionsOf<Region>(),
            enumOptionsOf<SubRegion>(),
            // 부트캠프
            enumOptionsOf<BootcampRecruitmentStatus>(),
            enumOptionsOf<BootcampRecruitmentType>(),
            enumOptionsOf<BootcampOperationType>(),
            enumOptionsOf<BootcampTuitionType>(),
            enumOptionsOf<BootcampApplicationMethod>(),
            enumOptionsOf<BootcampApplicationStatus>(),
            enumOptionsOf<BootcampPublicationStatus>(),
            enumOptionsOf<BootcampSortType>(),
            enumOptionsOf<BootcampCategory>(),
            enumOptionsOf<BootcampContentField>(),
            // 기업회원 등록 콘텐츠의 검수
            enumOptionsOf<ContentReviewTargetType>(),
            enumOptionsOf<ContentReviewStatus>(),
            enumOptionsOf<ContentSource>(),
            // 북마크
            enumOptionsOf<BookmarkSortType>(),
            // 사이드 프로젝트·스터디 모집글
            enumOptionsOf<RecruitmentPostType>(),
            enumOptionsOf<RecruitmentPostPosition>(),
            enumOptionsOf<RecruitmentPostProgressMethod>(),
            enumOptionsOf<RecruitmentPostContactMethod>(),
            enumOptionsOf<RecruitmentPostRecruitmentStatus>(),
            enumOptionsOf<RecruitmentPostPublicationStatus>(),
            enumOptionsOf<RecruitmentPostSortType>(),
            enumOptionsOf<RecruitmentPostManagementStatus>(),
            enumOptionsOf<RecruitmentPostApplicantPresence>(),
            enumOptionsOf<RecruitmentPostManagementSortType>(),
            enumOptionsOf<RecruitmentPostApplicationProgressStatus>(),
            enumOptionsOf<RecruitmentPostApplicationSortType>(),
            // 취준고민
            enumOptionsOf<ConcernCategory>(),
            enumOptionsOf<ConcernSortType>(),
            enumOptionsOf<ConcernPopularSortType>(),
            // 회원
            enumOptionsOf<UserRole>(),
            enumOptionsOf<UserStatus>(),
            enumOptionsOf<UserGrade>(),
            enumOptionsOf<LetsCareerAuthProvider>(),
        )
    }
}
