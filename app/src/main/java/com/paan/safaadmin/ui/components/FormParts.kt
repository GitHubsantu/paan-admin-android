package com.paan.safaadmin.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.paan.safaadmin.ui.theme.PaanColors

@Composable
fun AppField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
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

/** Photo preview + choose/remove. [model] is a Uri, a URL string, or null. Needs no storage permission. */
@Composable
fun PhotoPicker(model: Any?, onPick: (Uri) -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPick(uri)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PaanColors.ForestGreen.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (model != null) {
                AsyncImage(model = model, contentDescription = "Photo preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("No photo", color = PaanColors.MutedBrown)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (model == null) "Choose photo" else "Change photo") }
            if (model != null) TextButton(onClick = onRemove) { Text("Remove", color = PaanColors.Maroon) }
        }
    }
}