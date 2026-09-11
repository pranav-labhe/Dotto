package com.pranav.dotto.presentation.setup

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.domain.ai.AiDifficulty
import com.pranav.dotto.presentation.components.StarField
import com.pranav.dotto.presentation.sound.SoundManager
import com.pranav.dotto.presentation.theme.*
import kotlin.math.atan2

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    config: SetupConfig,
    onConfigChange: (SetupConfig) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
    totalScore: Int = 0,
    highestLevel: Int = 1,
    soundManager: SoundManager? = null
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
    
    val breathingGlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DottoBackground)
            .safeDrawingPadding()
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Neon Animated Title
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "DOTTO",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = Shadow(
                                color = Color(0xFF5DE1E6).copy(alpha = breathingGlow * 0.2f),
                                blurRadius = 0.5f * breathingGlow
                            )
                        ),
                        color = DottoBackground.copy(alpha = breathingGlow * 0.3f),
                        modifier = Modifier.scale(1f + (0.08f * breathingGlow)),
                        fontWeight = FontWeight.Black
                    )
                    
                    Text(
                        text = "DOTTO",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = Shadow(
                                color = DottoBackground.copy(alpha = breathingGlow),
                                blurRadius = 2f
                            )
                        ),
                        color = Color.Transparent,
                        fontWeight = FontWeight.Black
                    )
                    
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

                // Player Stats
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL SCORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(totalScore.toString(), style = MaterialTheme.typography.titleMedium, color = DottoSecondary, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAX LEVEL REACHED", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("LVL $highestLevel", style = MaterialTheme.typography.titleMedium, color = DottoSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                // Rule Banner
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

                // Main Settings Card
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    StarField(
                        modifier = Modifier.matchParentSize().alpha(0.7f),
                        starCount = 280
                    )

                    Surface(
                        color = DottoSurface.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp),
                        border = borderStroke(0.2.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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

                            // Level Section - Mission Map
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ORBITAL MISSION MAP", style = MaterialTheme.typography.labelLarge, color = DottoPrimary)
                                
                                val lazyListState = rememberLazyListState()
                                val rocketPainter = rememberVectorPainter(Icons.Default.RocketLaunch)
                                val density = LocalDensity.current
                                
                                val spacingPx = with(density) { 32.dp.toPx() }
                                val tileWidthPx = with(density) { 75.dp.toPx() }
                                val topYPx = with(density) { 40.dp.toPx() }
                                val bottomYPx = with(density) { 140.dp.toPx() }
                                
                                val totalLevels = 1000
                                val unlockLimit = maxOf(highestLevel, 5)

                                LaunchedEffect(highestLevel) {
                                    val targetColumn = ((config.levelNumber - 1) / 2).coerceAtLeast(0)
                                    lazyListState.scrollToItem(targetColumn)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                ) {
                                    // Background Path & Rocket Canvas (Scroll-Aware)
                                    val infiniteTransition = rememberInfiniteTransition(label = "energy-flow")
                                    val energyPulse by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(2000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "energy"
                                    )
                                    val blazeScale by infiniteTransition.animateFloat(
                                        initialValue = 0.8f,
                                        targetValue = 1.2f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(100, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "blaze"
                                    )

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val firstVisible = lazyListState.firstVisibleItemIndex
                                        val firstOffset = lazyListState.firstVisibleItemScrollOffset
                                        val scrollX = firstVisible * (tileWidthPx + spacingPx) + firstOffset
                                        
                                        withTransform({
                                            translate(-scrollX + with(density) { 40.dp.toPx() }, 0f)
                                        }) {
                                            val path = Path()
                                            val activePath = Path()
                                            val startCol = (firstVisible - 2).coerceAtLeast(0)
                                            val endCol = (firstVisible + 8).coerceAtMost(totalLevels / 2 - 1)
                                            
                                            val selectedLevel = config.levelNumber

                                            for (col in startCol..endCol) {
                                                val lStart = col * 2 + 1
                                                val lEnd = col * 2 + 2
                                                val isEven = col % 2 == 0
                                                val midX = col * (tileWidthPx + spacingPx) + tileWidthPx / 2f
                                                
                                                val jStart = getLevelJitter(lStart)
                                                val jEnd = getLevelJitter(lEnd)
                                                
                                                // Pattern: Even Col (1-T, 2-B), Odd Col (3-B, 4-T)
                                                val pS = Offset(midX + jStart.x.dp.toPx(), (if(isEven) topYPx else bottomYPx) + jStart.y.dp.toPx())
                                                val pE = Offset(midX + jEnd.x.dp.toPx(), (if(isEven) bottomYPx else topYPx) + jEnd.y.dp.toPx())

                                                // 1. Vertical segment within column (1->2 Down, 3->4 Up)
                                                val cp1V = Offset(pS.x + (if (isEven) 50f else -50f), pS.y + (if (isEven) 30f else -30f))
                                                val cp2V = Offset(pE.x + (if (isEven) -50f else 50f), pE.y + (if (isEven) -30f else 30f))
                                                path.moveTo(pS.x, pS.y)
                                                path.cubicTo(cp1V.x, cp1V.y, cp2V.x, cp2V.y, pE.x, pE.y)
                                                
                                                if (lEnd <= selectedLevel) {
                                                    activePath.moveTo(pS.x, pS.y)
                                                    if (lEnd < selectedLevel) {
                                                        activePath.cubicTo(cp1V.x, cp1V.y, cp2V.x, cp2V.y, pE.x, pE.y)
                                                    } else {
                                                        // selectedLevel is levelB, rocket is at t=0.5
                                                        drawPartialCubic(activePath, pS, cp1V, cp2V, pE, 0.5f)
                                                    }
                                                }

                                                // Rocket on vertical segment (Mid-path for visibility)
                                                if (selectedLevel == lEnd) {
                                                    drawRocketWithBlaze(rocketPainter, pS, cp1V, cp2V, pE, t = 0.5f, color = Color(0xFFAD582F), blazeScale = blazeScale)
                                                }

                                                // 2. Horizontal segment to next column (2->3 Bottom-to-Bottom, 4->5 Top-to-Top)
                                                if (col < totalLevels / 2 - 1) {
                                                    val lNext = (col + 1) * 2 + 1
                                                    val nextMidX = (col + 1) * (tileWidthPx + spacingPx) + tileWidthPx / 2f
                                                    val jNext = getLevelJitter(lNext)
                                                    val pNext = Offset(nextMidX + jNext.x.dp.toPx(), (if(isEven) bottomYPx else topYPx) + jNext.y.dp.toPx())
                                                    
                                                    val cp1H = Offset(pE.x + spacingPx * 0.7f, pE.y)
                                                    val cp2H = Offset(pNext.x - spacingPx * 0.7f, pNext.y)
                                                    path.moveTo(pE.x, pE.y)
                                                    path.cubicTo(cp1H.x, cp1H.y, cp2H.x, cp2H.y, pNext.x, pNext.y)
                                                    
                                                    if (lNext <= selectedLevel) {
                                                        activePath.moveTo(pE.x, pE.y)
                                                        if (lNext < selectedLevel) {
                                                            activePath.cubicTo(cp1H.x, cp1H.y, cp2H.x, cp2H.y, pNext.x, pNext.y)
                                                        } else {
                                                            // selectedLevel is nextLevelA, rocket at t=0.5
                                                            drawPartialCubic(activePath, pE, cp1H, cp2H, pNext, 0.5f)
                                                        }
                                                    }

                                                    // Rocket on horizontal segment
                                                    if (selectedLevel == lNext) {
                                                        drawRocketWithBlaze(rocketPainter, pE, cp1H, cp2H, pNext, t = 0.5f, color = Color(0xFFAD582F), blazeScale = blazeScale)
                                                    }
                                                }
                                            }

                                            // Draw Base Path
                                            drawPath(path, color = DottoPrimary.copy(alpha = 0.15f), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                                            drawPath(
                                                path = path,
                                                color = DottoPrimary.copy(alpha = 0.5f),
                                                style = Stroke(
                                                    width = 2.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                                                    cap = StrokeCap.Round
                                                )
                                            )
                                            
                                            // Draw Active Glowing Path
                                            drawPath(
                                                path = activePath,
                                                color = Color.White.copy(alpha = 0.15f),
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            drawPath(
                                                path = activePath,
                                                color = Color(0xFFACA95D).copy(alpha = 0.8f),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(
                                                        floatArrayOf(5f, 25f),
                                                        -energyPulse * 30f
                                                    ),
                                                    cap = StrokeCap.Round
                                                )
                                            )
                                        }
                                    }

                                    LazyRow(
                                        state = lazyListState,
                                        contentPadding = PaddingValues(horizontal = 40.dp),
                                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(totalLevels / 2) { colIndex ->
                                            val isEven = colIndex % 2 == 0
                                            val levelLow = colIndex * 2 + 1 
                                            val levelHigh = colIndex * 2 + 2 
                                            
                                            val lTop = if (isEven) levelLow else levelHigh
                                            val lBottom = if (isEven) levelHigh else levelLow
                                            
                                            val jTop = getLevelJitter(lTop)
                                            val jBottom = getLevelJitter(lBottom)

                                            Box(modifier = Modifier.width(75.dp).fillMaxHeight()) {
                                                CyberLevelTile(
                                                    level = lTop,
                                                    gridSize = lTop + 2,
                                                    selected = config.levelNumber == lTop,
                                                    isLocked = lTop > unlockLimit,
                                                    modifier = Modifier.align(Alignment.TopCenter).offset(jTop.x.dp, (40.dp - 29.dp) + jTop.y.dp), 
                                                    onClick = { 
                                                        soundManager?.playGridSelect(config.soundEnabled)
                                                        onConfigChange(config.copy(gridDots = lTop + 2, levelNumber = lTop)) 
                                                    }
                                                )
                                                CyberLevelTile(
                                                    level = lBottom,
                                                    gridSize = lBottom + 2,
                                                    selected = config.levelNumber == lBottom,
                                                    isLocked = lBottom > unlockLimit,
                                                    modifier = Modifier.align(Alignment.TopCenter).offset(jBottom.x.dp, (140.dp - 29.dp) + jBottom.y.dp),
                                                    onClick = { 
                                                        soundManager?.playGridSelect(config.soundEnabled)
                                                        onConfigChange(config.copy(gridDots = lBottom + 2, levelNumber = lBottom)) 
                                                    }
                                                )
                                            }
                                        }
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
                                            onClick = { 
                                                soundManager?.playUISelect(config.soundEnabled)
                                                onConfigChange(config.copy(aiDifficulty = difficulty)) 
                                            },
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

                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = Dp.Hairline)

                            // Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vibe (Sounds)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Switch(
                                    checked = config.soundEnabled,
                                    onCheckedChange = { 
                                        onConfigChange(config.copy(soundEnabled = it))
                                        soundManager?.playUISelect(it)
                                    },
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
                                    onCheckedChange = { 
                                        soundManager?.playUISelect(config.soundEnabled)
                                        onConfigChange(config.copy(hapticEnabled = it)) 
                                    },
                                    modifier = Modifier.scale(0.7f),
                                    colors = SwitchDefaults.colors(checkedThumbColor = DottoPrimary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fixed Action Button
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(vertical = 8.dp),
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

private fun getLevelJitter(level: Int): Offset {
    val x = ((level * 13) % 24 - 12).toFloat() 
    val y = ((level * 23) % 16 - 8).toFloat()
    return Offset(x, y)
}

private fun DrawScope.drawPartialCubic(path: Path, p0: Offset, cp1: Offset, cp2: Offset, p3: Offset, t: Float) {
    val it = 1 - t
    // Split cubic at t (using de Casteljau)
    val p01 = p0 * it + cp1 * t
    val p12 = cp1 * it + cp2 * t
    val p23 = cp2 * it + p3 * t
    val p012 = p01 * it + p12 * t
    val p123 = p12 * it + p23 * t
    val p0123 = p012 * it + p123 * t
    
    path.cubicTo(p01.x, p01.y, p012.x, p012.y, p0123.x, p0123.y)
}

private fun DrawScope.drawRocketWithBlaze(
    painter: VectorPainter,
    p0: Offset, cp1: Offset, cp2: Offset, p3: Offset,
    t: Float,
    size: Float = 14.dp.toPx(),
    color: Color = Color.White,
    blazeScale: Float = 1f
) {
    val it = 1 - t
    val pos = p0 * (it * it * it) + cp1 * (3 * it * it * t) + cp2 * (3 * it * t * t) + p3 * (t * t * t)
    val tangent = (cp1 - p0) * (3 * it * it) + (cp2 - cp1) * (6 * it * t) + (p3 - cp2) * (3 * t * t)
    val angle = Math.toDegrees(atan2(tangent.y.toDouble(), tangent.x.toDouble())).toFloat()
    
    withTransform({
        translate(pos.x, pos.y)
        rotate(angle + 45f, Offset.Zero) 
        translate(-size / 2f, -size / 2f)
    }) {
        // Engine Blaze
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), Color(0xFFAD582F).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(0f, size), // At back of rocket
                radius = 8.dp.toPx() * blazeScale
            ),
            center = Offset(0f, size),
            radius = 8.dp.toPx() * blazeScale
        )
        
        with(painter) {
            draw(Size(size, size), colorFilter = ColorFilter.tint(color))
        }
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

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = center,
                radius = 60f
            ),
            center = center,
            radius = 60f
        )
        
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = center
        )
        
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = 15f,
            center = center
        )
    }
}

