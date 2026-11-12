package com.paan.safaadmin.ui.screens.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paan.safaadmin.ui.components.AdminBottomBar
import com.paan.safaadmin.ui.components.AdminTab
import com.paan.safaadmin.ui.components.EmptyState
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.PrimaryPaanButton
import com.paan.safaadmin.ui.components.ScreenHeader
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import androidx.compose.material.icons.filled.NotificationsActive

@Composable
fun ShopScreen(
    onTabSelected: (AdminTab) -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenAlertSetup: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val form = state.form

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { ScreenHeader("Shop", "Details customers see") },
        bottomBar = { AdminBottomBar(AdminTab.Shop, onTabSelected) },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                form == null -> EmptyState(
                    title = "Couldn't load settings",
                    message = state.loadError ?: "Please try again.",
                    actionLabel = "Try again",
                    onAction = viewModel::load,
                )
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    Field("Shop name", form.name) { v -> viewModel.edit { it.copy(name = v) } }
                    Field("Address", form.address) { v -> viewModel.edit { it.copy(address = v) } }
                    Field("Shop phone", form.phone, KeyboardType.Phone) { v ->
                        viewModel.edit { it.copy(phone = v.filter(Char::isDigit).take(10)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                        Field("Opens (HH:MM)", form.openingTime, modifier = Modifier.weight(1f)) { v ->
                            viewModel.edit { it.copy(openingTime = v.take(5)) }
                        }
                        Field("Closes (HH:MM)", form.closingTime, modifier = Modifier.weight(1f)) { v ->
                            viewModel.edit { it.copy(closingTime = v.take(5)) }
                        }
                    }
                    Field("Pickup message", form.pickupMessage, singleLine = false) { v ->
                        viewModel.edit { it.copy(pickupMessage = v) }
                    }

                    Card(
                        Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
                        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (form.isOpen) "Shop is open" else "Shop is closed", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Closed shops can't receive new orders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PaanColors.MutedBrown,
                                )
                            }
                            Switch(
                                checked = form.isOpen,
                                onCheckedChange = { v -> viewModel.edit { it.copy(isOpen = v) } },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = PaanColors.ForestGreen,
                                    checkedThumbColor = PaanColors.Cream,
                                ),
                            )
                        }
                    }

                    state.fieldError?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = PaanColors.Error)
                    }

                    PrimaryPaanButton(
                        text = "Save changes",
                        onClick = viewModel::save,
                        isLoading = state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Dimens.SpaceSm))
                    Card(
                        Modifier.fillMaxWidth().clickable(onClick = onOpenCustomers),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
                        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Group, null, tint = PaanColors.ForestGreen)
                            Spacer(Modifier.width(12.dp))
                            Text("Customers", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = PaanColors.MutedBrown)
                        }
                    }
                    Card(
                        Modifier.fillMaxWidth().clickable(onClick = onOpenAlertSetup),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
                        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, null, tint = PaanColors.ForestGreen)
                            Spacer(Modifier.width(12.dp))
                            Text("Order alerts & sound", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = PaanColors.MutedBrown)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PaanColors.CardWhite,
            unfocusedContainerColor = PaanColors.CardWhite,
            focusedBorderColor = PaanColors.ForestGreen,
            focusedLabelColor = PaanColors.ForestGreen,
            cursorColor = PaanColors.ForestGreen,
        ),
    )
}