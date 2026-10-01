package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 이미지가 저장될 S3와 클라이언트에 반환할 표시용 URL 설정이다. [S3ImageStorageConfiguration]이 [CloudAwsProperties]로 만든다.
 *
 * bucket이 비어 있으면 애플리케이션은 기동할 수 있지만 업로드 시 실패한다.
 * 이미지 저장소 설정 하나 때문에 사용자 API 전체가 기동하지 못하는 것보다,
 * 실제 업로드 요청에서 명확하게 실패시키는 편이 안전하다.
 */
data class S3ImageStorageProperties(
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

/**
 * 2026-10-01 이전 코드가 읽던 이름이다. 운영 설정은 처음부터 [CloudAwsProperties] 이름을 써서 이 값이 비어 있었고,
 * 그래서 관리자 API가 이미지를 옮기지 못했다. 혹시 이 이름으로 넣어 둔 환경이 있을 수 있어 `cloud.aws`가 비었을 때만 쓴다.
 */
@ConfigurationProperties(prefix = "ogonggo.storage.s3")
data class LegacyS3ImageStorageProperties(
    val bucket: String = "",
    val region: String = "",
    val publicBaseUrl: String = "",
)

internal const val DEFAULT_REGION = "ap-northeast-2"
