package com.paan.safaadmin.ui.screens.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Add
import coil3.compose.AsyncImage
import com.paan.safaadmin.core.util.asRupees
import com.paan.safaadmin.domain.model.MenuItem
import com.paan.safaadmin.ui.components.AdminBottomBar
import com.paan.safaadmin.ui.components.AdminTab
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.ScreenHeader
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PillShape

@Composable
fun MenuScreen(
    onTabSelected: (AdminTab) -> Unit,
    onAddProduct: () -> Unit, onEditProduct: (String) -> Unit,
    onOpenCategories: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                containerColor = PaanColors.ForestGreen,
                contentColor = PaanColors.Cream,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add paan") },
            )
        },
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                ScreenHeader(
                    "Menu",
                    if (state.soldOutCount > 0) "${state.soldOutCount} sold out" else "Everything is available",
                )
                TextButton(
                    onClick = onOpenCategories,
                    modifier = Modifier.background(PaanColors.Cream).padding(horizontal = 8.dp),
                ) { Text("Manage categories", color = PaanColors.ForestGreen) }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PaanColors.Cream)
                        .padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceSm),
                    singleLine = true,
                    placeholder = { Text("Search paan or category") },
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
        bottomBar = { AdminBottomBar(AdminTab.Menu, onTabSelected) },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                state.error != null -> EmptyState(
                    title = "Couldn't load the menu",
                    message = state.error!!,
                    actionLabel = "Try again",
                    onAction = viewModel::refresh,
                )
                state.items.isEmpty() -> EmptyState(
                    title = "No products found",
                    message = "Try a different search.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    items(state.items, key = { it.id }, contentType = { "product" }) { item ->
                        MenuRow(
                            item,
                            onToggle = { viewModel.setAvailable(item, it) },
                            onClick = { onEditProduct(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaanColors.ForestGreen.copy(alpha = 0.08f)),
            ) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${item.price.asRupees()}${if (item.category.isNotEmpty()) " • ${item.category}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaanColors.MutedBrown,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PaanColors.ForestGreen,
                        checkedThumbColor = PaanColors.Cream,
                    ),
                )
                Text(
                    if (item.isAvailable) "Available" else "Sold out",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isAvailable) PaanColors.FreshGreen else PaanColors.Maroon,
                )
            }
        }
    }
}