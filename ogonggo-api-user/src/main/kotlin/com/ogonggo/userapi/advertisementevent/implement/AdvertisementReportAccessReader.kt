package com.ogonggo.userapi.advertisementevent.implement

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Component

/** Loads the access facts used by the business service to authorize a report. */
@Component
class AdvertisementReportAccessReader(
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementExposureReader: AdvertisementExposureReader,
    private val userReader: UserReader,
) {

    fun read(userId: Long, contractId: Long): AdvertisementReportAccess =
        AdvertisementReportAccess(
            contractOwnerId = advertisementContractReader.read(contractId).userId,
            userRole = userReader.read(userId).role,
            exposureIds = advertisementExposureReader.readIdsByContractId(contractId),
        )
}

data class AdvertisementReportAccess(
    val contractOwnerId: Long?,
    val userRole: UserRole,
    val exposureIds: List<Long>,
)
