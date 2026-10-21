package com.tierone.manager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.data.*
import com.tierone.manager.game.*
import com.tierone.manager.ui.MatchDayState
import com.tierone.manager.ui.components.*
import com.tierone.manager.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MatchDayScreen(
    state: GameState,
    matchday: MatchDayState,
    live: LiveMatch?,
    resultApplied: Boolean,
    onStart: (Tactic, Int, DraftPlan) -> Unit,
    onDecision: (MatchDecision) -> Unit,
    onAnimationDone: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    val matchup = matchday.queue.getOrNull(matchday.index)
    val canExit = live == null || live.phase != LivePhase.LIVE
    val matchLabel = when {
        matchup?.isCup == true -> "КУБОК"
        matchup?.isPlayoff == true -> "ПЛЕЙ-ОФФ"
        else -> "МАТЧ ДНЯ"
    }
    val matchAccent = if (matchup?.isPlayoff == true) Gold else Cyan
    val feedback = rememberGameFeedback(state.settings.sound, state.settings.vibration)
    LaunchedEffect(resultApplied, live?.result?.id) {
        if (resultApplied && live?.result != null) feedback.confirm()
    }
    BackHandler(enabled = true) {
        if (canExit) onExit()
    }
    NeonBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        matchLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = matchAccent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "${matchup?.discipline?.label ?: "Матч"} · неделя ${state.week}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMain
                    )
                }
                if (canExit) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Rounded.Close, "Закрыть", tint = TextDim)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Outline.copy(alpha = 0.5f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (resultApplied) 1f else if (live?.phase == LivePhase.PREGAME) 0.08f else 0.55f)
                        .height(2.dp)
                        .background(matchAccent)
                )
            }
            Spacer(Modifier.height(12.dp))

            when {
                live == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка матча…", color = TextDim)
                }
                live.phase == LivePhase.PREGAME && !resultApplied -> PregameContent(
                    state = state,
                    live = live,
                    matchup = matchup,
                    onStart = { tactic, mapIndex, plan -> feedback.confirm(); onStart(tactic, mapIndex, plan) }
                )
                resultApplied && live.result != null -> ResultContent(
                    state = state,
                    live = live,
                    onNext = onNext
                )
                else -> LiveContent(
                    state = state,
                    live = live,
                    matchKey = "${state.season}-${state.week}-${matchday.index}",
                    onDecision = { decision -> feedback.confirm(); onDecision(decision) },
                    onAnimationDone = onAnimationDone,
                    resultApplied = resultApplied
                )
            }
        }
    }
}

