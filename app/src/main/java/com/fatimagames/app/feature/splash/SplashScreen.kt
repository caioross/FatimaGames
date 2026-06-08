package com.fatimagames.app.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatimagames.app.core.theme.LocalAppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    val theme = LocalAppTheme.current

    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val tagAlpha = remember { Animatable(0f) }
    val particlesProgress = remember { Animatable(0f) }
    val finalFade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Particles aparecem desde o início
        particlesProgress.animateTo(1f, animationSpec = tween(durationMillis = 1800, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        // Logo cresce com bounce
        logoAlpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
        logoScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        tagAlpha.animateTo(1f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        finalFade.animateTo(0f, animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing))
        onFinished()
    }

    val particles = remember {
        val rng = Random(42)
        List(28) {
            FloatingPiece(
                xRel = rng.nextFloat(),
                yStart = 1.2f + rng.nextFloat() * 0.5f,
                yEnd = -0.4f - rng.nextFloat() * 0.3f,
                size = 16f + rng.nextFloat() * 28f,
                rotateBase = rng.nextFloat() * 360f,
                rotateSpeed = (rng.nextFloat() - 0.5f) * 540f,
                color = listOf(
                    theme.color.primary,
                    theme.color.accentTerracotta,
                    theme.color.accentGold,
                    theme.color.accentPlum,
                ).random(rng),
                shapeKind = rng.nextInt(3),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.color.bgCanvas)
            .graphicsLayer { alpha = finalFade.value },
    ) {
        // Camada de partículas: peças de puzzle/gemas/folhas voando
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (p in particles) {
                val t = particlesProgress.value
                val y = (p.yStart + (p.yEnd - p.yStart) * t) * size.height
                val x = p.xRel * size.width + sin((t + p.rotateBase / 360f) * PI.toFloat() * 2f) * size.width * 0.05f
                val rot = p.rotateBase + p.rotateSpeed * t
                drawSplashShape(
                    kind = p.shapeKind,
                    cx = x,
                    cy = y,
                    size = p.size,
                    rotationDeg = rot,
                    color = p.color.copy(alpha = (1f - kotlin.math.abs(t - 0.5f) * 1.6f).coerceIn(0.2f, 0.85f)),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Símbolo
            Box(
                modifier = Modifier
                    .height(112.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val r = h * 0.42f
                    // Anel externo dourado
                    drawCircle(theme.color.accentGold, radius = r * 1.05f, center = Offset(cx, cy))
                    // Disco primary
                    drawCircle(theme.color.primary, radius = r, center = Offset(cx, cy))
                    // Letra F estilizada (caligráfica simplificada)
                    drawSplashF(cx = cx, cy = cy, scale = r * 0.7f, color = theme.color.bgCanvas)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Fatima Games",
                color = theme.color.textPrimary,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.W600,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.graphicsLayer { alpha = tagAlpha.value },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Jogos clássicos · com carinho",
                color = theme.color.textSecondary,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.graphicsLayer { alpha = tagAlpha.value },
            )
        }
    }
}

private data class FloatingPiece(
    val xRel: Float,
    val yStart: Float,
    val yEnd: Float,
    val size: Float,
    val rotateBase: Float,
    val rotateSpeed: Float,
    val color: Color,
    val shapeKind: Int,
)

private fun DrawScope.drawSplashShape(
    kind: Int,
    cx: Float,
    cy: Float,
    size: Float,
    rotationDeg: Float,
    color: Color,
) {
    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
        when (kind) {
            0 -> {
                // Quadrado arredondado (puzzle piece)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - size / 2f, cy - size / 2f),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(size * 0.2f, size * 0.2f),
                )
            }
            1 -> {
                // Círculo (gema)
                drawCircle(color, radius = size / 2f, center = Offset(cx, cy))
            }
            2 -> {
                // Losango (gem alt)
                val path = Path().apply {
                    moveTo(cx, cy - size / 2f)
                    lineTo(cx + size / 2f, cy)
                    lineTo(cx, cy + size / 2f)
                    lineTo(cx - size / 2f, cy)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}

private fun DrawScope.drawSplashF(
    cx: Float,
    cy: Float,
    scale: Float,
    color: Color,
) {
    val path = Path().apply {
        // F estilizado: barra vertical + barra superior + meio
        // Coords relativas ao centro
        val w = scale * 0.7f
        val h = scale
        moveTo(cx - w / 2f, cy - h)
        lineTo(cx + w / 2f, cy - h)
        lineTo(cx + w / 2f, cy - h * 0.78f)
        lineTo(cx - w / 4f, cy - h * 0.78f)
        lineTo(cx - w / 4f, cy - h * 0.20f)
        lineTo(cx + w / 4f, cy - h * 0.20f)
        lineTo(cx + w / 4f, cy + h * 0.05f)
        lineTo(cx - w / 4f, cy + h * 0.05f)
        lineTo(cx - w / 4f, cy + h)
        lineTo(cx - w / 2f, cy + h)
        close()
    }
    drawPath(path, color)
}
