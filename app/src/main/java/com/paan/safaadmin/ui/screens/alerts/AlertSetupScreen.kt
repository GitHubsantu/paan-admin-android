package com.paan.safaadmin.ui.screens.alerts

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.paan.safaadmin.push.AdminAlerts
import com.paan.safaadmin.push.NewOrderAlert
import com.paan.safaadmin.push.NotificationChannels
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }   // re-check after coming back from Settings

    val manager = context.getSystemService(NotificationManager::class.java)
    val notificationsOn = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val soundOn = remember(tick) {
        val importance = manager.getNotificationChannel(NotificationChannels.NEW_ORDER)?.importance
            ?: NotificationManager.IMPORTANCE_NONE
        importance >= NotificationManager.IMPORTANCE_HIGH
    }
    val fullScreenOn = remember(tick) { Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent() }
    var testing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Order alerts", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.ScreenMargin),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                StatusRow("Notifications allowed", notificationsOn, "Open settings") {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
                StatusRow("New-order sound and pop-up on", soundOn, "Open sound settings") {
                    context.startActivity(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannels.NEW_ORDER),
                    )
                }
                StatusRow("Full-screen alert on the lock screen", fullScreenOn, "Allow") { openFullScreenSettings(context) }

                Button(
                    onClick = {
                        testing = true
                        scope.launch {
                            delay(10_000)   // time to lock the phone
                            AdminAlerts.showNewOrder(context, NewOrderAlert.sample())
                            testing = false
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = PaanColors.ForestGreen, contentColor = PaanColors.Cream),
                ) { Text(if (testing) "Lock your phone now… (10 s)" else "Send test alert") }

                Text(
                    "Tip: tap the button, then lock the phone. The screen should wake with the New Order card and the custom sound.",
                    style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown,
                )
                Text(
                    "If the lock-screen card does not appear on Xiaomi/Redmi: Settings → Apps → Manage apps → Safar Paan Admin → " +
                            "Other permissions → turn on \"Show on lock screen\" and \"Display pop-up windows while running in background\". " +
                            "Also set Battery saver to \"No restrictions\" and turn Autostart on. On Samsung: Settings → Apps → Safar Paan Admin → " +
                            "Battery → Unrestricted. Do not swipe the app away with \"Force stop\".",
                    style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown,
                )
            }
        }
    }
}

private fun openFullScreenSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= 34) {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

@Composable
private fun StatusRow(label: String, ok: Boolean, actionLabel: String, onAction: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
        border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (ok) "✓ OK" else "✗ Needs your attention",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ok) PaanColors.FreshGreen else PaanColors.Maroon,
                )
            }
            if (!ok) TextButton(onClick = onAction) { Text(actionLabel, color = PaanColors.ForestGreen) }
        }
    }
}