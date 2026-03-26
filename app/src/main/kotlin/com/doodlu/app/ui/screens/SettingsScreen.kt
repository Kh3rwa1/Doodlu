package com.doodlu.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

    var showLeaveDialog  by remember { mutableStateOf(false) }
    var showRevokeDialog by remember { mutableStateOf(false) }

    // ── Revoke Partner confirmation dialog ──────────────────────────────────
    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = KawaiiCard,
            title = {
                Text(
                    "Remove Partner? 💔",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = KawaiiTextPri
                )
            },
            text = {
                Text(
                    "This will disconnect your partner from the room. They will need to re-join to draw together again.",
                    fontFamily = NunitoFamily,
                    color = KawaiiTextSec
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SyncManager.sendKickUser()
                    showRevokeDialog = false
                }) {
                    Text(
                        "Remove 💔",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        color = KawaiiRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = false }) {
                    Text(
                        "Cancel",
                        fontFamily = NunitoFamily,
                        color = KawaiiPink
                    )
                }
            }
        )
    }

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
                    fontSize = 22.sp,
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KawaiiTextSec,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    "Settings ⚙️",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
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
                    fontSize = 14.sp,
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
                        fontSize = 16.sp,
                        color = KawaiiTextSec
                    )
                    KawaiiConnectionBadge(
                        isConnected = isConnected,
                        playerCount = playerCount
                    )
                }

                // ── Revoke partner — only when someone else is in the room ─
                if (isConnected && playerCount >= 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = KawaiiCodeBorder.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KawaiiRed.copy(alpha = 0.06f))
                            .border(1.5.dp, KawaiiRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember {
                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                },
                                indication = null
                            ) { showRevokeDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.PersonRemove,
                                contentDescription = null,
                                tint = KawaiiRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Revoke Partner",
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = KawaiiRed
                                )
                                Text(
                                    "Remove them from your room",
                                    fontFamily = NunitoFamily,
                                    fontSize = 12.sp,
                                    color = KawaiiRed.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
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
                            Icons.AutoMirrored.Filled.ExitToApp,
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
                    fontSize = 14.sp,
                    color = KawaiiTextSec
                )
                Text(
                    "made with 💕 for people in love",
                    fontFamily = NunitoFamily,
                    fontSize = 13.sp,
                    color = KawaiiPurple
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Wallpaper Status Card — Premium
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun WallpaperStatusCard(
    isActive: Boolean,
    wallpaperTarget: String,
    onSetWallpaper: () -> Unit,
    onTargetChange: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wp_card")

    KawaiiCard(modifier = Modifier.fillMaxWidth()) {

        // ── Gradient header pill ─────────────────────────────────────────
        val headerGlow by infiniteTransition.animateFloat(
            initialValue = 0.35f, targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ), label = "hg"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                KawaiiPink.copy(alpha = headerGlow),
                                KawaiiPurple.copy(alpha = headerGlow)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    "Wallpaper 🖼️",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isActive) {
            // ── Active: pulsing green status with animated icon ───────
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "ps"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.10f, targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "ga"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = KawaiiGreen.copy(alpha = 0.15f),
                        spotColor = KawaiiGreen.copy(alpha = 0.10f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                KawaiiGreen.copy(alpha = glowAlpha),
                                KawaiiGreen.copy(alpha = glowAlpha * 0.6f)
                            )
                        )
                    )
                    .border(1.5.dp, KawaiiGreen.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Animated checkmark circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    KawaiiGreen.copy(alpha = 0.25f),
                                    KawaiiGreen.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = KawaiiGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Doodlu is live! ✨",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = KawaiiGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Doodles show up on your screen in real time 💕",
                        fontFamily = NunitoFamily,
                        fontSize = 13.sp,
                        color = KawaiiGreen.copy(alpha = 0.75f),
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            // ── Inactive: premium prompt with animated phone icon ─────
            val phoneFloat by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "pf"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Floating phone emoji with glow
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationY = phoneFloat }
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    KawaiiPink.copy(alpha = 0.12f),
                                    KawaiiPurple.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📱", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Not set as wallpaper yet",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = KawaiiTextPri,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Set it now so your partner's doodles appear on your screen! ✨",
                    fontFamily = NunitoFamily,
                    fontSize = 14.sp,
                    color = KawaiiTextSec,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                KawaiiPrimaryButton(
                    text = "Set as Wallpaper",
                    emoji = "🎨",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSetWallpaper
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = KawaiiCodeBorder.copy(alpha = 0.25f))
        Spacer(modifier = Modifier.height(16.dp))

        // ── Target selection header ──────────────────────────────────────
        Text(
            "Where to show 🎯",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = KawaiiTextPri
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumWallpaperChip(
                label = "Lock Screen 🔒",
                selected = wallpaperTarget == "lock",
                onClick = { onTargetChange("lock") }
            )
            PremiumWallpaperChip(
                label = "Home + Lock 🏠",
                selected = wallpaperTarget == "both",
                onClick = { onTargetChange("both") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Informational note — upgraded
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            KawaiiCodeBg,
                            KawaiiCodeBg.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(1.dp, KawaiiCodeBorder.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("💡", fontSize = 13.sp)
            Text(
                "Android controls this in the wallpaper picker. You can change it any time in your phone's settings.",
                fontFamily = NunitoFamily,
                fontSize = 12.sp,
                color = KawaiiTextSec,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Premium wallpaper chip with gradient when selected ─────────────────────
@Composable
private fun PremiumWallpaperChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) KawaiiPink else KawaiiCodeBg,
        animationSpec = tween(300),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) KawaiiPink else KawaiiCodeBorder,
        animationSpec = tween(300),
        label = "chip_border"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .then(
                if (selected) Modifier.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(50.dp),
                    spotColor = KawaiiPink.copy(alpha = 0.25f)
                ) else Modifier
            )
            .background(
                if (selected) Brush.horizontalGradient(
                    listOf(KawaiiPink, KawaiiCoral)
                ) else Brush.horizontalGradient(
                    listOf(bgColor, bgColor)
                )
            )
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = NunitoFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (selected) Color.White else KawaiiTextSec
        )
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
