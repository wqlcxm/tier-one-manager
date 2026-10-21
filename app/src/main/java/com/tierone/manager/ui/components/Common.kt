package com.tierone.manager.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.data.*
import com.tierone.manager.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

val CardShape = RoundedCornerShape(20.dp)
val SmallShape = RoundedCornerShape(12.dp)

class GameFeedback(
    private val soundEnabled: Boolean,
    private val vibrationEnabled: Boolean,
    private val playClick: () -> Unit,
    private val vibrate: () -> Unit
) {
    fun confirm() {
        if (soundEnabled) playClick()
        if (vibrationEnabled) vibrate()
    }
}

@Composable
fun rememberGameFeedback(soundEnabled: Boolean, vibrationEnabled: Boolean): GameFeedback {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    return remember(soundEnabled, vibrationEnabled, view, haptic) {
        GameFeedback(
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            playClick = { view.playSoundEffect(SoundEffectConstants.CLICK) },
            vibrate = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        )
    }
}

fun colorFor(seed: String): Color {
    val palette = listOf(Cyan, Magenta, Gold, Success, Color(0xFF8B7BFF), Color(0xFFFF8A5C), Color(0xFF5CC8FF), Color(0xFFFF6FB5))
    return palette[abs(seed.hashCode()) % palette.size]
}

@Composable
fun NeonBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg, Color(0xFF0A1322), Bg)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Cyan.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.05f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.9f, size.height * 0.05f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Magenta.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.8f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.05f, size.height * 0.8f)
            )
            val step = 54.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(Outline.copy(alpha = 0.13f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Outline.copy(alpha = 0.10f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }
        content()
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    borderColor: Color = Outline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var m = modifier
        .clip(CardShape)
        .background(Brush.linearGradient(listOf(Surface, Bg2)))
        .border(1.dp, borderColor.copy(alpha = 0.75f), CardShape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextDim,
            letterSpacing = 1.4.sp,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelSmall,
                color = Cyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun Bar(points: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Outline.copy(alpha = 0.5f))
    )
}

@Composable
fun StatBar(label: String, value: Int, color: Color, modifier: Modifier = Modifier, max: Int = 100) {
    val pct by animateFloatAsState((value.toFloat() / max).coerceIn(0f, 1f), tween(450), label = "bar")
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextDim, modifier = Modifier.width(82.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Outline.copy(alpha = 0.45f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)))
            )
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (value >= 65) color else TextMain,
            modifier = Modifier.width(30.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun PlayerAvatar(nick: String, id: String, size: Dp = 46.dp, ovr: Int? = null, role: Role? = null) {
    val color = colorFor(id + nick)
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.25f))))
                .border(1.dp, color.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                nick.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF04121A),
                fontWeight = FontWeight.Black
            )
        }
        if (ovr != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Bg)
                    .border(1.dp, color, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(ovr.toString(), color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TeamLogo(name: String, seed: String, size: Dp = 44.dp) {
    val color = colorFor(seed)
    val second = colorFor("$seed-logo")
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(color, second.copy(alpha = 0.65f))))
            .border(1.dp, TextMain.copy(alpha = 0.18f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase(),
            color = Bg,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.29f).sp
        )
    }
}

@Composable
fun OrgHeader(state: GameState, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(state.org.name, state.org.name + state.org.logoSeed, 44.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.org.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Сезон ${state.season} · Неделя ${state.week}/7 · ${state.org.region}",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AttachMoney, null, tint = Success, modifier = Modifier.size(15.dp))
                Text(state.money.moneyShort(), style = MaterialTheme.typography.titleSmall, color = Success)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.People, null, tint = Magenta, modifier = Modifier.size(13.dp))
                Text(" ${state.org.fans.fansShort()}", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Rounded.ArrowBack, "Назад", tint = TextMain)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextMain)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
    }
}

@Composable
fun DisciplineTabs(selected: Discipline, onChange: (Discipline) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Discipline.entries.forEach { d ->
            val active = d == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Cyan.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (active) Cyan.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable { onChange(d) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Cyan else TextDim
                )
            }
        }
    }
}

@Composable
fun FormDots(results: List<Boolean>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (results.isEmpty()) {
            Text("—", color = TextDim, style = MaterialTheme.typography.labelSmall)
        }
        results.takeLast(8).forEach { win ->
            Box(
                Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (win) Success.copy(alpha = 0.2f) else Danger.copy(alpha = 0.2f))
                    .border(1.dp, if (win) Success.copy(alpha = 0.6f) else Danger.copy(alpha = 0.6f), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (win) "В" else "П",
                    color = if (win) Success else Danger,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RadarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier, color: Color = Cyan) {
    val density = LocalDensity.current
    Canvas(modifier.fillMaxWidth().height(205.dp)) {
        if (values.isEmpty()) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx())
        val radius = min(size.width, size.height) * 0.35f
        val n = values.size
        fun point(i: Int, r: Float): Offset {
            val angle = -PI / 2 + 2 * PI * i / n
            return Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
        }
        for (ring in 1..4) {
            val path = Path()
            for (i in 0 until n) {
                val p = point(i, radius * ring / 4f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, Outline.copy(alpha = if (ring == 4) 0.7f else 0.35f), style = Stroke(1f))
        }
        for (i in 0 until n) {
            drawLine(Outline.copy(alpha = 0.4f), center, point(i, radius), 1f)
        }
        val dataPath = Path()
        for (i in 0 until n) {
            val p = point(i, radius * (values[i].coerceIn(0, 100) / 100f))
            if (i == 0) dataPath.moveTo(p.x, p.y) else dataPath.lineTo(p.x, p.y)
        }
        dataPath.close()
        drawPath(dataPath, color.copy(alpha = 0.20f))
        drawPath(dataPath, color, style = Stroke(2.2.dp.toPx()))
        for (i in 0 until n) {
            val p = point(i, radius * (values[i].coerceIn(0, 100) / 100f))
            drawCircle(color, 3.2.dp.toPx(), p)
        }
        val paint = android.graphics.Paint().apply {
            this.color = TextDim.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        labels.forEachIndexed { i, label ->
            val p = point(i, radius + 17.dp.toPx())
            val adjustedY = if (p.y < center.y) p.y - 3.dp.toPx() else p.y + 9.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(label, p.x, adjustedY, paint)
        }
    }
}

@Composable
fun Confetti(active: Boolean, modifier: Modifier = Modifier) {
    val particles = remember {
        List(70) {
            ConfettiParticle(
                x = Random.nextFloat(),
                offset = Random.nextFloat(),
                speed = 0.6f + Random.nextFloat() * 0.8f,
                color = listOf(Cyan, Magenta, Gold, Success, TextMain).random(),
                size = 3f + Random.nextFloat() * 5f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2600, easing = LinearEasing))
        }
    }
    Canvas(modifier.fillMaxSize()) {
        if (!active) return@Canvas
        particles.forEach { p ->
            val y = ((p.offset + progress.value * p.speed) % 1.1f) * size.height
            val x = (p.x * size.width + sin((progress.value + p.offset) * 9f) * 14.dp.toPx())
            drawRect(
                color = p.color.copy(alpha = (1f - progress.value * 0.5f).coerceIn(0f, 1f)),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.8f)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val offset: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val rotation: Float
)

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextMain)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextDim, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
