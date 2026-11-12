package com.paan.safaadmin.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paan.safaadmin.core.util.asIndianPhone
import com.paan.safaadmin.core.util.asOrderTime
import com.paan.safaadmin.core.util.asRupees
import com.paan.safaadmin.core.util.openDialer
import com.paan.safaadmin.domain.model.Order
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.OrderStatusChip
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.PrimaryPaanButton
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PaanTextStyles
import com.paan.safaadmin.ui.theme.PillShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.paan.safaadmin.core.util.rememberQrImage
import com.paan.safaadmin.domain.model.OrderStatus
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showCancel by remember { mutableStateOf(false) }
    val order = state.order
    val lifecycleOwner = LocalLifecycleOwner.current
    val isReady = order?.status == OrderStatus.READY
    // While the order is ready, check every 5 s so the screen updates when the customer scans.
    LaunchedEffect(isReady, lifecycleOwner) {
        if (isReady) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(5_000)
                    viewModel.refreshQuiet()
                }
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
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
                title = { Text(order?.orderNumber ?: "Order", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
        bottomBar = {
            if (order != null && order.status.isActive) {
                ActionBar(
                    actionLabel = order.status.actionLabel.orEmpty(),
                    busy = state.busy,
                    onAdvance = viewModel::advance,
                    onCancel = { showCancel = true },
                )
            }
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                order == null -> EmptyState(
                    title = "Couldn't load this order",
                    message = state.error ?: "Please try again.",
                    actionLabel = "Try again",
                    onAction = viewModel::load,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    item { TokenCard(order) }
                    if (order.status == OrderStatus.READY) {
                        item { PickupQrCard(order, onReload = viewModel::load) }
                    }
                    item { CustomerCard(order, onCall = { context.openDialer(order.customerPhone) }) }
                    item { ItemsCard(order) }
                    if (order.comment.isNotBlank()) item { NoteCard(order.comment) }
                    item { HistoryCard(order) }
                }
            }
        }
    }

    if (showCancel) {
        CancelDialog(
            onDismiss = { showCancel = false },
            onConfirm = { reason ->
                showCancel = false
                viewModel.cancel(reason)
            },
        )
    }
}

@Composable
private fun ActionBar(actionLabel: String, busy: Boolean, onAdvance: () -> Unit, onCancel: () -> Unit) {
    Surface(color = PaanColors.CardWhite, shadowElevation = 12.dp) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !busy,
                shape = PillShape,
                modifier = Modifier.height(Dimens.ButtonHeight),
                border = BorderStroke(1.5.dp, PaanColors.Maroon),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.Maroon),
            ) { Text("Cancel order") }
            PrimaryPaanButton(
                text = actionLabel,
                onClick = onAdvance,
                isLoading = busy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun TokenCard(order: Order) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.DeepForest),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.6f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("PICKUP TOKEN", style = MaterialTheme.typography.labelMedium, color = PaanColors.LightGold)
            Text("#%02d".format(order.token), style = PaanTextStyles.TokenCode, color = PaanColors.Cream)
            OrderStatusChip(order.status)
            Text("Placed ${order.createdAt.asOrderTime()}", style = MaterialTheme.typography.labelSmall, color = PaanColors.LightGold)
            order.completedViaLabel?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = PaanColors.LightGold)
            }
        }
    }
}

@Composable
private fun CustomerCard(order: Order, onCall: () -> Unit) {
    SectionCard("Customer") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(order.customerName, style = MaterialTheme.typography.titleMedium)
                if (order.customerPhone.isNotBlank()) {
                    Text(order.customerPhone.asIndianPhone(), style = MaterialTheme.typography.bodyMedium, color = PaanColors.MutedBrown)
                }
            }
            if (order.customerPhone.isNotBlank()) {
                FilledTonalIconButton(onClick = onCall) {
                    Icon(Icons.Filled.Call, contentDescription = "Call customer", tint = PaanColors.ForestGreen)
                }
            }
        }
    }
}

@Composable
private fun ItemsCard(order: Order) {
    SectionCard("Items (${order.itemCount})") {
        order.items.forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("${line.quantity} × ${line.name}", style = MaterialTheme.typography.titleMedium)
                    Text(line.options, style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown)
                }
                Text((line.unitPrice * line.quantity).asRupees(), style = MaterialTheme.typography.titleMedium)
            }
        }
        HorizontalDivider(color = PaanColors.RoyalGold.copy(alpha = 0.3f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Total", style = MaterialTheme.typography.titleMedium)
                Text(order.paymentLabel, style = MaterialTheme.typography.labelSmall, color = PaanColors.FreshGreen)
            }
            Text(order.total.asRupees(), style = PaanTextStyles.Currency)
        }
    }
}

@Composable
private fun NoteCard(comment: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = PaanColors.RoyalGold.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, PaanColors.RoyalGold),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Customer note", style = MaterialTheme.typography.labelMedium, color = PaanColors.DarkBrown)
            Text("\"$comment\"", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HistoryCard(order: Order) {
    if (order.history.isEmpty()) return
    SectionCard("History") {
        order.history.forEach { h ->
            Column {
                Text("${h.status.label} • ${h.at.asOrderTime()}", style = MaterialTheme.typography.bodyMedium)
                h.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown) }
            }
        }
    }
}

@Composable
private fun CancelDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val invalid = reason.trim().length < 3

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this order?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The customer will be notified and a paid order is refunded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaanColors.MutedBrown,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 255) reason = it },
                    label = { Text("Reason (required)") },
                    isError = tried && invalid,
                    supportingText = if (tried && invalid) ({ Text("Enter a short reason") }) else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaanColors.ForestGreen,
                        focusedLabelColor = PaanColors.ForestGreen,
                        cursorColor = PaanColors.ForestGreen,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (!invalid) onConfirm(reason)
            }) { Text("Cancel order", color = PaanColors.Maroon) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep order", color = PaanColors.ForestGreen) }
        },
        containerColor = PaanColors.Cream,
    )
}


@Composable
private fun PickupQrCard(order: Order, onReload: () -> Unit) {
    val payload = order.pickupQrPayload
    SectionCard("Pickup QR") {
        if (payload == null) {
            Text("The QR code isn't loaded yet.", style = MaterialTheme.typography.bodyMedium, color = PaanColors.MutedBrown)
            TextButton(onClick = onReload) { Text("Reload", color = PaanColors.ForestGreen) }
        } else {
            val qr = rememberQrImage(payload)
            Text(
                "Ask the customer to scan this in their Safar Paan app. You can also tap \"Mark collected\" below.",
                style = MaterialTheme.typography.bodyMedium,
                color = PaanColors.MutedBrown,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.background(Color.White, MaterialTheme.shapes.medium).padding(4.dp)) {
                    Image(
                        bitmap = qr,
                        contentDescription = "Pickup QR code for order ${order.orderNumber}",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.size(250.dp),
                    )
                }
            }
        }
    }
}