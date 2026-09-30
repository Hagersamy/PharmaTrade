package com.pharmatrade.feature.auth.domain.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.model.Zone

interface ZoneRepository {
    suspend fun getZones(): Result<List<Zone>>
}
