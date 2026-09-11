package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 이미지가 저장될 S3와 클라이언트에 반환할 표시용 URL 설정이다.
 *
 * bucket이 비어 있으면 애플리케이션은 기동할 수 있지만 업로드 시 실패한다.
 * 이미지 저장소 설정 하나 때문에 사용자 API 전체가 기동하지 못하는 것보다,
 * 실제 업로드 요청에서 명확하게 실패시키는 편이 안전하다.
 */
@ConfigurationProperties(prefix = "ogonggo.storage.s3")
data class S3ImageStorageProperties(
    val bucket: String = "",
    val region: String = "ap-northeast-2",
    val publicBaseUrl: String = "",
)
