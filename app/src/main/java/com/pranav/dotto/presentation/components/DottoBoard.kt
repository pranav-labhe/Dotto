package com.pranav.dotto.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.presentation.theme.*

@Composable
fun DottoBoard(
    gameState: GameState,
    recentlyCompletedBoxes: Set<BoxCoordinate>,
    lastMoveLine: Line? = null,
    enabled: Boolean,
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    isPanningMode: Boolean = false,
    onOffsetChange: (Offset) -> Unit = {},
    onScaleChange: (Float) -> Unit = {},
    onLineTapped: (Line) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = gameState.board.config
    val level = config.dotRows - 2
    val isLarge = level >= 6 // Level 6 (8x8) is now the baseline for viewport locking
    
    val aspect = if (isLarge) 1f else {
        (config.dotColumns - 1).toFloat().coerceAtLeast(1f) /
                (config.dotRows - 1).toFloat().coerceAtLeast(1f)
    }

    val coroutineScope = rememberCoroutineScope()
    val animatedOffset = remember { Animatable(offset, Offset.VectorConverter) }
    
    LaunchedEffect(offset) {
        if (!animatedOffset.isRunning) animatedOffset.snapTo(offset)
    }

    val playerColor: (PlayerId) -> Color = remember(gameState.players) {
        { id ->
            val player = gameState.players.firstOrNull { it.id == id }
            player?.let { PlayerPresentation.colorFor(it.colorToken) } ?: DottoDotColor
        }
    }

    val boxAlpha by animateFloatAsState(
        targetValue = if (recentlyCompletedBoxes.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "box-completion"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "dot-glow")
    val dotGlowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val dotGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val lineDrawProgress = remember(lastMoveLine, gameState.moveNumber) { Animatable(0f) }
    LaunchedEffect(lastMoveLine, gameState.moveNumber) {
        if (lastMoveLine != null) {
            lineDrawProgress.snapTo(0f)
            lineDrawProgress.animateTo(1f, animationSpec = tween(300, easing = LinearOutSlowInEasing))
        }
    }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(aspect.coerceIn(0.4f, 2.5f))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, config, isPanningMode) {
                    if (!enabled) return@pointerInput
                    
                    if (isPanningMode) {
                        coroutineScope {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val mapper = BoardGeometryMapper(config, size.width.toFloat(), size.height.toFloat(), PADDING_DP.dp.toPx(), isLarge)
                                val boardWidth = mapper.dotX(config.dotColumns - 1) + PADDING_DP.dp.toPx()
                                val boardHeight = mapper.dotY(config.dotRows - 1) + PADDING_DP.dp.toPx()
                                
                                val minScaleToFit = minOf(size.width.toFloat() / boardWidth, size.height.toFloat() / boardHeight).coerceAtMost(1f)
                                
                                val oldScale = scale
                                val newScale = (scale * zoom).coerceIn(minScaleToFit, 4f)
                                
                                var nextOffset = animatedOffset.value + pan + (centroid - animatedOffset.value) * (1 - newScale / oldScale)
                                
                                val minX = size.width.toFloat() - (boardWidth * newScale)
                                val nextX = if (minX >= 0) minX / 2f else nextOffset.x.coerceIn(minX, 0f)
                                
                                val minY = size.height.toFloat() - (boardHeight * newScale)
                                val nextY = if (minY >= 0) minY / 2f else nextOffset.y.coerceIn(minY, 0f)
                                
                                nextOffset = Offset(nextX, nextY)
                                
                                launch { animatedOffset.snapTo(nextOffset) }
                                onScaleChange(newScale)
                                onOffsetChange(nextOffset)
                            }
                        }
                    } else {
                        detectTapGestures { tapOffset ->
                            val currentOff = animatedOffset.value
                            val transformedTap = (tapOffset - currentOff) / scale
                            val mapper = BoardGeometryMapper(
                                config = config,
                                canvasWidthPx = size.width.toFloat(),
                                canvasHeightPx = size.height.toFloat(),
                                paddingPx = PADDING_DP.dp.toPx(),
                                isLargeLevel = isLarge
                            )
                            mapper.hitTestLine(transformedTap.x, transformedTap.y)?.let { line ->
                                if (!gameState.board.isLineDrawn(line)) {
                                    onLineTapped(line)
                                }
                            }
                        }
                    }
                }
        ) {
            val mapper = BoardGeometryMapper(
                config = config,
                canvasWidthPx = size.width,
                canvasHeightPx = size.height,
                paddingPx = PADDING_DP.dp.toPx(),
                isLargeLevel = isLarge
            )

            clipRect {
                withTransform({
                    translate(animatedOffset.value.x, animatedOffset.value.y)
                    scale(scale, scale, Offset.Zero)
                }) {
                    BoardGeometry.allBoxes(config).forEach { box ->
                        val ownerId = gameState.board.boxOwners[box]
                        if (ownerId != null) {
                            val isRecent = box in recentlyCompletedBoxes
                            val alpha = if (isRecent) 0.35f * boxAlpha else 0.18f
                            drawBox(mapper, box, playerColor(ownerId), alpha = alpha)
                        }
                    }

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    BoardGeometry.allLines(config).forEach { line ->
                        if (!gameState.board.isLineDrawn(line)) {
                            drawLineShape(mapper, line, DottoLineIdle, strokeWidthPx = 4f, pathEffect = dashEffect)
                        }
                    }

                    gameState.board.lineOwners.forEach { (line, ownerId) ->
                        val progress = if (line == lastMoveLine) lineDrawProgress.value else 1f
                        drawLineShape(mapper, line, playerColor(ownerId), strokeWidthPx = 10f, progress = progress)
                    }

                    for (row in 0 until config.dotRows) {
                        for (col in 0 until config.dotColumns) {
                            val centerX = mapper.dotX(col)
                            val centerY = mapper.dotY(row)
                            drawCircle(color = Color.White.copy(alpha = dotGlowAlpha * 0.3f), radius = DOT_RADIUS_PX * dotGlowScale * 1.5f, center = Offset(centerX, centerY))
                            drawCircle(color = Color.White, radius = DOT_RADIUS_PX, center = Offset(centerX, centerY))
                        }
                    }

                    BoardGeometry.allBoxes(config).forEach { box ->
                        val ownerId = gameState.board.boxOwners[box] ?: return@forEach
                        val player = gameState.players.firstOrNull { it.id == ownerId } ?: return@forEach
                        drawBoxInitial(mapper, box, player.initial, playerColor(ownerId))
                    }
                }
            }
        }
    }
}

