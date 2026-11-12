package com.paan.safaadmin.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paan.safaadmin.ui.components.GoldDivider
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.PaanLogo
import com.paan.safaadmin.ui.components.PrimaryPaanButton
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PaanColors.ForestGreen,
        focusedLabelColor = PaanColors.ForestGreen,
        cursorColor = PaanColors.ForestGreen,
        unfocusedContainerColor = PaanColors.CardWhite,
        focusedContainerColor = PaanColors.CardWhite,
    )

    PaanBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PaanLogo(logoSize = 96.dp)
            Spacer(Modifier.height(Dimens.SpaceMd))
            Text("Safar Paan Admin", style = MaterialTheme.typography.displayMedium, color = PaanColors.DeepForest)
            Spacer(Modifier.height(Dimens.SpaceSm))
            GoldDivider()
            Spacer(Modifier.height(Dimens.SpaceMd))
            Text(
                "Staff sign-in",
                style = MaterialTheme.typography.bodyMedium,
                color = PaanColors.MutedBrown,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.SpaceLg))

            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Mobile number") },
                prefix = { Text("+91  ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                colors = fieldColors,
            )
            Spacer(Modifier.height(Dimens.SpaceSm))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password") },
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                        )
                    }
                },
                colors = fieldColors,
            )
            Spacer(Modifier.height(Dimens.SpaceMd))
            PrimaryPaanButton(
                text = "Sign in",
                onClick = viewModel::submit,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}