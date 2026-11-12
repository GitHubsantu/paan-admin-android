package com.paan.safaadmin.ui.screens.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paan.safaadmin.core.util.asAgo
import com.paan.safaadmin.core.util.asRupees
import com.paan.safaadmin.domain.model.Order
import com.paan.safaadmin.domain.model.OrderStatus
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.OrderStatusChip
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.PaanLogo
import com.paan.safaadmin.ui.components.AdminBottomBar
import com.paan.safaadmin.ui.components.AdminTab
import com.paan.safaadmin.ui.components.NotificationPermissionPrompt
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PaanTextStyles
import com.paan.safaadmin.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onOpenOrder: (String) -> Unit,
    onTabSelected: (AdminTab) -> Unit,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    NotificationPermissionPrompt("You'll hear an alert the moment a new order arrives, even when the phone is locked.")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val snackbar = remember { SnackbarHostState() }
    var showLogout by remember { mutableStateOf(false) }

    // Keep the screen awake while the queue is open (counter tablet/phone).
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
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
            Column {
                QueueHeader(onLogout = { showLogout = true })
                SummaryRow(
                    state = state,
                    onSelect = viewModel::onFilterSelected,
                )
                PrimaryTabRow(
                    selectedTabIndex = state.tab.ordinal,
                    containerColor = PaanColors.Cream,
                    contentColor = PaanColors.ForestGreen,
                ) {
                    QueueTab.entries.forEach { tab ->
                        Tab(
                            selected = state.tab == tab,
                            onClick = { viewModel.onTabSelected(tab) },
                            text = { Text(tab.title, style = MaterialTheme.typography.labelLarge) },
                            unselectedContentColor = PaanColors.MutedBrown,
                        )
                    }
                }
            }
        },
        bottomBar = { AdminBottomBar(AdminTab.Orders, onTabSelected) },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                state.error != null -> EmptyState(
                    title = "Couldn't load orders",
                    message = state.error!!,
                    actionLabel = "Try again",
                    onAction = viewModel::retry,
                )
                state.orders.isEmpty() && state.tab == QueueTab.Active && state.filter != QueueFilter.All -> EmptyState(
                    title = "No ${state.filter.title.lowercase()} orders",
                    message = "Tap All to see every active order.",
                )
                state.orders.isEmpty() && state.tab == QueueTab.Active -> EmptyState(
                    title = "No active orders",
                    message = "New orders appear here by themselves, with a sound.",
                )
                state.orders.isEmpty() -> EmptyState(
                    title = "No past orders yet",
                    message = "Collected and cancelled orders will show up here.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    items(state.orders, key = { it.id }, contentType = { "order" }) { order ->
                        OrderQueueCard(
                            order = order,
                            busy = order.id in state.busyIds,
                            onOpen = { onOpenOrder(order.id) },
                            onAdvance = { viewModel.advance(order) },
                        )
                    }
                }
            }
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Sign out?") },
            text = { Text("You will need your password to sign in again.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    viewModel.logout()
                }) { Text("Sign out", color = PaanColors.Maroon) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Cancel", color = PaanColors.ForestGreen) }
            },
            containerColor = PaanColors.Cream,
        )
    }
}

@Composable
private fun QueueHeader(onLogout: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PaanColors.DeepForest)
            .statusBarsPadding()
            .heightIn(min = Dimens.TopBarHeight)
            .padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaanLogo(logoSize = 36.dp)
        Spacer(Modifier.width(Dimens.SpaceSm))
        Column(Modifier.weight(1f)) {
            Text("Safar Paan Admin", style = MaterialTheme.typography.titleLarge, color = PaanColors.Cream)
            Text("Order queue", style = MaterialTheme.typography.labelSmall, color = PaanColors.LightGold)
        }
        IconButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = PaanColors.Cream)
        }
    }
}

@Composable
private fun SummaryRow(state: QueueUiState, onSelect: (QueueFilter) -> Unit) {
    // Tab is Active-only for filters; on History nothing is highlighted.
    val selected = if (state.tab == QueueTab.Active) state.filter else null
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        SummaryCard("All", state.allCount, selected == QueueFilter.All, Modifier.weight(1f)) { onSelect(QueueFilter.All) }
        SummaryCard("New", state.newCount, selected == QueueFilter.New, Modifier.weight(1f), highlight = state.newCount > 0) { onSelect(QueueFilter.New) }
        SummaryCard("Preparing", state.makingCount, selected == QueueFilter.Preparing, Modifier.weight(1f)) { onSelect(QueueFilter.Preparing) }
        SummaryCard("Ready", state.readyCount, selected == QueueFilter.Ready, Modifier.weight(1f)) { onSelect(QueueFilter.Ready) }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> PaanColors.ForestGreen
                highlight -> PaanColors.RoyalGold.copy(alpha = 0.35f)
                else -> PaanColors.CardWhite
            },
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PaanColors.RoyalGold else PaanColors.RoyalGold.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = PaanTextStyles.Currency, color = if (selected) PaanColors.Cream else PaanColors.ForestGreen)
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) PaanColors.LightGold else PaanColors.MutedBrown, maxLines = 1)
        }
    }
}

@Composable
private fun OrderQueueCard(
    order: Order,
    busy: Boolean,
    onOpen: () -> Unit,
    onAdvance: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(
            if (order.status == OrderStatus.PENDING) 2.dp else 1.dp,
            if (order.status == OrderStatus.PENDING) PaanColors.RoyalGold else PaanColors.RoyalGold.copy(alpha = 0.25f),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(PaanColors.DeepForest, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("#%02d".format(order.token), style = PaanTextStyles.TokenCode, color = PaanColors.Cream)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${order.createdAt.asAgo()} • ${order.customerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaanColors.MutedBrown,
                    )
                }
                OrderStatusChip(order.status)
            }

            order.items.forEach { line ->
                Column {
                    Text("${line.quantity} × ${line.name}", style = MaterialTheme.typography.titleMedium)
                    Text(line.options, style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown)
                }
            }

            if (order.comment.isNotBlank()) {
                Surface(shape = MaterialTheme.shapes.small, color = PaanColors.RoyalGold.copy(alpha = 0.2f)) {
                    Text(
                        "Note: ${order.comment}",
                        Modifier.fillMaxWidth().padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            HorizontalDivider(color = PaanColors.RoyalGold.copy(alpha = 0.3f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.total.asRupees(), style = PaanTextStyles.Currency)
                    Text(order.paymentLabel, style = MaterialTheme.typography.labelSmall, color = PaanColors.FreshGreen)
                }
                order.status.actionLabel?.let { label ->
                    Button(
                        onClick = onAdvance,
                        enabled = !busy,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaanColors.ForestGreen,
                            contentColor = PaanColors.Cream,
                        ),
                        modifier = Modifier.height(Dimens.ButtonHeight),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = PaanColors.Cream)
                        } else {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}