@Composable
private fun CyberLevelTile(
    level: Int,
    gridSize: Int,
    selected: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.15f else 1f, label = "selection-scale")
    val contentAlpha by animateFloatAsState(if (isLocked) 0.3f else 1f, label = "lock-alpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(75.dp)
            .scale(scale)
            .clickable(enabled = !isLocked) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .drawBehind {
                    if (selected) {
                        // 1. Atmospheric Aura (The Glow)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(DottoPrimary.copy(alpha = 0.45f), Color.Transparent),
                                center = center,
                                radius = size.maxDimension / 2f + 12.dp.toPx()
                            ),
                            radius = size.maxDimension / 2f + 12.dp.toPx()
                        )
                        // 2. Fuzzy Rim (Light bleeding from the edge)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(DottoPrimary, Color.Transparent),
                                center = center,
                                radius = size.maxDimension / 2f + 3.dp.toPx()
                            ),
                            radius = size.maxDimension / 2f,
                            style = Stroke(width = 5.dp.toPx())
                        )
                    }
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (selected) {
                            listOf(DottoPrimary.copy(alpha = 0.8f), DottoBackground)
                        } else {
                            listOf(DottoSurface.copy(alpha = 0.6f), DottoBackground.copy(alpha = 0.4f))
                        }
                    )
                )
                .border(
                    width = if (selected) 0.5.dp else 1.dp,
                    brush = Brush.sweepGradient(
                        if (selected) listOf(DottoPrimary, DottoSecondary, DottoPrimary)
                        else listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f))
                    ),
                    shape = CircleShape
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridScale = 0.3f
                val gridWidth = size.width * gridScale
                val gridHeight = size.height * gridScale
                val startX = (size.width - gridWidth) / 2f
                val startY = (size.height - gridHeight) / 2f
                
                val dotRadius = 0.7.dp.toPx()
                val cellW = gridWidth / 4f
                val cellH = gridHeight / 4f
                
                for (r in 0..4) {
                    for (c in 0..4) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f * contentAlpha),
                            radius = dotRadius,
                            center = Offset(startX + c * cellW, startY + r * cellH)
                        )
                    }
                }
            }

            if (isLocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (selected) DottoPrimary else Color.White).copy(alpha = 0.5f)
                    )
                    Text(
                        text = level.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (selected) DottoPrimary else Color.White,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }
            
            if (selected) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                        .drawBehind {
                            val alpha = 0.3f * (1f - (pulseScale - 1f) / 0.12f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(DottoPrimary.copy(alpha = alpha), Color.Transparent),
                                    center = center,
                                    radius = size.maxDimension / 2.4f
                                ),
                                radius = size.maxDimension / 1.2f,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "$gridSize×$gridSize",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) DottoPrimary else Color.White.copy(alpha = contentAlpha),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun borderStroke(width: Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