private const val PADDING_DP = 20
private const val DOT_RADIUS_PX = 9f

private fun DrawScope.drawLineShape(mapper: BoardGeometryMapper, line: Line, color: Color, strokeWidthPx: Float, pathEffect: PathEffect? = null, progress: Float = 1f) {
    val (start, end) = when (line) {
        is Line.Horizontal -> Offset(mapper.dotX(line.column), mapper.dotY(line.row)) to Offset(mapper.dotX(line.column + 1), mapper.dotY(line.row))
        is Line.Vertical -> Offset(mapper.dotX(line.column), mapper.dotY(line.row)) to Offset(mapper.dotX(line.column), mapper.dotY(line.row + 1))
    }
    val actualEnd = if (progress < 1f) Offset(start.x + (end.x - start.x) * progress, start.y + (end.y - start.y) * progress) else end
    drawLine(color = color, start = start, end = actualEnd, strokeWidth = strokeWidthPx, cap = StrokeCap.Round, pathEffect = pathEffect)
}

private fun DrawScope.drawBox(mapper: BoardGeometryMapper, box: BoxCoordinate, color: Color, alpha: Float) {
    val topLeft = Offset(mapper.dotX(box.column), mapper.dotY(box.row))
    val size = Size(mapper.cellWidth, mapper.cellHeight)
    drawRect(color = color.copy(alpha = alpha), topLeft = topLeft, size = size)
}

private fun DrawScope.drawBoxInitial(mapper: BoardGeometryMapper, box: BoxCoordinate, initial: String, playerColor: Color) {
    val centerX = mapper.dotX(box.column) + mapper.cellWidth / 2f
    val centerY = mapper.dotY(box.row) + mapper.cellHeight / 2f
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb((playerColor.alpha * 255).toInt(), (playerColor.red * 255).toInt(), (playerColor.green * 255).toInt(), (playerColor.blue * 255).toInt())
            textSize = (minOf(mapper.cellWidth, mapper.cellHeight) * 0.42f)
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        val fontMetrics = paint.fontMetrics
        val textOffsetY = -(fontMetrics.ascent + fontMetrics.descent) / 2f
        drawText(initial, centerX, centerY + textOffsetY, paint)
    }
}
