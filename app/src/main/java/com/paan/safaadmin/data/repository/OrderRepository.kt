package com.paan.safaadmin.data.repository

import com.paan.safaadmin.data.remote.AdminApi
import com.paan.safaadmin.data.remote.CancelRequest
import com.paan.safaadmin.data.remote.StatusRequest
import com.paan.safaadmin.data.remote.apiCall
import com.paan.safaadmin.data.remote.toDomain
import com.paan.safaadmin.domain.model.Order
import com.paan.safaadmin.domain.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class LoadState(val loaded: Boolean = false, val error: String? = null)

@Singleton
class OrderRepository @Inject constructor(
    private val api: AdminApi,
) {
    private val _active = MutableStateFlow<List<Order>>(emptyList())
    val active: StateFlow<List<Order>> = _active.asStateFlow()

    private val _history = MutableStateFlow<List<Order>>(emptyList())
    val history: StateFlow<List<Order>> = _history.asStateFlow()

    private val _activeState = MutableStateFlow(LoadState())
    val activeState: StateFlow<LoadState> = _activeState.asStateFlow()

    /** Oldest first: the order that has waited longest is at the top of the kitchen queue. */
    suspend fun refreshActive(): Result<Unit> {
        val result = apiCall { api.orders(status = "active", sort = "oldest", perPage = 100, page = 1) }.map { list ->
            _active.value = list.map { it.toDomain() }.sortedBy { it.createdAt }
            Unit
        }
        _activeState.value = LoadState(loaded = true, error = result.exceptionOrNull()?.message)
        return result
    }

    suspend fun refreshHistory(): Result<Unit> =
        apiCall { api.orders(status = "history", sort = "newest", perPage = 50, page = 1) }.map { list ->
            _history.value = list.map { it.toDomain() }.sortedByDescending { it.createdAt }
            Unit
        }

    suspend fun fetch(id: String): Result<Order> =
        apiCall { api.order(id) }.map { it.toDomain() }

    suspend fun setStatus(id: String, status: OrderStatus, note: String? = null): Result<Order> =
        apiCall { api.updateStatus(id, StatusRequest(status.api, note)) }
            .map { dto -> dto.toDomain().also(::merge) }

    suspend fun cancel(id: String, reason: String): Result<Order> =
        apiCall { api.cancel(id, CancelRequest(reason)) }
            .map { dto -> dto.toDomain().also(::merge) }

    fun clear() {
        _active.value = emptyList()
        _history.value = emptyList()
        _activeState.value = LoadState()
    }

    /** Moves the order between the lists straight away so the queue updates without waiting for the next refresh. */
    private fun merge(order: Order) {
        _active.update { list -> list.filterNot { it.id == order.id } }
        if (order.status.isActive) {
            _active.update { list -> (list + order).sortedBy { it.createdAt } }
        } else {
            _history.update { list -> (listOf(order) + list.filterNot { it.id == order.id }).sortedByDescending { it.createdAt } }
        }
    }
}