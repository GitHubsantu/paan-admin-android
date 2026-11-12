package com.paan.safaadmin.ui.screens.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.CustomerRepository
import com.paan.safaadmin.domain.model.CustomerRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomersUiState(
    val query: String = "",
    val customers: List<CustomerRow> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val repository: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomersUiState())
    val state: StateFlow<CustomersUiState> = _state.asStateFlow()

    private val query = MutableStateFlow("")
    private var page = 1

    init {
        // A new search starts 0.4 s after the user stops typing.
        viewModelScope.launch {
            query.debounce(400).distinctUntilChanged().collectLatest { loadFirstPage(it, showSpinner = true) }
        }
        // Add / edit / block / delete elsewhere: refresh quietly.
        viewModelScope.launch {
            repository.version.drop(1).collect { loadFirstPage(_state.value.query, showSpinner = false) }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        query.value = value
    }

    fun retry() {
        viewModelScope.launch { loadFirstPage(_state.value.query, showSpinner = true) }
    }

    private suspend fun loadFirstPage(search: String, showSpinner: Boolean) {
        if (showSpinner) _state.update { it.copy(isLoading = true, error = null) }
        repository.page(search, 1)
            .onSuccess { (list, meta) ->
                page = 1
                _state.update {
                    it.copy(
                        customers = list,
                        isLoading = false,
                        error = null,
                        canLoadMore = (meta?.lastPage ?: 1) > 1,
                    )
                }
            }
            .onFailure { e ->
                if (showSpinner) _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load customers") }
            }
    }

    fun loadMore() {
        val s = _state.value
        if (!s.canLoadMore || s.isLoadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            repository.page(s.query, page + 1)
                .onSuccess { (list, meta) ->
                    page += 1
                    _state.update {
                        it.copy(
                            customers = it.customers + list,
                            isLoadingMore = false,
                            canLoadMore = page < (meta?.lastPage ?: page),
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }
}