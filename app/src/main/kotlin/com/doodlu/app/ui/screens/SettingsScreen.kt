package com.doodlu.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.sync.ConnectionState
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*
import com.doodlu.app.util.buildWallpaperPickerIntent
import com.doodlu.app.util.isDoodluActiveWallpaper
import kotlinx.coroutines.launch

// ── Active wallpaper green ─────────────────────────────────────────────────
private val KawaiiGreen = Color(0xFF6BCB77)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val clipboard     = LocalClipboardManager.current
    val prefs         = remember { PreferencesManager(context) }

    val wallpaperTarget by prefs.wallpaperTarget.collectAsState(initial = "lock")
    val roomId          by prefs.roomId.collectAsState(initial = "")
    val connState       by SyncManager.connectionState.collectAsState()
    val playerCount     by SyncManager.playerCount.collectAsState()
    val isConnected     = connState == ConnectionState.CONNECTED

    // Live check — re-evaluated after picker returns
    var doodluIsWallpaper by remember { mutableStateOf(isDoodluActiveWallpaper(context)) }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        doodluIsWallpaper = isDoodluActiveWallpaper(context)
    }

    var showLeaveDialog by remember { mutableStateOf(false) }

    // ── Leave Room confirmation dialog ────────────────────────────────────
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = KawaiiCard,
            title = {
                Text(
                    "Leave Room? 🥺",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = KawaiiTextPri
                )
            },
            text = {
                Text(
                    "You'll be disconnected and need to create or join a new room.",
                    fontFamily = NunitoFamily,
                    color = KawaiiTextSec
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        SyncManager.disconnect()
                        prefs.clearRoom()
                        showLeaveDialog = false
                        onLeaveRoom()
                    }
                }) {
                    Text(
                        "Leave 💔",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        color = KawaiiRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(
                        "Stay 💕",
                        fontFamily = NunitoFamily,
                        color = KawaiiPink
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kawaiiBgGradient)
    ) {
        FloatingParticles(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Top bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KawaiiCard)
                        .clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null,
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KawaiiTextSec,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    "Settings ⚙️",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = KawaiiTextPri
                )

                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ════════════════════════════════════════════════════════════
            // ① WALLPAPER CARD — at top per spec
            // ════════════════════════════════════════════════════════════
            WallpaperStatusCard(
                isActive       = doodluIsWallpaper,
                wallpaperTarget = wallpaperTarget,
                onSetWallpaper = {
                    val intent = buildWallpaperPickerIntent(context)
                    wallpaperLauncher.launch(intent)
                },
                onTargetChange = { scope.launch { prefs.setWallpaperTarget(it) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ════════════════════════════════════════════════════════════
            // ② ROOM INFO CARD
            // ════════════════════════════════════════════════════════════
            KawaiiCard(modifier = Modifier.fillMaxWidth()) {
                SettingsSectionTitle("Your Room 💬")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Room Code",
                    fontFamily = NunitoFamily,
                    fontSize = 13.sp,
                    color = KawaiiTextSec
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (roomId ?: "------").take(6).forEachIndexed { idx, char ->
                            CodeBubble(char = char, animDelay = idx * 40)
                        }
                    }
                    IconButton(
                        onClick = {
                            roomId?.let { clipboard.setText(AnnotatedString(it)) }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KawaiiCodeBg)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            null,
                            tint = KawaiiPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = KawaiiCodeBorder.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Connection",
                        fontFamily = NunitoFamily,
                        fontSize = 14.sp,
                        color = KawaiiTextSec
                    )
                    KawaiiConnectionBadge(
                        isConnected = isConnected,
                        playerCount = playerCount
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ════════════════════════════════════════════════════════════
            // ③ LEAVE ROOM CARD
            // ════════════════════════════════════════════════════════════
            KawaiiCard(modifier = Modifier.fillMaxWidth()) {
                SettingsSectionTitle("Account 👤")
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .border(2.dp, KawaiiRed.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null
                        ) { showLeaveDialog = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ExitToApp,
                            null,
                            tint = KawaiiRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Leave Room",
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = KawaiiRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Footer ─────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Doodlu v2.1",
                    fontFamily = NunitoFamily,
                    fontSize = 12.sp,
                    color = KawaiiTextSec
                )
                Text(
                    "made with 💕 for people in love",
                    fontFamily = NunitoFamily,
                    fontSize = 11.sp,
                    color = KawaiiPurple
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Wallpaper Status Card
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun WallpaperStatusCard(
    isActive: Boolean,
    wallpaperTarget: String,
    onSetWallpaper: () -> Unit,
    onTargetChange: (String) -> Unit
) {
    KawaiiCard(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionTitle("Wallpaper 🖼️")
        Spacer(modifier = Modifier.height(14.dp))

        if (isActive) {
            // ── Active state: green checkmark badge ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = KawaiiGreen.copy(alpha = 0.18f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(KawaiiGreen.copy(alpha = 0.10f))
                    .border(1.5.dp, KawaiiGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Big green checkmark circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(KawaiiGreen.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = KawaiiGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Doodlu is your wallpaper",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = KawaiiGreen
                    )
                    Text(
                        "Your doodles show up on your lock screen in real time 💕",
                        fontFamily = NunitoFamily,
                        fontSize = 12.sp,
                        color = KawaiiGreen.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // ── Inactive state: prompt to set wallpaper ───────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Doodlu isn't your wallpaper yet",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = KawaiiTextPri,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Set it now so your partner's doodles appear on your lock screen!",
                    fontFamily = NunitoFamily,
                    fontSize = 12.sp,
                    color = KawaiiTextSec,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                KawaiiPrimaryButton(
                    text = "Set as Wallpaper",
                    emoji = "🎨",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSetWallpaper
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = KawaiiCodeBorder.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(14.dp))

        // ── Target selection (informational chips) ────────────────────
        Text(
            "Wallpaper Screen",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = KawaiiTextPri
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KawaiiChip(
                label = "Lock Screen \uD83D\uDD12",
                selected = wallpaperTarget == "lock",
                onClick = { onTargetChange("lock") }
            )
            KawaiiChip(
                label = "Home + Lock \uD83C\uDFE0",
                selected = wallpaperTarget == "both",
                onClick = { onTargetChange("both") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Informational note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KawaiiCodeBg)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "\u2139\uFE0F",
                fontSize = 13.sp
            )
            Text(
                "Android controls this choice in the wallpaper picker dialog. You can also change it any time in your phone's wallpaper settings.",
                fontFamily = NunitoFamily,
                fontSize = 12.sp,
                color = KawaiiTextSec,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Kawaii Switch ──────────────────────────────────────────────────────────
@Composable
fun KawaiiSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = KawaiiPink,
            checkedBorderColor = KawaiiPink,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = KawaiiCodeBorder.copy(alpha = 0.5f),
            uncheckedBorderColor = KawaiiCodeBorder
        )
    )
}

// ── Kawaii chip (filter) ───────────────────────────────────────────────────
@Composable
fun KawaiiChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) KawaiiPink else KawaiiCodeBg)
            .border(
                width = 1.5.dp,
                color = if (selected) KawaiiPink else KawaiiCodeBorder,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = if (selected) Color.White else KawaiiTextSec
        )
    }
}

// ── Settings section title ─────────────────────────────────────────────────
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        title,
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = KawaiiPink
    )
}
