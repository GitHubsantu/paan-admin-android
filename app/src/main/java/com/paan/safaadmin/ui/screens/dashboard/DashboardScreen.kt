package com.paan.safaadmin.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paan.safaadmin.core.util.asRupees
import com.paan.safaadmin.domain.model.Dashboard
import com.paan.safaadmin.domain.model.OrderStatus
import com.paan.safaadmin.ui.components.AdminBottomBar
import com.paan.safaadmin.ui.components.AdminTab
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.OrderStatusChip
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.ScreenHeader
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PaanTextStyles

@Composable
fun DashboardScreen(
    onTabSelected: (AdminTab) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmClose by remember { mutableStateOf(false) }

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
        topBar = { ScreenHeader("Dashboard", "Today at the counter") },
        bottomBar = { AdminBottomBar(AdminTab.Dashboard, onTabSelected) },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            val dashboard = state.dashboard
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                dashboard == null -> EmptyState(
                    title = "Couldn't load the dashboard",
                    message = state.error ?: "Please try again.",
                    actionLabel = "Try again",
                    onAction = viewModel::retry,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    item {
                        ShopStatusCard(
                            open = state.shopOpen,
                            toggling = state.toggling,
                            onToggle = { wantOpen ->
                                if (wantOpen) viewModel.setShopOpen(true) else confirmClose = true
                            },
                        )
                    }
                    item { StatsGrid(dashboard) }
                    item { BreakdownCard(dashboard) }
                }
            }
        }
    }

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("Close the shop?") },
            text = { Text("Customers won't be able to place new orders until you open it again. Orders already placed are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    viewModel.setShopOpen(false)
                }) { Text("Close shop", color = PaanColors.Maroon) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text("Keep open", color = PaanColors.ForestGreen) }
            },
            containerColor = PaanColors.Cream,
        )
    }
}

@Composable
private fun ShopStatusCard(open: Boolean?, toggling: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (open == false) PaanColors.Maroon.copy(alpha = 0.1f) else PaanColors.CardWhite,
        ),
        border = BorderStroke(1.dp, if (open == false) PaanColors.Maroon else PaanColors.RoyalGold.copy(alpha = 0.4f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (open) {
                        true -> "Shop is open"
                        false -> "Shop is closed"
                        null -> "Loading shop status…"
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (open == false) "New orders are blocked" else "Customers can place orders",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaanColors.MutedBrown,
                )
            }
            if (open != null) {
                Switch(
                    checked = open,
                    onCheckedChange = onToggle,
                    enabled = !toggling,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PaanColors.ForestGreen,
                        checkedThumbColor = PaanColors.Cream,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(d: Dashboard) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            StatCard("Orders today", d.todayOrders.toString(), Modifier.weight(1f))
            StatCard("Sales today", d.todaySales.asRupees(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            StatCard("Total sales", d.totalSales.asRupees(), Modifier.weight(1f))
            StatCard("Customers", d.totalCustomers.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = PaanTextStyles.Currency, color = PaanColors.ForestGreen)
            Text(label, style = MaterialTheme.typography.labelMedium, color = PaanColors.MutedBrown)
        }
    }
}

@Composable
private fun BreakdownCard(d: Dashboard) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Orders by status", style = MaterialTheme.typography.titleLarge)
            BreakdownRow(OrderStatus.PENDING, d.pending)
            BreakdownRow(OrderStatus.MAKING, d.making)
            BreakdownRow(OrderStatus.READY, d.ready)
            BreakdownRow(OrderStatus.COMPLETED, d.completed)
            BreakdownRow(OrderStatus.CANCELLED, d.cancelled)
        }
    }
}

@Composable
private fun BreakdownRow(status: OrderStatus, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        OrderStatusChip(status)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}