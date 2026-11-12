package com.paan.safaadmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PaanColors.DeepForest)
            .statusBarsPadding()
            .heightIn(min = Dimens.TopBarHeight)
            .padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaanLogo(logoSize = 36.dp)
        Spacer(Modifier.width(Dimens.SpaceSm))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = PaanColors.Cream)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = PaanColors.LightGold) }
        }
    }
}