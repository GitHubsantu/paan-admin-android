package com.paan.safaadmin.ui.screens.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.ShopRepository
import com.paan.safaadmin.domain.model.ShopSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopUiState(
    val form: ShopSettings? = null,
    val isLoading: Boolean = true,
    val saving: Boolean = false,
    val loadError: String? = null,
    val fieldError: String? = null,
    val message: String? = null,
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shop: ShopRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    private val phoneRegex = Regex("^[6-9][0-9]{9}$")
    private val timeRegex = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            shop.refreshSettings()
                .onSuccess { _state.update { it.copy(isLoading = false, form = shop.settings.value) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load settings") } }
        }
    }

    fun edit(change: (ShopSettings) -> ShopSettings) {
        _state.update { s -> s.form?.let { s.copy(form = change(it), fieldError = null) } ?: s }
    }

    private fun validate(f: ShopSettings): String? = when {
        f.name.isBlank() -> "Enter the shop name"
        !phoneRegex.matches(f.phone.trim()) -> "Enter a 10-digit mobile number"
        !timeRegex.matches(f.openingTime.trim()) -> "Opening time must look like 09:00 (24-hour)"
        !timeRegex.matches(f.closingTime.trim()) -> "Closing time must look like 22:00 (24-hour)"
        else -> null
    }

    fun save() {
        val form = _state.value.form ?: return
        if (_state.value.saving) return
        validate(form)?.let { error ->
            _state.update { it.copy(fieldError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, fieldError = null) }
            shop.saveSettings(form)
                .onSuccess { _state.update { it.copy(saving = false, form = shop.settings.value, message = "Saved") } }
                .onFailure { e -> _state.update { it.copy(saving = false, fieldError = e.message ?: "Couldn't save") } }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}