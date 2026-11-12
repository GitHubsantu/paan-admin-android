package com.paan.safaadmin.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.core.util.ticker
import com.paan.safaadmin.data.repository.ShopRepository
import com.paan.safaadmin.domain.model.Dashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val dashboard: Dashboard? = null,
    val shopOpen: Boolean? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val toggling: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shop: ShopRepository,
) : ViewModel() {

    private data class Local(
        val loaded: Boolean = false,
        val error: String? = null,
        val toggling: Boolean = false,
        val message: String? = null,
    )

    private val local = MutableStateFlow(Local())

    private val polling = ticker(30_000) { refresh() }

    val state: StateFlow<DashboardUiState> =
        combine(shop.dashboard, shop.settings, local, polling) { dashboard, settings, l, _ ->
            DashboardUiState(
                dashboard = dashboard,
                shopOpen = settings?.isOpen,
                isLoading = !l.loaded && dashboard == null,
                error = if (dashboard == null) l.error else null,
                toggling = l.toggling,
                message = l.message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private suspend fun refresh() {
        val result = shop.refreshDashboard()
        if (shop.settings.value == null) shop.refreshSettings()
        local.update { it.copy(loaded = true, error = result.exceptionOrNull()?.message) }
    }

    fun retry() {
        viewModelScope.launch { refresh() }
    }

    fun setShopOpen(open: Boolean) {
        if (local.value.toggling) return
        viewModelScope.launch {
            local.update { it.copy(toggling = true, message = null) }
            val result = shop.setShopOpen(open)
            local.update { it.copy(toggling = false, message = result.exceptionOrNull()?.message) }
        }
    }

    fun messageShown() = local.update { it.copy(message = null) }
}