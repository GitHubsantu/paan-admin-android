package com.paan.safaadmin.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.paan.safaadmin.ui.theme.PaanColors

enum class AdminTab(val label: String, val icon: ImageVector) {
    Orders("Orders", Icons.Filled.ConfirmationNumber),
    Dashboard("Dashboard", Icons.Filled.Dashboard),
    Menu("Menu", Icons.Filled.RestaurantMenu),
    Shop("Shop", Icons.Filled.Storefront),
}

@Composable
fun AdminBottomBar(selected: AdminTab, onSelected: (AdminTab) -> Unit) {
    NavigationBar(containerColor = PaanColors.CardWhite) {
        AdminTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelected(tab) },
                label = { Text(tab.label) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = PaanColors.ForestGreen,
                    selectedIconColor = PaanColors.LightGold,
                    selectedTextColor = PaanColors.ForestGreen,
                    unselectedIconColor = PaanColors.MutedBrown,
                    unselectedTextColor = PaanColors.MutedBrown,
                ),
            )
        }
    }
}