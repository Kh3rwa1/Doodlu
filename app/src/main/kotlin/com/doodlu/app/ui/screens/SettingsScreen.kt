package com.doodlu.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.components.ConnectionIndicator
import com.doodlu.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val prefsManager = remember { PreferencesManager(context) }

    val autoWallpaper by prefsManager.autoWallpaper.collectAsState(initial = false)
    val wallpaperTarget by prefsManager.wallpaperTarget.collectAsState(initial = "lock")
    val roomId by prefsManager.roomId.collectAsState(initial = "")
    val connectionState by SyncManager.connectionState.collectAsState()
    val playerCount by SyncManager.playerCount.collectAsState()

    var showLeaveDialog by remember { mutableStateOf(false) }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = DoodluSurface,
            title = { Text("Leave Room?", color = DoodluTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("You'll be disconnected and need to create or join a new room.", color = DoodluTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            SyncManager.disconnect()
                            prefsManager.clearRoom()
                            showLeaveDialog = false
                            onLeaveRoom()
                        }
                    }
                ) {
                    Text("Leave", color = DoodluPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay", color = DoodluTextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = DoodluTextPrimary)
                }
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DoodluTextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Room info card
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Room")

                SettingsRow(
                    label = "Room Code",
                    value = roomId ?: "—"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                roomId?.let { clipboardManager.setText(AnnotatedString(it)) }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, tint = DoodluTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider(color = DoodluSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connection", color = DoodluTextSecondary, fontSize = 14.sp)
                    ConnectionIndicator(state = connectionState, playerCount = playerCount)
                }

                HorizontalDivider(color = DoodluSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Players Online", color = DoodluTextSecondary, fontSize = 14.sp)
                    Text("$playerCount", color = DoodluTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wallpaper settings
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Live Wallpaper")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-set as wallpaper", color = DoodluTextPrimary, fontSize = 14.sp)
                        Text("Set automatically on new drawings", color = DoodluTextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoWallpaper,
                        onCheckedChange = {
                            scope.launch { prefsManager.setAutoWallpaper(it) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DoodluTextPrimary,
                            checkedTrackColor = DoodluPrimary,
                            uncheckedThumbColor = DoodluTextSecondary,
                            uncheckedTrackColor = DoodluSurfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = DoodluSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Wallpaper Target", color = DoodluTextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = wallpaperTarget == "lock",
                            onClick = { scope.launch { prefsManager.setWallpaperTarget("lock") } },
                            label = { Text("Lock Screen") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DoodluPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = DoodluSurfaceVariant,
                                labelColor = DoodluTextSecondary
                            )
                        )
                        FilterChip(
                            selected = wallpaperTarget == "both",
                            onClick = { scope.launch { prefsManager.setWallpaperTarget("both") } },
                            label = { Text("Home + Lock") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DoodluPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = DoodluSurfaceVariant,
                                labelColor = DoodluTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Danger zone
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { showLeaveDialog = true },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DoodluPrimary.copy(alpha = 0.15f),
                        contentColor = DoodluPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Room", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Doodlu v1.0", color = DoodluTextSecondary, fontSize = 12.sp)
                Text("Made with ♥ for couples & besties", color = DoodluTextSecondary, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16213E))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = DoodluPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = DoodluTextSecondary, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = DoodluTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            trailing()
        }
    }
}
