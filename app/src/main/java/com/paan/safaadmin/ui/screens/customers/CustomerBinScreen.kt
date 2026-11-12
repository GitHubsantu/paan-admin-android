package com.paan.safaadmin.ui.screens.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.core.util.asIndianPhone
import com.paan.safaadmin.core.util.asOrderTime
import com.paan.safaadmin.data.repository.CustomerRepository
import com.paan.safaadmin.domain.model.DeletedCustomer
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BinState(
    val items: List<DeletedCustomer> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val busyId: String? = null,
    val message: String? = null,
)

@HiltViewModel
class CustomerBinViewModel @Inject constructor(
    private val repo: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BinState())
    val state: StateFlow<BinState> = _state.asStateFlow()
    private var page = 1

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.deletedPage(1)
                .onSuccess { (list, meta) ->
                    page = 1
                    _state.update { it.copy(items = list, isLoading = false, canLoadMore = (meta?.lastPage ?: 1) > 1) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load the bin") } }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (!s.canLoadMore || s.isLoadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            repo.deletedPage(page + 1)
                .onSuccess { (list, meta) ->
                    page += 1
                    _state.update {
                        it.copy(items = it.items + list, isLoadingMore = false, canLoadMore = page < (meta?.lastPage ?: page))
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    fun restore(c: DeletedCustomer) = act(c.id, "${c.name} was restored.") { repo.restore(c.id) }
    fun deleteForever(c: DeletedCustomer) = act(c.id, "${c.name} was deleted forever.") { repo.purge(c.id) }

    private fun act(id: String, okMessage: String, block: suspend () -> Result<Any?>) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = id, message = null) }
            block()
                .onSuccess {
                    _state.update { s -> s.copy(busyId = null, items = s.items.filterNot { it.id == id }, message = okMessage) }
                }
                .onFailure { e -> _state.update { s -> s.copy(busyId = null, message = e.message ?: "Something went wrong") } }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBinScreen(
    onBack: () -> Unit,
    viewModel: CustomerBinViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var restoreTarget by remember { mutableStateOf<DeletedCustomer?>(null) }
    var purgeTarget by remember { mutableStateOf<DeletedCustomer?>(null) }
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(s.message) {
        s.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Customer bin", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PaanColors.ForestGreen) }
                s.error != null -> EmptyState("Couldn't load the bin", s.error!!, actionLabel = "Try again", onAction = viewModel::load)
                s.items.isEmpty() -> EmptyState("The bin is empty", "Deleted customers appear here. You can restore them or delete them forever.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    item(key = "hint") {
                        Text(
                            "Deleted customers can't log in. Restore them, or delete them forever.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaanColors.MutedBrown,
                        )
                    }
                    items(s.items, key = { it.id }, contentType = { "deleted" }) { c ->
                        BinCard(
                            c = c,
                            busy = s.busyId == c.id,
                            enabled = s.busyId == null,
                            onRestore = { restoreTarget = c },
                            onDeleteForever = { typed = ""; purgeTarget = c },
                        )
                    }
                    if (s.canLoadMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                if (s.isLoadingMore) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = PaanColors.ForestGreen)
                                else TextButton(onClick = viewModel::loadMore) { Text("Load more", color = PaanColors.ForestGreen) }
                            }
                        }
                    }
                }
            }
        }
    }

    restoreTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text("Restore ${c.name}?") },
            text = { Text("The customer comes back as active and signs in again with an OTP.") },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; viewModel.restore(c) }) { Text("Restore", color = PaanColors.ForestGreen) }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("Cancel", color = PaanColors.MutedBrown) } },
            containerColor = PaanColors.Cream,
        )
    }

    purgeTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { purgeTarget = null },
            title = { Text("Delete ${c.name} forever?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This removes the name, phone, email and login from the database. Past orders stay in your " +
                                "sales history as \"Deleted customer\". This can't be undone.",
                    )
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it.take(10) },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed == "DELETE",
                    onClick = { purgeTarget = null; viewModel.deleteForever(c) },
                ) { Text("Delete forever", color = if (typed == "DELETE") PaanColors.Maroon else PaanColors.MutedBrown) }
            },
            dismissButton = { TextButton(onClick = { purgeTarget = null }) { Text("Cancel", color = PaanColors.ForestGreen) } },
            containerColor = PaanColors.Cream,
        )
    }
}

@Composable
private fun BinCard(c: DeletedCustomer, busy: Boolean, enabled: Boolean, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(c.name, style = MaterialTheme.typography.titleMedium)
            Text(c.phone.asIndianPhone(), style = MaterialTheme.typography.bodyMedium, color = PaanColors.MutedBrown)
            Text(
                "${c.ordersCount} ${if (c.ordersCount == 1) "order" else "orders"} • Deleted ${c.deletedAt.asOrderTime()}",
                style = MaterialTheme.typography.labelSmall,
                color = PaanColors.MutedBrown,
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onRestore, enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.ForestGreen),
                ) { Text("Restore") }
                OutlinedButton(
                    onClick = onDeleteForever, enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.Maroon),
                ) { Text("Delete forever") }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PaanColors.ForestGreen)
            }
        }
    }
}