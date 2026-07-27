package com.pharmatrade.feature.auth.domain.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.model.Zone

interface ZoneRepository {
    suspend fun getZones(): Result<List<Zone>>
}
