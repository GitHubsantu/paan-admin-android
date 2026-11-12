package com.paan.safaadmin.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val phoneRegex = Regex("^[6-9][0-9]{9}$")

    fun onPhoneChange(value: String) =
        _state.update { it.copy(phone = value.filter(Char::isDigit).take(10), error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    /** On success the saved login changes, and the navigation moves to the queue by itself. */
    fun submit() {
        val s = _state.value
        val error = when {
            !phoneRegex.matches(s.phone) -> "Enter a valid 10-digit mobile number"
            s.password.isBlank() -> "Enter your password"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            auth.login(s.phone, s.password)
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Could not sign in") } }
                .onSuccess { _state.update { it.copy(isLoading = false) } }
        }
    }
}