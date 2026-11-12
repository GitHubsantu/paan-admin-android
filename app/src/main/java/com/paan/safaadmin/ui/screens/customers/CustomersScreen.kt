package com.paan.safaadmin.ui.screens.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Delete
import com.paan.safaadmin.core.util.asIndianPhone
import com.paan.safaadmin.core.util.asOrderTime
import com.paan.safaadmin.domain.model.CustomerRow
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onBack: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onAddCustomer: () -> Unit,
    viewModel: CustomersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomer,
                containerColor = PaanColors.ForestGreen,
                contentColor = PaanColors.Cream,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add customer") },
            )
        },
        topBar = {
            Column(Modifier.background(PaanColors.Cream)) {
                TopAppBar(
                    title = { Text("Customers", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        IconButton(onClick = onOpenBin) { Icon(Icons.Filled.Delete, contentDescription = "Deleted customers") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceSm),
                    singleLine = true,
                    placeholder = { Text("Search name or phone") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = PillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PaanColors.CardWhite,
                        unfocusedContainerColor = PaanColors.CardWhite,
                        focusedBorderColor = PaanColors.ForestGreen,
                        cursorColor = PaanColors.ForestGreen,
                    ),
                )
            }
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                state.error != null -> EmptyState(
                    title = "Couldn't load customers",
                    message = state.error!!,
                    actionLabel = "Try again",
                    onAction = viewModel::retry,
                )
                state.customers.isEmpty() -> EmptyState(
                    title = "No customers found",
                    message = "Customers appear here after they sign up, or tap \"Add customer\".",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    items(state.customers, key = { it.id }, contentType = { "customer" }) { c ->
                        CustomerCard(c, onClick = { onOpenCustomer(c.id) })
                    }
                    if (state.canLoadMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = PaanColors.ForestGreen)
                                } else {
                                    TextButton(onClick = viewModel::loadMore) {
                                        Text("Load more", color = PaanColors.ForestGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(c: CustomerRow, onClick: () -> Unit) {
    val initials = c.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(if (c.isActive) PaanColors.ForestGreen else PaanColors.MutedBrown, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(initials, style = MaterialTheme.typography.titleMedium, color = PaanColors.Cream) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(c.phone.asIndianPhone(), style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown)
                Text(
                    "Joined ${c.joinedAt.asOrderTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaanColors.MutedBrown,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${c.ordersCount}", style = MaterialTheme.typography.titleMedium, color = PaanColors.ForestGreen)
                Text(if (c.ordersCount == 1) "order" else "orders", style = MaterialTheme.typography.labelSmall, color = PaanColors.MutedBrown)
                if (!c.isActive) Text("Blocked", style = MaterialTheme.typography.labelMedium, color = PaanColors.Maroon)
            }
        }
    }
}