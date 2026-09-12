package com.ogonggo.userapi.image.implement

import com.ogonggo.core.image.implement.ImageAssetManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
class ImageAssetCleanupScheduler(
    private val imageAssetManager: ImageAssetManager,
    private val clock: Clock,
    @Value("\${ogonggo.storage.s3.cleanup.retention-hours:24}")
    private val retentionHours: Long,
) {

    @Scheduled(fixedDelayString = "\${ogonggo.storage.s3.cleanup.fixed-delay-ms:3600000}")
    fun cleanup() {
        val deletedCount = imageAssetManager.cleanup(
            now = LocalDateTime.now(clock),
            retention = Duration.ofHours(retentionHours),
        )
        if (deletedCount > 0) {
            log.info("고아 이미지 정리 완료. deletedCount={}", deletedCount)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ImageAssetCleanupScheduler::class.java)
    }
}
