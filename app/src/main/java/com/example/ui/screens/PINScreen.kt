package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ProfileViewModel

@Composable
fun PINScreen(
    viewModel: ProfileViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val masterPin by viewModel.masterPin.collectAsState()
    
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isConfirmingSetup by remember { mutableStateOf(false) }
    var originalSetupPin by remember { mutableStateOf("") }

    val isSetupMode = masterPin == null

    val cardBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textStyle = MaterialTheme.colorScheme.onBackground

    LaunchedEffect(pinText) {
        if (pinText.length == 4) {
            if (isSetupMode) {
                if (!isConfirmingSetup) {
                    originalSetupPin = pinText
                    pinText = ""
                    isConfirmingSetup = true
                    errorMessage = ""
                } else {
                    if (pinText == originalSetupPin) {
                        viewModel.setupMasterPIN(pinText)
                        onAuthSuccess()
                    } else {
                        errorMessage = "PINs do not match. Restarting setup."
                        pinText = ""
                        isConfirmingSetup = false
                        originalSetupPin = ""
                    }
                }
            } else {
                val validated = viewModel.authenticatePIN(pinText)
                if (validated) {
                    onAuthSuccess()
                } else {
                    errorMessage = "Incorrect PIN. Please try again."
                    pinText = ""
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Icon Accent Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Secure",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSetupMode) {
                    if (isConfirmingSetup) "Confirm Security PIN" else "Initialize Secure Locker"
                } else "Locker Authenticating",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isSetupMode) {
                    if (isConfirmingSetup) "Re-enter your 4-digit PIN" else "Establish a 4-digit master passcode to lock and isolate profile launcher and workspace records."
                } else "Input master passcode to unlock isolated profiles workspace launcher.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Enter indicator circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < pinText.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Custom secure numpad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val padRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "Delete")
                )

                for (row in padRows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.5f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key.isEmpty()) return@Box
                                
                                Button(
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                pinText = ""
                                                errorMessage = ""
                                            }
                                            "Delete" -> {
                                                if (pinText.isNotEmpty()) {
                                                    pinText = pinText.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (pinText.length < 4) {
                                                    pinText += key
                                                    errorMessage = ""
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (key == "C" || key == "Delete") {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                        },
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("pin_key_$key")
                                ) {
                                    if (key == "Delete") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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
