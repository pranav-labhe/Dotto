package com.pranav.dotto.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.presentation.theme.*

/**
 * Renders the current [GameState.board] on a Canvas with animations,
 * glowing dots, and dotted idle lines for a modern vibrant style.
 */
@Composable
fun DottoBoard(
    gameState: GameState,
    recentlyCompletedBoxes: Set<BoxCoordinate>,
    lastMoveLine: Line? = null,
    enabled: Boolean,
    onLineTapped: (Line) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = gameState.board.config
    val aspect = (config.dotColumns - 1).toFloat().coerceAtLeast(1f) /
            (config.dotRows - 1).toFloat().coerceAtLeast(1f)

    val playerColor: (PlayerId) -> Color = remember(gameState.players) {
        { id ->
            val player = gameState.players.firstOrNull { it.id == id }
            player?.let { PlayerPresentation.colorFor(it.colorToken) } ?: DottoDotColor
        }
    }

    // Animation for recently completed boxes
    val boxAlpha by animateFloatAsState(
        targetValue = if (recentlyCompletedBoxes.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "box-completion"
    )

    // Pulsing animation for dots
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

    // Animation for the last drawn line
    val lineDrawProgress = remember(lastMoveLine, gameState.moveNumber) { Animatable(0f) }
    LaunchedEffect(lastMoveLine, gameState.moveNumber) {
        if (lastMoveLine != null) {
            lineDrawProgress.snapTo(0f)
            lineDrawProgress.animateTo(1f, animationSpec = tween(300, easing = LinearOutSlowInEasing))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect.coerceIn(0.4f, 2.5f))
            .pointerInput(enabled, config, gameState.moveNumber) {
                if (!enabled) return@pointerInput
                detectTapGestures { tapOffset ->
                    val mapper = BoardGeometryMapper(
                        config = config,
                        canvasWidthPx = size.width.toFloat(),
                        canvasHeightPx = size.height.toFloat(),
                        paddingPx = PADDING_DP.dp.toPx()
                    )
                    mapper.hitTestLine(tapOffset.x, tapOffset.y)?.let { line ->
                        if (!gameState.board.isLineDrawn(line)) {
                            onLineTapped(line)
                        }
                    }
                }
            }
    ) {
        val mapper = BoardGeometryMapper(
            config = config,
            canvasWidthPx = size.width,
            canvasHeightPx = size.height,
            paddingPx = PADDING_DP.dp.toPx()
        )

        // Boxes (drawn first)
        BoardGeometry.allBoxes(config).forEach { box ->
            val ownerId = gameState.board.boxOwners[box]
            if (ownerId != null) {
                val isRecent = box in recentlyCompletedBoxes
                val alpha = if (isRecent) 0.35f * boxAlpha else 0.18f
                drawBox(mapper, box, playerColor(ownerId), alpha = alpha)
            }
        }

        // Idle (undrawn) lines - DOTTED
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        BoardGeometry.allLines(config).forEach { line ->
            if (!gameState.board.isLineDrawn(line)) {
                drawLineShape(mapper, line, DottoLineIdle, strokeWidthPx = 4f, pathEffect = dashEffect)
            }
        }

        // Drawn lines
        gameState.board.drawnLines.forEach { line ->
            val progress = if (line == lastMoveLine) lineDrawProgress.value else 1f
            drawLineShape(mapper, line, DottoOnBackground, strokeWidthPx = 10f, progress = progress)
        }

        // Dots with glow
        for (row in 0 until config.dotRows) {
            for (col in 0 until config.dotColumns) {
                val centerX = mapper.dotX(col)
                val centerY = mapper.dotY(row)
                
                // Outer glow
                drawCircle(
                    color = DottoDotColor.copy(alpha = dotGlowAlpha),
                    radius = DOT_RADIUS_PX * dotGlowScale,
                    center = Offset(centerX, centerY)
                )
                
                // Main dot
                drawCircle(
                    color = DottoDotColor,
                    radius = DOT_RADIUS_PX,
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Initials
        BoardGeometry.allBoxes(config).forEach { box ->
            val ownerId = gameState.board.boxOwners[box] ?: return@forEach
            val player = gameState.players.firstOrNull { it.id == ownerId } ?: return@forEach
            drawBoxInitial(mapper, box, player.initial, playerColor(ownerId))
        }
    }
}

private const val PADDING_DP = 20
private const val DOT_RADIUS_PX = 9f

private fun DrawScope.drawLineShape(
    mapper: BoardGeometryMapper, 
    line: Line, 
    color: Color, 
    strokeWidthPx: Float,
    pathEffect: PathEffect? = null,
    progress: Float = 1f
) {
    val (start, end) = when (line) {
        is Line.Horizontal -> Offset(mapper.dotX(line.column), mapper.dotY(line.row)) to
                Offset(mapper.dotX(line.column + 1), mapper.dotY(line.row))
        is Line.Vertical -> Offset(mapper.dotX(line.column), mapper.dotY(line.row)) to
                Offset(mapper.dotX(line.column), mapper.dotY(line.row + 1))
    }
    
    val actualEnd = if (progress < 1f) {
        Offset(
            start.x + (end.x - start.x) * progress,
            start.y + (end.y - start.y) * progress
        )
    } else {
        end
    }

    drawLine(
        color = color,
        start = start,
        end = actualEnd,
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round,
        pathEffect = pathEffect
    )
}

private fun DrawScope.drawBox(mapper: BoardGeometryMapper, box: BoxCoordinate, color: Color, alpha: Float) {
    val topLeft = Offset(mapper.dotX(box.column), mapper.dotY(box.row))
    val size = Size(mapper.cellWidth, mapper.cellHeight)
    drawRect(
        color = color.copy(alpha = alpha),
        topLeft = topLeft,
        size = size
    )
}

private fun DrawScope.drawBoxInitial(mapper: BoardGeometryMapper, box: BoxCoordinate, initial: String, playerColor: Color) {
    val centerX = mapper.dotX(box.column) + mapper.cellWidth / 2f
    val centerY = mapper.dotY(box.row) + mapper.cellHeight / 2f
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (playerColor.alpha * 255).toInt(),
                (playerColor.red * 255).toInt(),
                (playerColor.green * 255).toInt(),
                (playerColor.blue * 255).toInt()
            )
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
