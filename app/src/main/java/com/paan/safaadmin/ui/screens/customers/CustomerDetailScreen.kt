package com.paan.safaadmin.ui.screens.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.core.util.asIndianPhone
import com.paan.safaadmin.core.util.asOrderTime
import com.paan.safaadmin.core.util.asRupees
import com.paan.safaadmin.data.repository.CustomerRepository
import com.paan.safaadmin.domain.model.CustomerDetail
import com.paan.safaadmin.domain.model.RecentOrder
import com.paan.safaadmin.navigation.Route
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.OrderStatusChip
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PaanTextStyles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetailState(
    val isLoading: Boolean = true,
    val detail: CustomerDetail? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CustomerRepository,
) : ViewModel() {

    private val customerId: String = savedStateHandle[Route.CustomerDetail.ARG_ID] ?: ""
    private val _state = MutableStateFlow(CustomerDetailState())
    val state: StateFlow<CustomerDetailState> = _state.asStateFlow()

    init {
        load()
        // After edit / block / unblock: reload quietly.
        viewModelScope.launch { repo.version.drop(1).collect { fetch() } }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            fetch()
        }
    }

    private suspend fun fetch() {
        repo.detail(customerId)
            .onSuccess { d -> _state.update { it.copy(isLoading = false, detail = d, error = null) } }
            .onFailure { e ->
                _state.update {
                    if (it.detail == null) it.copy(isLoading = false, error = e.message ?: "Couldn't load this customer")
                    else it.copy(isLoading = false)
                }
            }
    }

    fun setBlocked(blocked: Boolean, reason: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            repo.setBlocked(customerId, blocked, reason)
                .onSuccess {
                    _state.update { s -> s.copy(busy = false, message = if (blocked) "Customer blocked." else "Customer unblocked.") }
                }
                .onFailure { e -> _state.update { s -> s.copy(busy = false, message = e.message ?: "Something went wrong") } }
        }
    }

    fun delete() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            repo.delete(customerId)
                .onSuccess { _state.update { s -> s.copy(busy = false, deleted = true) } }
                // e.g. "This customer has active orders. Finish or cancel them first."
                .onFailure { e -> _state.update { s -> s.copy(busy = false, message = e.message ?: "Couldn't delete") } }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

private enum class CustomerDialog { Block, Unblock, Delete }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<CustomerDialog?>(null) }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(s.deleted) { if (s.deleted) onBack() }
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
                title = { Text("Customer", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            val d = s.detail
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                s.error != null -> EmptyState("Couldn't load this customer", s.error!!, actionLabel = "Try again", onAction = viewModel::load)
                d != null -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    item(key = "info") { InfoCard(d) }
                    item(key = "stats") {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                            StatCard("Orders", "${d.customer.ordersCount}", Modifier.weight(1f))
                            StatCard("Total spent", d.totalSpent.asRupees(), Modifier.weight(1f))
                        }
                    }
                    item(key = "recent-title") {
                        Text("Recent orders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (d.recentOrders.isEmpty()) {
                        item(key = "recent-empty") {
                            Text("No orders yet.", style = MaterialTheme.typography.bodyMedium, color = PaanColors.MutedBrown)
                        }
                    } else {
                        items(d.recentOrders, key = { it.id }, contentType = { "order" }) { o ->
                            OrderRow(o, onClick = { onOpenOrder(o.id) })
                        }
                    }
                    item(key = "actions") {
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                            OutlinedButton(
                                onClick = { onEdit(d.customer.id) },
                                enabled = !s.busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.ForestGreen),
                            ) { Text("Edit details") }
                            if (d.customer.isActive) {
                                OutlinedButton(
                                    onClick = { reason = ""; dialog = CustomerDialog.Block },
                                    enabled = !s.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.Maroon),
                                ) { Text("Block customer") }
                            } else {
                                OutlinedButton(
                                    onClick = { dialog = CustomerDialog.Unblock },
                                    enabled = !s.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.ForestGreen),
                                ) { Text("Unblock customer") }
                            }
                            OutlinedButton(
                                onClick = { dialog = CustomerDialog.Delete },
                                enabled = !s.busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.Maroon),
                            ) { Text("Delete customer") }
                        }
                    }
                }
            }
        }
    }

    val name = s.detail?.customer?.name ?: "this customer"
    when (dialog) {
        CustomerDialog.Block -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Block $name?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("They are signed out right away and can't log in or place orders. You can unblock them later.")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(255) },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setBlocked(true, reason); dialog = null }) {
                    Text("Block", color = PaanColors.Maroon)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel", color = PaanColors.ForestGreen) } },
            containerColor = PaanColors.Cream,
        )
        CustomerDialog.Unblock -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Unblock $name?") },
            text = { Text("They can log in and order again.") },
            confirmButton = {
                TextButton(onClick = { viewModel.setBlocked(false, ""); dialog = null }) {
                    Text("Unblock", color = PaanColors.ForestGreen)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel", color = PaanColors.ForestGreen) } },
            containerColor = PaanColors.Cream,
        )
        CustomerDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete $name?") },
            text = {
                Text(
                    "They are signed out and their account is removed. Past orders stay in your history. " +
                            "You can't delete a customer who has active orders."
                )
            },
            confirmButton = {
                TextButton(onClick = { dialog = null; viewModel.delete() }) { Text("Delete", color = PaanColors.Maroon) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Keep", color = PaanColors.ForestGreen) } },
            containerColor = PaanColors.Cream,
        )
        null -> Unit
    }
}

private val cardBorder = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f))

@Composable
private fun InfoCard(d: CustomerDetail) {
    val c = d.customer
    val initials = c.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = cardBorder,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).background(if (c.isActive) PaanColors.ForestGreen else PaanColors.MutedBrown, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(initials, style = MaterialTheme.typography.titleLarge, color = PaanColors.Cream) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleLarge)
                    Text(c.phone.asIndianPhone(), style = MaterialTheme.typography.bodyMedium, color = PaanColors.MutedBrown)
                }
            }
            if (c.email.isNotEmpty()) {
                Text(c.email, style = MaterialTheme.typography.bodyMedium, color = PaanColors.MutedBrown)
            }
            Text("Joined ${c.joinedAt.asOrderTime()}", style = MaterialTheme.typography.labelMedium, color = PaanColors.MutedBrown)
            if (!c.isActive) {
                Surface(shape = MaterialTheme.shapes.small, color = PaanColors.Maroon.copy(alpha = 0.1f)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("Blocked", style = MaterialTheme.typography.labelLarge, color = PaanColors.Maroon)
                        c.blockedReason?.let {
                            Text("Reason: $it", style = MaterialTheme.typography.bodySmall, color = PaanColors.DarkBrown)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = cardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = PaanTextStyles.Currency, color = PaanColors.ForestGreen)
            Text(label, style = MaterialTheme.typography.labelMedium, color = PaanColors.MutedBrown)
        }
    }
}

@Composable
private fun OrderRow(o: RecentOrder, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = cardBorder,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(o.orderNumber, style = MaterialTheme.typography.titleMedium)
                Text(o.createdAt.asOrderTime(), style = MaterialTheme.typography.labelSmall, color = PaanColors.MutedBrown)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(o.total.asRupees(), style = MaterialTheme.typography.titleMedium)
                OrderStatusChip(o.status)
            }
        }
    }
}