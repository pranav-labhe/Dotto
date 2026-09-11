package com.pranav.dotto.presentation.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.application.transport.MoveTransport
import com.pranav.dotto.presentation.components.StarField
import com.pranav.dotto.presentation.theme.*

@Composable
fun PvPSetup(
    config: SetupConfig,
    isHost: Boolean,
    connectionState: MoveTransport.ConnectionState,
    onEnterGame: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DottoBackground)
            .safeDrawingPadding()
    ) {
        StarField(starCount = 150)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHost) "CHALLENGER (HOST)" else "JOIN OPPONENT",
                    style = MaterialTheme.typography.titleLarge,
                    color = DottoPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Connection Status Box
            Surface(
                color = DottoSurface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DottoPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val icon = when (connectionState) {
                        is MoveTransport.ConnectionState.Connected -> Icons.Default.WifiTethering
                        is MoveTransport.ConnectionState.Error -> Icons.Default.SensorsOff
                        else -> Icons.Default.Sensors
                    }
                    val iconColor = when (connectionState) {
                        is MoveTransport.ConnectionState.Connected -> DottoSecondary
                        is MoveTransport.ConnectionState.Error -> DottoTertiary
                        else -> DottoPrimary
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(72.dp)
                            .then(
                                if (connectionState !is MoveTransport.ConnectionState.Connected)
                                    Modifier.scale(pulseScale)
                                else Modifier
                            )
                    )

                    val statusText = when (connectionState) {
                        is MoveTransport.ConnectionState.Idle -> "Initializing Nearby..."
                        is MoveTransport.ConnectionState.Advertising -> "Waiting for Opponent to join..."
                        is MoveTransport.ConnectionState.Discovering -> "Searching for Challenger..."
                        is MoveTransport.ConnectionState.Connecting -> "Connecting to Opponent..."
                        is MoveTransport.ConnectionState.Connected -> "Opponent Connected: ${connectionState.remoteName}"
                        is MoveTransport.ConnectionState.Error -> "Connection issue: ${connectionState.message}"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Grid Size: Level ${config.levelNumber} (${config.gridDots}x${config.gridDots})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Action / Enter button
            val isConnected = connectionState is MoveTransport.ConnectionState.Connected
            Button(
                onClick = onEnterGame,
                enabled = isConnected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DottoPrimary,
                    disabledContainerColor = DottoPrimary.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = if (isConnected) "ENTER THE GRID" else "CONNECTING...",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = if (isConnected) DottoBackground else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
