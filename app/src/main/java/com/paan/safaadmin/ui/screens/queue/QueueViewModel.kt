package com.paan.safaadmin.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.core.util.ticker
import com.paan.safaadmin.data.repository.AuthRepository
import com.paan.safaadmin.data.repository.LoadState
import com.paan.safaadmin.data.repository.OrderRepository
import com.paan.safaadmin.domain.model.Order
import com.paan.safaadmin.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QueueFilter(val title: String) {
    All("All"), New("New"), Preparing("Preparing"), Ready("Ready");

    fun matches(status: OrderStatus): Boolean = when (this) {
        All -> true
        New -> status == OrderStatus.PENDING
        Preparing -> status == OrderStatus.MAKING
        Ready -> status == OrderStatus.READY
    }
}
enum class QueueTab(val title: String) { Active("Active"), History("History") }

data class QueueUiState(
    val tab: QueueTab = QueueTab.Active,
    val filter: QueueFilter = QueueFilter.All,
    val orders: List<Order> = emptyList(),
    val allCount: Int = 0,
    val newCount: Int = 0,
    val makingCount: Int = 0,
    val readyCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val busyIds: Set<String> = emptySet(),
    val message: String? = null,
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val orders: OrderRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private data class Local(
        val tab: QueueTab = QueueTab.Active,
        val filter: QueueFilter = QueueFilter.All,
        val history: LoadState = LoadState(),
        val busy: Set<String> = emptySet(),
        val message: String? = null,
    )

    private val local = MutableStateFlow(Local())

    // Active orders are refreshed app-wide (SessionViewModel). History is only refreshed while it is open.
    private val historyPolling = ticker(8_000) {
        if (local.value.tab == QueueTab.History) refreshHistory()
    }

    val state: StateFlow<QueueUiState> =
        combine(local, orders.active, orders.history, orders.activeState, historyPolling) { l, active, history, activeLoad, _ ->
            val isActiveTab = l.tab == QueueTab.Active
            val list = if (isActiveTab) active.filter { l.filter.matches(it.status) } else history
            val load = if (isActiveTab) activeLoad else l.history
            QueueUiState(
                tab = l.tab,
                filter = l.filter,
                orders = list,
                allCount = active.size,
                newCount = active.count { it.status == OrderStatus.PENDING },
                makingCount = active.count { it.status == OrderStatus.MAKING },
                readyCount = active.count { it.status == OrderStatus.READY },
                isLoading = !load.loaded && list.isEmpty(),
                error = if (list.isEmpty()) load.error else null,
                busyIds = l.busy,
                message = l.message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QueueUiState())

    private suspend fun refreshHistory() {
        val result = orders.refreshHistory()
        local.update { it.copy(history = LoadState(loaded = true, error = result.exceptionOrNull()?.message)) }
    }

    fun onTabSelected(tab: QueueTab) {
        local.update { it.copy(tab = tab) }
        if (tab == QueueTab.History) retry()
    }

    fun onFilterSelected(filter: QueueFilter) {
        local.update { it.copy(tab = QueueTab.Active, filter = filter) }
    }

    fun retry() {
        viewModelScope.launch {
            if (local.value.tab == QueueTab.History) refreshHistory() else orders.refreshActive()
        }
    }

    fun advance(order: Order) {
        val next = order.status.next ?: return
        act(order.id) { orders.setStatus(order.id, next) }
    }

    private fun act(id: String, block: suspend () -> Result<Order>) {
        if (id in local.value.busy) return
        viewModelScope.launch {
            local.update { it.copy(busy = it.busy + id, message = null) }
            val result = block()
            local.update { it.copy(busy = it.busy - id, message = result.exceptionOrNull()?.message) }
        }
    }

    fun messageShown() = local.update { it.copy(message = null) }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}