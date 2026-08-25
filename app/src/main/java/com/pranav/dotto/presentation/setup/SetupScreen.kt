package com.pranav.dotto.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.domain.ai.AiDifficulty
import com.pranav.dotto.presentation.theme.DottoPrimary
import com.pranav.dotto.presentation.theme.DottoSecondary

@Composable
fun SetupScreen(
    config: SetupConfig,
    onConfigChange: (SetupConfig) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "DOTTO",
                style = MaterialTheme.typography.headlineLarge,
                color = DottoPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = config.humanName,
                            onValueChange = { onConfigChange(config.copy(humanName = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Enter your name") }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Grid Size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SetupConfig.AVAILABLE_GRID_SIZES.forEach { size ->
                                FilterChip(
                                    selected = config.gridDots == size,
                                    onClick = { onConfigChange(config.copy(gridDots = size)) },
                                    label = { Text("$size × $size") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI Difficulty", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            AiDifficulty.entries.forEach { difficulty ->
                                FilterChip(
                                    selected = config.aiDifficulty == difficulty,
                                    onClick = { onConfigChange(config.copy(aiDifficulty = difficulty)) },
                                    label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sound Effects", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Audible feedback for moves", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = config.soundEnabled,
                            onCheckedChange = { onConfigChange(config.copy(soundEnabled = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Feedback", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Vibrate on score and moves", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = config.hapticEnabled,
                            onCheckedChange = { onConfigChange(config.copy(hapticEnabled = it)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DottoPrimary)
            ) {
                Text("START ADVENTURE", fontWeight = FontWeight.ExtraBold, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
