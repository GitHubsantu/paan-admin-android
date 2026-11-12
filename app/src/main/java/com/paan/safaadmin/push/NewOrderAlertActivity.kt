package com.paan.safaadmin.push

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.theme.PaanColors
import com.paan.safaadmin.ui.theme.PaanTextStyles
import com.paan.safaadmin.ui.theme.PillShape
import com.paan.safaadmin.ui.theme.SafaTheme
import kotlinx.coroutines.delay

class NewOrderAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the lock screen and wake the screen up.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val alert = NewOrderAlert.from(intent)
        setContent {
            SafaTheme {
                AlertScreen(alert, onView = { viewOrder(alert) }, onDismiss = { dismiss(alert) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()   // a second order arrived while this card is open
    }

    private fun dismiss(a: NewOrderAlert) {
        NotificationManagerCompat.from(this).cancel(a.orderId.hashCode())
        finish()
    }

    private fun viewOrder(a: NewOrderAlert) {
        NotificationManagerCompat.from(this).cancel(a.orderId.hashCode())
        val keyguard = getSystemService(KeyguardManager::class.java)
        val go = {
            startActivity(AdminAlerts.openOrderIntent(this, a.orderId))
            finish()
        }
        if (keyguard.isKeyguardLocked) {
            // Asks the user to unlock first, then opens the order.
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = go()
            })
        } else go()
    }
}

@Composable
private fun AlertScreen(a: NewOrderAlert, onView: () -> Unit, onDismiss: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulse",
    )
    LaunchedEffect(Unit) {
        delay(60_000)
        onDismiss()
    }

    PaanBackground(
        background = Brush.verticalGradient(listOf(PaanColors.DeepForest, PaanColors.ForestGreen)),
        leafColor = PaanColors.LightGold.copy(alpha = 0.07f),
        veinColor = PaanColors.LightGold.copy(alpha = 0.10f),
    ) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(112.dp).scale(pulse).background(PaanColors.RoyalGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = PaanColors.DeepForest, modifier = Modifier.size(56.dp))
                }
                Text("NEW ORDER", style = MaterialTheme.typography.labelLarge, color = PaanColors.LightGold)
                Text(a.orderNumber, style = MaterialTheme.typography.displayMedium, color = PaanColors.Cream)
                if (a.token.isNotEmpty()) {
                    Text("TOKEN #${a.token.padStart(2, '0')}", style = PaanTextStyles.TokenCode, color = PaanColors.RoyalGold)
                }
                Text(a.customer, style = MaterialTheme.typography.bodyLarge, color = PaanColors.Cream)
                if (a.summary.isNotBlank()) {
                    Text(
                        a.summary, style = MaterialTheme.typography.bodyMedium, color = PaanColors.LightGold,
                        textAlign = TextAlign.Center, maxLines = 4,
                    )
                }
                Text("₹${a.total} • ${a.itemsCount} items", style = MaterialTheme.typography.titleLarge, color = PaanColors.Cream)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onView,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PaanColors.RoyalGold, contentColor = PaanColors.DarkBrown),
                ) { Text("View order", style = MaterialTheme.typography.titleMedium) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Dismiss", color = PaanColors.Cream)
                }
            }
        }
    }
}