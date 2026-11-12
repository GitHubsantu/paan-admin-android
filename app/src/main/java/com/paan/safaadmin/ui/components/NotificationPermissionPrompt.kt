package com.paan.safaadmin.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.paan.safaadmin.ui.theme.PaanColors

/** Android 13+ asks for notification permission at runtime. Shows a friendly reason first. */
@Composable
fun NotificationPermissionPrompt(message: String) {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        dismissed = true
    }
    if (!granted && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Turn on notifications") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Allow", color = PaanColors.ForestGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text("Not now", color = PaanColors.MutedBrown) }
            },
            containerColor = PaanColors.Cream,
        )
    }
}