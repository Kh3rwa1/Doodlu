package com.doodlu.app.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

fun generateRoomCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6).map { chars.random() }.joinToString("")
}

@Composable
fun PairingScreen(
    onPaired: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var myCode by remember { mutableStateOf(generateRoomCode()) }
    var theirCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Heart pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1f at 0
                1.25f at 200
                1f at 400
                1.1f at 600
                1f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo + Heart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Doodlu",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DoodluTextPrimary
                )
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Heart",
                    tint = DoodluPrimary,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(heartScale)
                )
            }

            Text(
                text = "Your screen, their heart.",
                color = DoodluTextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Your code card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DoodluSurface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Room Code",
                    color = DoodluTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Code display
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DoodluSurfaceVariant)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    myCode.forEach { char ->
                        Text(
                            text = char.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DoodluPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy button
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(myCode))
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DoodluTextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DoodluSurfaceVariant)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 13.sp)
                    }

                    // Share button
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join me on Doodlu! Enter code: $myCode — Download: https://play.google.com/store/apps/details?id=com.doodlu.app"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share with your person"))
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoodluPrimary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share with your person", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "— OR —",
                color = DoodluTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enter their code
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DoodluSurface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join their room",
                    color = DoodluTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = theirCode,
                    onValueChange = {
                        theirCode = it.uppercase().take(6)
                        errorMessage = ""
                    },
                    placeholder = {
                        Text(
                            "Enter 6-char code",
                            color = DoodluTextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            focusManager.clearFocus()
                            if (theirCode.length == 6) {
                                scope.launch {
                                    joinRoom(theirCode, prefsManager, onPaired)
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DoodluPrimary,
                        unfocusedBorderColor = DoodluSurfaceVariant,
                        focusedTextColor = DoodluTextPrimary,
                        unfocusedTextColor = DoodluTextPrimary,
                        cursorColor = DoodluPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = DoodluPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (theirCode.length == 6) {
                            scope.launch {
                                isJoining = true
                                joinRoom(theirCode, prefsManager, onPaired)
                                isJoining = false
                            }
                        } else {
                            errorMessage = "Code must be 6 characters"
                        }
                    },
                    enabled = theirCode.length == 6 && !isJoining,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DoodluSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Join Room", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Or just use own code
            Text(
                text = "Or use your own code to test solo",
                color = DoodluTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        joinRoom(myCode, prefsManager, onPaired)
                    }
                }
            ) {
                Text(
                    text = "Use my code →",
                    color = DoodluPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private suspend fun joinRoom(
    roomCode: String,
    prefsManager: PreferencesManager,
    onPaired: () -> Unit
) {
    val userId = UUID.randomUUID().toString().take(8)
    prefsManager.saveRoom(roomCode, userId, "X") // Symbol assigned by server on init
    SyncManager.connect(roomCode, userId)
    onPaired()
}