@Composable
private fun PregameContent(
    state: GameState,
    live: LiveMatch,
    matchup: Matchup?,
    onStart: (Tactic, Int, DraftPlan) -> Unit
) {
    val discipline = live.discipline
    var tactic by remember(live.opponentId) { mutableStateOf(state.teamFor(discipline).tactic) }
    var mapIndex by remember(live.opponentId) { mutableIntStateOf(0) }
    var plan by remember(live.opponentId) { mutableStateOf(DraftPlan.BALANCED) }
    val strength = MatchEngine.previewStrength(state, live, tactic, mapIndex, plan)
    val chance = (winChance(strength, live.opponentStrength) * 100).roundToInt()
    val best = Tactic.entries.maxByOrNull { tacticEdge(it, live.opponentTactic) } ?: Tactic.BALANCED
    val analystKnowsPlan = state.staffFor(StaffRole.ANALYST) != null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        NeonCard(Modifier.fillMaxWidth(), borderColor = if (live.isPlayoff) Gold.copy(alpha = 0.4f) else Cyan.copy(alpha = 0.25f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TeamLogo(state.org.name, state.org.name + state.org.logoSeed, 54.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(state.org.name, style = MaterialTheme.typography.titleSmall, color = TextMain, textAlign = TextAlign.Center, maxLines = 2)
                    Text("${strength.roundToInt()}", style = MaterialTheme.typography.headlineMedium, color = Cyan)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VS", style = MaterialTheme.typography.titleLarge, color = TextDim)
                    Spacer(Modifier.height(4.dp))
                    Pill("${chance}%", if (chance >= 50) Success else Danger)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TeamLogo(live.opponentName, live.opponentName, 54.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(live.opponentName, style = MaterialTheme.typography.titleSmall, color = TextMain, textAlign = TextAlign.Center, maxLines = 2)
                    Text("${live.opponentStrength}", style = MaterialTheme.typography.headlineMedium, color = Magenta)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Box(
                    Modifier
                        .weight(chance.toFloat().coerceAtLeast(1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Cyan)
                )
                Spacer(Modifier.width(3.dp))
                Box(
                    Modifier
                        .weight((100 - chance).toFloat().coerceAtLeast(1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Magenta.copy(alpha = 0.7f))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    live.isCup && live.cupRound >= 2 -> "Финал Кубка лиги. Серия до двух побед — цена ошибки максимальна."
                    live.isCup -> "Полуфинал Кубка лиги. Серия до двух побед."
                    live.isPlayoff -> "Серия до двух побед. Ошибки стоят дороже."
                    else -> "Один матч. Победа приносит деньги, фанатов и очки в таблицу."
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        Spacer(Modifier.height(12.dp))
        NeonCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Стиль соперника", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.weight(1f))
                if (live.isCup) Pill("КУБОК", Gold)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(live.opponentTactic.label(discipline), Magenta)
                Spacer(Modifier.width(8.dp))
                Text(live.opponentTactic.hint(discipline), style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
            if (discipline == Discipline.DOTA) {
                Spacer(Modifier.height(7.dp))
                Text(
                    if (analystKnowsPlan) "Аналитик: соперник готовит «${live.opponentPlan.label}»."
                    else "План драфта соперника скрыт. Аналитик в штате раскроет его.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (analystKnowsPlan) Cyan else TextDim
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Мета: «${state.meta.favoredTactic.styleLabel()}» и ${state.meta.favoredAttribute.label}.",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Совет штаба: «${best.label(discipline)}» даст тактическое преимущество.",
                style = MaterialTheme.typography.labelSmall,
                color = Gold
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("ВАША ТАКТИКА", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(7.dp))
        Tactic.entries.forEach { t ->
            val active = t == tactic
            val e = tacticEdge(t, live.opponentTactic)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (active) Cyan.copy(alpha = 0.09f) else Surface)
                    .border(1.dp, if (active) Cyan.copy(alpha = 0.6f) else Outline, RoundedCornerShape(13.dp))
                    .clickable { tactic = t }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.label(discipline), style = MaterialTheme.typography.titleSmall, color = if (active) Cyan else TextMain, modifier = Modifier.weight(1f))
                    when {
                        e > 0 -> Pill("КОНТРИТ", Success)
                        e < 0 -> Pill("СЛАБЕЕ", Danger)
                        else -> Pill("РОВНО", TextDim)
                    }
                }
                Text(t.hint(discipline), style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
        if (discipline == Discipline.CS && live.mapChoices.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("ВЫБОР КАРТЫ", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 1.2.sp)
            Text(
                "Карта с любимым стилем даёт преимущество, чужая — риск.",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(7.dp))
            live.mapChoices.forEachIndexed { index, choice ->
                val active = index == mapIndex
                val synergy = choice.favoredTactic == tactic
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) Cyan.copy(alpha = 0.09f) else Surface)
                        .border(1.dp, if (active) Cyan.copy(alpha = 0.6f) else Outline, RoundedCornerShape(13.dp))
                        .clickable { mapIndex = index }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(choice.name, style = MaterialTheme.typography.titleSmall, color = if (active) Cyan else TextMain)
                        Text(
                            "Любимый стиль карты: ${choice.favoredTactic.styleLabel()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim
                        )
                    }
                    Pill(if (synergy) "СИНЕРГИЯ" else "РИСК", if (synergy) Success else TextDim)
                }
            }
        }
        if (discipline == Discipline.DOTA) {
            Spacer(Modifier.height(12.dp))
            Text("ПЛАН ДРАФТА", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 1.2.sp)
            Text(
                "Ранний темп бьёт лейт, лейт — баланс, баланс — ранний темп.",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(7.dp))
            DraftPlan.entries.forEach { p ->
                val active = p == plan
                val edge = draftEdge(p, live.opponentPlan)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) Cyan.copy(alpha = 0.09f) else Surface)
                        .border(1.dp, if (active) Cyan.copy(alpha = 0.6f) else Outline, RoundedCornerShape(13.dp))
                        .clickable { plan = p }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.label, style = MaterialTheme.typography.titleSmall, color = if (active) Cyan else TextMain, modifier = Modifier.weight(1f))
                        if (analystKnowsPlan) {
                            when {
                                edge > 0 -> Pill("СИЛЬНЕЕ", Success)
                                edge < 0 -> Pill("СЛАБЕЕ", Danger)
                                else -> Pill("РОВНО", TextDim)
                            }
                        }
                    }
                    Text(p.hint, style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = { onStart(tactic, mapIndex, plan) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
        ) {
            Icon(Icons.Rounded.PlayArrow, null)
            Spacer(Modifier.width(6.dp))
            Text("НАЧАТЬ МАТЧ", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Можно закрыть экран и вернуться к тренировкам до начала матча.",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LiveContent(
    state: GameState,
    live: LiveMatch,
    matchKey: String,
    onDecision: (MatchDecision) -> Unit,
    onAnimationDone: () -> Unit,
    resultApplied: Boolean
) {
    var reveal by remember(matchKey, live.discipline, live.opponentId) { mutableIntStateOf(0) }
    var speed by remember(matchKey) { mutableIntStateOf(1) }
    val shown = live.events.take(reveal).lastOrNull()
    val shownUs = shown?.us ?: 0
    val shownThem = shown?.them ?: 0
    val listState = rememberLazyListState()
    LaunchedEffect(live.events.size, live.decisionPending, speed) {
        val baseDelay = if (live.discipline == Discipline.CS) 230L else 300L
        while (reveal < live.events.size) {
            delay((baseDelay / speed).coerceAtLeast(30L))
            reveal = (reveal + 1).coerceAtMost(live.events.size)
        }
    }
    LaunchedEffect(reveal) {
        if (reveal > 0) listState.animateScrollToItem((reveal - 1).coerceAtMost((live.events.size - 1).coerceAtLeast(0)))
    }
    LaunchedEffect(reveal, live.finished, live.events.size) {
        if (live.finished && reveal >= live.events.size && !resultApplied) {
            delay(450)
            onAnimationDone()
        }
    }
    val showDecision = live.decisionPending && reveal >= live.events.size

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamLogo(state.org.name, state.org.name + state.org.logoSeed, 40.dp)
                Text(state.org.name, style = MaterialTheme.typography.labelSmall, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    "$shownUs : $shownThem",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextMain,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (live.discipline == Discipline.CS) "РАУНДЫ" else "УБИЙСТВА",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    letterSpacing = 1.5.sp
                )
                if (live.isPlayoff) {
                    Text("Серия ${live.seriesUs}:${live.seriesThem}", style = MaterialTheme.typography.labelSmall, color = Gold)
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamLogo(live.opponentName, live.opponentName, 40.dp)
                Text(live.opponentName, style = MaterialTheme.typography.labelSmall, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(if (live.discipline == Discipline.CS) "Карта ${live.mapName}" else "Игра ${live.mapNumber}", Cyan)
            Spacer(Modifier.width(6.dp))
            Pill(live.tactic.label(live.discipline), Gold)
            Spacer(Modifier.weight(1f))
            Text(
                if (live.finished) "Завершено" else "В эфире",
                style = MaterialTheme.typography.labelSmall,
                color = if (live.finished) TextDim else Success
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Outline.copy(alpha = 0.5f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((reveal.toFloat() / live.events.size.coerceAtLeast(1)).coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(Cyan)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Скорость ×$speed",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { speed = when (speed) { 1 -> 2; 2 -> 4; else -> 1 } }
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            if (reveal < live.events.size) {
                Text(
                    "ПРОПУСТИТЬ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { reveal = live.events.size }
                        .padding(horizontal = 5.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(live.events.take(reveal)) { _, event ->
                LiveEventRow(event)
            }
        }
    }

    if (showDecision) {
        GameDialog(
            onDismiss = {},
            title = if (live.discipline == Discipline.CS) "Перерыв · $shownUs:$shownThem" else "20-я минута · $shownUs:$shownThem",
            subtitle = "Матч на качелях. Что сказать команде перед решающей частью?"
        ) {
            DecisionOption(
                "Взять тайм-аут",
                "Спокойно разобрать план. +тактика, без потери морали.",
                Cyan
            ) { onDecision(MatchDecision.TIMEOUT) }
            Spacer(Modifier.height(7.dp))
            DecisionOption(
                "Играть как играем",
                "Ничего не менять. Небольшой бонус к морали.",
                Success
            ) { onDecision(MatchDecision.CALM) }
            Spacer(Modifier.height(7.dp))
            DecisionOption(
                "Жёсткий разбор",
                "Максимальный тактический бонус, но удар по морали.",
                Danger
            ) { onDecision(MatchDecision.HARSH) }
        }
    }
}

@Composable
private fun DecisionOption(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
            .padding(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Spacer(Modifier.height(5.dp))
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text("ВЫБРАТЬ", color = color, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LiveEventRow(event: LiveEvent) {
    val color = when (event.kind) {
        LiveEventKind.SCORE -> Success
        LiveEventKind.INFO -> TextDim
        LiveEventKind.OBJECTIVE -> Gold
        LiveEventKind.MAP_END -> Cyan
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (event.kind == LiveEventKind.MAP_END) Cyan.copy(alpha = 0.08f) else Surface.copy(alpha = 0.7f))
            .border(1.dp, if (event.kind == LiveEventKind.MAP_END) Cyan.copy(alpha = 0.3f) else Outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            event.text,
            style = MaterialTheme.typography.bodySmall,
            color = if (event.kind == LiveEventKind.INFO) TextDim else TextMain,
            modifier = Modifier.weight(1f)
        )
        Text("${event.us}:${event.them}", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ResultContent(state: GameState, live: LiveMatch, onNext: () -> Unit) {
    val result = live.result ?: return
    val win = result.win
    val heroColor = if (win) Success else Danger
    Box(Modifier.fillMaxSize()) {
        if (win) Confetti(active = true, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (win) "ПОБЕДА" else "ПОРАЖЕНИЕ",
                style = MaterialTheme.typography.displayLarge,
                color = heroColor,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            Text(
                when {
                    result.isCup -> "КУБОК · ${result.discipline.label} · ${result.opponentName}"
                    result.isPlayoff -> "ПЛЕЙ-ОФФ · ${result.discipline.label} · ${result.opponentName}"
                    else -> "${result.discipline.label} · ${result.opponentName}"
                },
                style = MaterialTheme.typography.titleSmall,
                color = TextDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${result.seriesUs} : ${result.seriesThem}",
                style = MaterialTheme.typography.displayMedium,
                color = TextMain,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                when {
                    result.isCup -> "Кубковая серия до двух побед"
                    result.isPlayoff -> "Серия плей-офф"
                    else -> "Итог матча"
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                result.maps.forEach { map ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Surface)
                            .border(1.dp, if (map.win) Success.copy(alpha = 0.4f) else Outline, RoundedCornerShape(11.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(map.name, style = MaterialTheme.typography.labelSmall, color = TextDim, maxLines = 1)
                        Text(
                            "${map.us}:${map.them}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (map.win) Success else Danger
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MVP матча", style = MaterialTheme.typography.titleSmall, color = TextMain)
                    Spacer(Modifier.weight(1f))
                    Text(result.mvp, style = MaterialTheme.typography.titleMedium, color = Gold)
                    Spacer(Modifier.width(5.dp))
                    Pill(if (result.mvpIsUser) "НАШ" else "СОПЕРНИК", if (result.mvpIsUser) Success else Magenta)
                }
            }
            Spacer(Modifier.height(10.dp))
            NeonCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Text("Статистика состава", style = MaterialTheme.typography.titleSmall, color = TextMain)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("ИГРОК", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.weight(1f))
                    Text("K", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(26.dp))
                    Text("D", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(26.dp))
                    Text("A", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(26.dp))
                    Text("РЕЙТ", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(42.dp))
                }
                result.stats.filter { it.isUser }.sortedByDescending { it.rating }.forEach { stat ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stat.nick,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (stat.nick == result.mvp) Gold else TextMain,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("${stat.kills}", style = MaterialTheme.typography.labelSmall, color = TextMain, modifier = Modifier.width(26.dp))
                        Text("${stat.deaths}", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(26.dp))
                        Text("${stat.assists}", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(26.dp))
                        Text(String.format("%.2f", stat.rating), style = MaterialTheme.typography.labelSmall, color = if (stat.rating >= 1.1) Success else TextMain, modifier = Modifier.width(42.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardBox("Призовые", "+${result.money.moneyShort()}", Success, Modifier.weight(1f))
                RewardBox("Фанаты", "+${result.fans}", Magenta, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (win) Success else Cyan, contentColor = Bg)
            ) {
                Text("ПРОДОЛЖИТЬ", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RewardBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
            .padding(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}
