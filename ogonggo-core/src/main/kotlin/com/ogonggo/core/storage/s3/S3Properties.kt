package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 파일을 저장할 S3 bucket과 클라이언트에 반환할 표시용 URL 설정이다. [S3Configuration]이 [CloudAwsProperties]로 만든다.
 *
 * bucket이 비어 있으면 애플리케이션은 기동할 수 있지만 업로드 시 실패한다.
 * 저장소 설정 하나 때문에 사용자 API 전체가 기동하지 못하는 것보다,
 * 실제 업로드 요청에서 명확하게 실패시키는 편이 안전하다.
 */
data class S3Properties(
    val bucket: String = "",
    val region: String = DEFAULT_REGION,
    val publicBaseUrl: String = "",
)

/**
 * 운영 설정 파일의 AWS 항목이다. 두 API 모두 이 이름으로 S3 설정을 둔다.
 *
 * ```yaml
 * cloud:
 *   aws:
 *     s3:
 *       bucket: 버킷 이름
 *       public-base-url: CDN 주소(선택). 비우면 S3 기본 주소를 쓴다.
 *     credentials:
 *       access-key: 접근 키(선택)
 *       secret-key: 비밀 키(선택)
 *     region:
 *       static: ap-northeast-2
 * ```
 *
 * 접근 키 둘 중 하나라도 비어 있으면 AWS 기본 방식(환경변수, ECS 태스크 역할 등)으로 인증한다.
 */
@ConfigurationProperties(prefix = "cloud.aws")
data class CloudAwsProperties(
    val s3: S3 = S3(),
    val credentials: Credentials = Credentials(),
    val region: Region = Region(),
) {
    data class S3(val bucket: String = "", val publicBaseUrl: String = "")

    data class Credentials(val accessKey: String = "", val secretKey: String = "") {
        val configured: Boolean get() = accessKey.isNotBlank() && secretKey.isNotBlank()

        /** 설정 객체가 로그에 찍혀도 키가 드러나지 않게 한다. */
        override fun toString(): String = "Credentials(configured=$configured)"
    }

    data class Region(val static: String = DEFAULT_REGION)
}

internal const val DEFAULT_REGION = "ap-northeast-2"
