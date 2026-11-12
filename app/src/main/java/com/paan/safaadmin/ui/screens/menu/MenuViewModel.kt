package com.paan.safaadmin.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.MenuRepository
import com.paan.safaadmin.domain.model.MenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val items: List<MenuItem> = emptyList(),
    val query: String = "",
    val soldOutCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menu: MenuRepository,
) : ViewModel() {

    private data class Local(
        val query: String = "",
        val loaded: Boolean = false,
        val error: String? = null,
        val message: String? = null,
    )

    private val local = MutableStateFlow(Local())

    val state: StateFlow<MenuUiState> = combine(menu.items, local) { items, l ->
        val q = l.query.trim()
        MenuUiState(
            items = if (q.isEmpty()) items else items.filter {
                it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
            },
            query = l.query,
            soldOutCount = items.count { !it.isAvailable },
            isLoading = !l.loaded && items.isEmpty(),
            error = if (items.isEmpty()) l.error else null,
            message = l.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val result = menu.refresh()
            local.update { it.copy(loaded = true, error = result.exceptionOrNull()?.message) }
        }
    }

    fun onQueryChange(value: String) = local.update { it.copy(query = value) }

    fun setAvailable(item: MenuItem, available: Boolean) {
        viewModelScope.launch {
            menu.setAvailable(item.id, available)
                .onFailure { e -> local.update { it.copy(message = e.message ?: "Couldn't update ${item.name}") } }
        }
    }

    fun messageShown() = local.update { it.copy(message = null) }
}