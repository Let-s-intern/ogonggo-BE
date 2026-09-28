package com.ogonggo.core.work24.implement

import com.ogonggo.core.work24.domain.Work24Service
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 고용24 Open API 설정이다.
 *
 * 인증키는 사용 신청한 서비스마다 따로 발급된다. 키 이름을 모두 `-auth-key`로 끝내는 이유는
 * 배포 로그 마스킹 스크립트가 키 이름에 `key`가 들어간 줄만 가리기 때문이다.
 *
 * 인증키의 기본값을 빈 문자열로 둬서 키가 없어도 애플리케이션은 기동한다.
 * 아직 승인되지 않은 서비스 하나 때문에 API 전체가 뜨지 못하면 안 되므로, 호출 시점에 실패로 처리한다.
 */
@ConfigurationProperties(prefix = "ogonggo.work24")
data class Work24Properties(
    val baseUrl: String = "https://www.work24.go.kr/cm/openApi/call",
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(10),
    val recruitmentAuthKey: String = "",
    val tomorrowLearningCardAuthKey: String = "",
    val workStudyAuthKey: String = "",
    val governmentJobAuthKey: String = "",
    val jobSeekerProgramAuthKey: String = "",
    val occupationAuthKey: String = "",
    val dutyAuthKey: String = "",
    val smallGiantCompanyAuthKey: String = "",
    /**
     * 고용24에서 받은 훈련과정을 부트캠프로 등록할 때 쓰는 대표 이미지다. 고용24는 과정 이미지를 주지 않는다.
     * 비어 있으면 훈련과정 수집을 건너뛴다. 부트캠프는 대표 이미지가 필수다.
     */
    val bootcampImageUrl: String = "",
) {
    fun authKey(service: Work24Service): String = when (service) {
        Work24Service.RECRUITMENT -> recruitmentAuthKey
        Work24Service.TOMORROW_LEARNING_CARD -> tomorrowLearningCardAuthKey
        Work24Service.WORK_STUDY -> workStudyAuthKey
        Work24Service.GOVERNMENT_JOB -> governmentJobAuthKey
        Work24Service.JOB_SEEKER_PROGRAM -> jobSeekerProgramAuthKey
        Work24Service.OCCUPATION -> occupationAuthKey
        Work24Service.DUTY -> dutyAuthKey
        Work24Service.SMALL_GIANT_COMPANY -> smallGiantCompanyAuthKey
    }

    /** 설정 객체가 로그에 찍혀도 인증키가 드러나지 않게 한다. */
    override fun toString(): String = "Work24Properties(baseUrl=$baseUrl, bootcampImageUrl=$bootcampImageUrl)"
}
