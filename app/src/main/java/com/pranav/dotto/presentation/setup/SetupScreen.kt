package com.pranav.dotto.presentation.setup

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.domain.ai.AiDifficulty
import com.pranav.dotto.presentation.components.StarField
import com.pranav.dotto.presentation.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    config: SetupConfig,
    onConfigChange: (SetupConfig) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Title Animation
    val infiniteTransition = rememberInfiniteTransition(label = "neon-title")
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(DottoBackground)
                    .safeDrawingPadding() // CRITICAL: Fixes top/bottom padding issues
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Neon Animated Title
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "DOTTO",
                            style = MaterialTheme.typography.headlineLarge,
                            color = DottoPrimary.copy(alpha = 0.3f * titleGlow),
                            modifier = Modifier.offset(y = 2.dp),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "DOTTO",
                            style = MaterialTheme.typography.headlineLarge,
                            color = DottoPrimary,
                            fontWeight = FontWeight.Black
                        )
                    }
        
                    Spacer(modifier = Modifier.height(8.dp))
        
                    // Rule Banner - Sleek & Floating
                    Surface(
                        color = DottoSurface.copy(alpha = 0.6f),
                        shape = CircleShape,
                        border = borderStroke(1.dp, DottoPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DottoPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Close boxes to capture turns and win!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
        
                    // Main Settings Card - Glassmorphism
                    Surface(
                        color = DottoSurface.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(28.dp),
                        border = borderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Name Section
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("PLAYER IDENTITY", style = MaterialTheme.typography.labelLarge, color = DottoPrimary)
                                OutlinedTextField(
                                    value = config.humanName,
                                    onValueChange = { onConfigChange(config.copy(humanName = it)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DottoPrimary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    placeholder = { Text("Enter tag...", color = Color.Gray) }
                                )
                            }
        
                            // Level Section
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CHOOSE YOUR LEVEL", style = MaterialTheme.typography.labelLarge, color = DottoPrimary)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SetupConfig.AVAILABLE_GRID_SIZES.forEachIndexed { index, size ->
                                        LevelBadge(
                                            level = index + 1,
                                            gridSize = size,
                                            selected = config.gridDots == size,
                                            onClick = { onConfigChange(config.copy(gridDots = size)) }
                                        )
                                    }
                                }
                            }
        
                            // Difficulty Section
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("AI INTELLECT", style = MaterialTheme.typography.labelLarge, color = DottoPrimary)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AiDifficulty.entries.forEach { difficulty ->
                                        FilterChip(
                                            selected = config.aiDifficulty == difficulty,
                                            onClick = { onConfigChange(config.copy(aiDifficulty = difficulty)) },
                                            label = { Text(difficulty.name, fontSize = 12.sp) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DottoPrimary,
                                                selectedLabelColor = DottoBackground,
                                                labelColor = Color.White
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = config.aiDifficulty == difficulty,
                                                borderColor = Color.White.copy(alpha = 0.2f),
                                                selectedBorderColor = DottoPrimary
                                            )
                                        )
                                    }
                                }
                            }
        
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        
                            // Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vibe (Sounds)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Switch(
                                    checked = config.soundEnabled,
                                    onCheckedChange = { onConfigChange(config.copy(soundEnabled = it)) },
                                    modifier = Modifier.scale(0.7f),
                                    colors = SwitchDefaults.colors(checkedThumbColor = DottoPrimary)
                                )
                            }
        
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tactile (Haptics)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Switch(
                                    checked = config.hapticEnabled,
                                    onCheckedChange = { onConfigChange(config.copy(hapticEnabled = it)) },
                                    modifier = Modifier.scale(0.7f),
                                    colors = SwitchDefaults.colors(checkedThumbColor = DottoPrimary)
                                )
                            }
                        }
                    }
        
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StarField(starCount = 47)
                    }
        
                    // Action Button 2.0
                    Button(
                        onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DottoPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("ENTER THE GRID", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = DottoBackground)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Orbiting Star Border Effect
        ScreenBorderPulse()
    }
}

@Composable
private fun ScreenBorderPulse() {
    val infiniteTransition = rememberInfiniteTransition(label = "border-orbit")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val perimeter = 2 * (w + h)
        val currentDist = progress * perimeter

        val center = when {
            currentDist < w -> Offset(currentDist, 0f)
            currentDist < w + h -> Offset(w, currentDist - w)
            currentDist < 2 * w + h -> Offset(w - (currentDist - (w + h)), h)
            else -> Offset(0f, h - (currentDist - (2 * w + h)))
        }

        // Trail/Streak
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = center,
                radius = 60f
            ),
            center = center,
            radius = 60f
        )
        
        // The "Star" Core
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = center
        )
        
        // Secondary outer glow
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = 15f,
            center = center
        )
    }
}

@Composable
private fun LevelBadge(
    level: Int,
    gridSize: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) DottoPrimary else Color.White.copy(alpha = 0.1f)
    val bgColor = if (selected) DottoPrimary.copy(alpha = 0.15f) else Color.Transparent
    
    Surface(
        modifier = Modifier
            .width(85.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = borderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LVL $level", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (selected) DottoPrimary else Color.Gray)
            Text("$gridSize × $gridSize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
