package com.ogonggo.userapi.advertisementevent.business

import com.ogonggo.core.advertisementevent.implement.AdvertisementEventCleanupManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdvertisementEventCleanupService(
    private val cleanupManager: AdvertisementEventCleanupManager,
) {

    @Transactional
    fun deleteExpired(cutoff: LocalDate, limit: Int): Int = cleanupManager.deleteExpired(cutoff, limit)
}
