package com.paan.safaadmin.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.OrderRepository
import com.paan.safaadmin.domain.model.Order
import com.paan.safaadmin.domain.model.OrderStatus
import com.paan.safaadmin.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrderRepository,
) : ViewModel() {

    private val orderId: String = savedStateHandle[Route.Detail.ARG_ID] ?: ""
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            orders.fetch(orderId)
                .onSuccess { o -> _state.update { it.copy(isLoading = false, order = o) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load this order") } }
        }
    }

    /** Reloads the order without any spinner (used while an order is ready, to see a QR pickup). */
    fun refreshQuiet() {
        if (_state.value.busy) return
        viewModelScope.launch {
            orders.fetch(orderId).onSuccess { o ->
                _state.update { if (it.busy) it else it.copy(order = o) }
            }
        }
    }

    fun advance() {
        val order = _state.value.order ?: return
        val next = order.status.next ?: return
        act { orders.setStatus(order.id, next) }
    }

    fun cancel(reason: String) {
        val order = _state.value.order ?: return
        act { orders.cancel(order.id, reason.trim()) }
    }

    private fun act(block: suspend () -> Result<Order>) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            block()
                .onSuccess { o ->
                    _state.update { it.copy(busy = false, order = o) }
                    // The status call may not include the pickup QR: load it now.
                    if (o.status == OrderStatus.READY && o.pickupQrPayload == null) refreshQuiet()
                }
                .onFailure { e -> _state.update { it.copy(busy = false, message = e.message ?: "Something went wrong") } }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}