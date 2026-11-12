package com.paan.safaadmin.ui.screens.customers

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.CustomerRepository
import com.paan.safaadmin.domain.model.CustomerForm
import com.paan.safaadmin.navigation.Route
import com.paan.safaadmin.ui.components.AppField
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.PrimaryPaanButton
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerFormState(
    val isLoading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val originalPhone: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val loadError: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class CustomerFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CustomerRepository,
) : ViewModel() {

    private val customerId: String? = savedStateHandle[Route.CustomerForm.ARG_ID]
    private val _state = MutableStateFlow(CustomerFormState(isEdit = customerId != null, isLoading = customerId != null))
    val state: StateFlow<CustomerFormState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val id = customerId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            repo.detail(id)
                .onSuccess { d ->
                    val c = d.customer
                    _state.update {
                        it.copy(
                            isLoading = false,
                            // The list shows "Customer" when a name is not set; do not prefill that.
                            name = if (c.name == "Customer") "" else c.name,
                            phone = c.phone,
                            originalPhone = c.phone,
                            email = c.email,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load this customer") } }
        }
    }

    fun edit(change: CustomerFormState.() -> CustomerFormState) = _state.update { it.change().copy(error = null) }

    private fun validate(s: CustomerFormState): String? = when {
        s.name.trim().length < 2 -> "Enter the customer's name"
        !Regex("^[6-9]\\d{9}$").matches(s.phone) -> "Enter a 10-digit mobile number starting with 6, 7, 8 or 9"
        s.email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(s.email.trim()).matches() -> "Enter a valid email or leave it empty"
        else -> null
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        validate(s)?.let { err -> _state.update { it.copy(error = err) }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            repo.save(customerId, CustomerForm(s.name, s.phone, s.email))
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                // The form keeps what was typed; the server message shows (e.g. phone already used).
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: CustomerFormViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.done) { if (s.done) onDone() }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (s.isEdit) "Edit customer" else "Add customer", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
        bottomBar = {
            if (!s.isLoading && s.loadError == null) {
                Surface(color = PaanColors.CardWhite, shadowElevation = 12.dp) {
                    PrimaryPaanButton(
                        text = "Save",
                        onClick = viewModel::save,
                        isLoading = s.saving,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                s.loadError != null -> EmptyState("Couldn't load the form", s.loadError!!, actionLabel = "Try again", onAction = viewModel::load)
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppField("Name", s.name, { v -> viewModel.edit { copy(name = v.take(60)) } })
                    AppField(
                        "Mobile number (10 digits)", s.phone,
                        { v -> viewModel.edit { copy(phone = v.filter(Char::isDigit).take(10)) } },
                        keyboardType = KeyboardType.Phone,
                    )
                    if (s.isEdit && s.phone != s.originalPhone) {
                        Text(
                            "Changing the number signs this customer out. They log in again with the new number.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaanColors.MutedBrown,
                        )
                    }
                    AppField(
                        "Email (optional)", s.email,
                        { v -> viewModel.edit { copy(email = v.take(120)) } },
                        keyboardType = KeyboardType.Email,
                    )
                    s.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = PaanColors.Error) }
                }
            }
        }
    }
}