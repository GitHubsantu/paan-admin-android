package com.paan.safaadmin.data.repository

import com.paan.safaadmin.data.remote.AdminApi
import com.paan.safaadmin.data.remote.apiCall
import com.paan.safaadmin.data.remote.toDomain
import com.paan.safaadmin.data.remote.toDto
import com.paan.safaadmin.domain.model.Dashboard
import com.paan.safaadmin.domain.model.ShopSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepository @Inject constructor(
    private val api: AdminApi,
) {
    private val _dashboard = MutableStateFlow<Dashboard?>(null)
    val dashboard: StateFlow<Dashboard?> = _dashboard.asStateFlow()

    private val _settings = MutableStateFlow<ShopSettings?>(null)
    val settings: StateFlow<ShopSettings?> = _settings.asStateFlow()

    suspend fun refreshDashboard(): Result<Unit> =
        apiCall { api.dashboard() }.map { dto ->
            _dashboard.value = dto.toDomain()
            Unit
        }

    suspend fun refreshSettings(): Result<Unit> =
        apiCall { api.settings() }.map { dto ->
            _settings.value = dto.toDomain()
            Unit
        }

    suspend fun saveSettings(settings: ShopSettings): Result<Unit> =
        apiCall { api.updateSettings(settings.toDto()) }.map { dto ->
            _settings.value = dto.toDomain()
            Unit
        }

    /** Opens or closes the shop. Closing blocks new customer orders on the server. */
    suspend fun setShopOpen(open: Boolean): Result<Unit> {
        if (_settings.value == null) refreshSettings().onFailure { return Result.failure(it) }
        val current = _settings.value ?: return Result.failure(IllegalStateException("Shop settings are not loaded."))
        return saveSettings(current.copy(isOpen = open))
    }

    fun clear() {
        _dashboard.value = null
        _settings.value = null
    }
}