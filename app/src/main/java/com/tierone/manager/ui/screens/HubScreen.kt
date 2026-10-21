package com.tierone.manager.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tierone.manager.data.*
import com.tierone.manager.game.Engine
import com.tierone.manager.game.Matchup
import com.tierone.manager.ui.components.*
import com.tierone.manager.ui.theme.*
import kotlin.math.max
import kotlin.math.roundToInt

private enum class HubAction { TRAIN, INDIVIDUAL, REST, SCRIM, MEDIA, SCOUT }

@Composable
fun HubScreen(
    state: GameState,
    onBeginWeek: () -> Unit,
    onTrainTeam: (Discipline, Attribute) -> Unit,
    onTrainPlayer: (String, Attribute) -> Unit,
    onRest: (Discipline) -> Unit,
    onScrim: (Discipline) -> Unit,
    onMedia: () -> Unit,
    onScout: () -> Unit,
    onGoClub: () -> Unit
) {
    var action by remember { mutableStateOf<HubAction?>(null) }
    var confirmWeek by remember { mutableStateOf(false) }
    val matchups = remember(
        state.week,
        state.playedThisWeek,
        state.playedCupMatches,
        state.weekResults,
        state.csCup?.playoffs,
        state.dotaCup?.playoffs,
        state.csLeague.playoffs,
        state.dotaLeague.playoffs
    ) {
        Engine.upcomingMatchups(state)
    }
    val ap = state.actionPoints

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { OrgHeader(state) }

            item {
                SectionHeader("Матчи недели")
                Spacer(Modifier.height(8.dp))
                if (matchups.isEmpty()) {
                    NeonCard(Modifier.fillMaxWidth()) {
                        Text(
                            if (state.playedThisWeek.isEmpty()) "На этой неделе официальных матчей нет"
                            else "Матчи недели сыграны",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextMain
                        )
                        Text(
                            if (state.week >= 6) "Плей-офф идёт без вашего участия. Завершите неделю."
                            else "Можно сосредоточиться на тренировках и трансферах.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim
                        )
                        if (state.weekResults.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            state.weekResults.forEach { result ->
                                ResultRow(result)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchups.forEach { m ->
                            MatchupCard(state, m)
                        }
                    }
                }
            }

            item {
                NeonCard(Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.28f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Действия", style = MaterialTheme.typography.titleMedium, color = TextMain, modifier = Modifier.weight(1f))
                        Pill("$ap / 4 ОД", if (ap > 0) Cyan else TextDim)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (i < ap) Cyan else Outline)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ActionRow(
                        listOf(
                            ActionSpec("Тренировка", "Состав + атрибут", Icons.Rounded.FitnessCenter, HubAction.TRAIN),
                            ActionSpec("Индивидуально", "Один игрок", Icons.Rounded.Person, HubAction.INDIVIDUAL)
                        ), ap, { action = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    ActionRow(
                        listOf(
                            ActionSpec("Отдых", "Восстановление", Icons.Rounded.Bedtime, HubAction.REST),
                            ActionSpec("Скрим", "Бонус к матчу", Icons.Rounded.SportsEsports, HubAction.SCRIM)
                        ), ap, { action = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    ActionRow(
                        listOf(
                            ActionSpec("Медиа", "+фанаты, +деньги", Icons.Rounded.Campaign, HubAction.MEDIA),
                            ActionSpec("Скаутинг", "Обновить рынок", Icons.Rounded.Search, HubAction.SCOUT)
                        ), ap, { action = it }
                    )
                }
            }

            item {
                SectionHeader("Задачи недели")
                Spacer(Modifier.height(8.dp))
                NeonCard(Modifier.fillMaxWidth()) {
                    state.objectives.forEachIndexed { index, obj ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        val progress = state.weekStats.progress(obj.type).coerceAtMost(obj.target)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(obj.text, style = MaterialTheme.typography.bodySmall, color = TextMain, modifier = Modifier.weight(1f))
                            Text("$progress/${obj.target}", style = MaterialTheme.typography.labelSmall, color = if (progress >= obj.target) Success else TextDim)
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Outline.copy(alpha = 0.5f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(progress.toFloat() / obj.target)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (progress >= obj.target) Success else Cyan)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val rewards = buildList {
                            if (obj.rewardMoney > 0) add(obj.rewardMoney.moneyFull())
                            if (obj.rewardFans > 0) add("+${obj.rewardFans} фанатов")
                        }.joinToString(" · ")
                        Text(rewards, style = MaterialTheme.typography.labelSmall, color = Gold.copy(alpha = 0.9f))
                    }
                }
            }

            item {
                NeonCard(Modifier.fillMaxWidth(), onClick = onGoClub) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Success, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Финансы", style = MaterialTheme.typography.titleSmall, color = TextMain, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowForward, null, tint = TextDim, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        FinanceStat("Баланс", state.money.moneyShort(), Success, Modifier.weight(1f))
                        FinanceStat("За неделю", signedMoney(weeklyNet(state)), if (weeklyNet(state) >= 0) Success else Danger, Modifier.weight(1f))
                        FinanceStat("Фанаты", state.org.fans.fansShort(), Magenta, Modifier.weight(1f))
                    }
                    if (state.sponsor == null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Спонсор не подписан — загляните в клуб.", style = MaterialTheme.typography.labelSmall, color = Gold)
                    }
                }
            }

            item { MetaCard(state) }

            item {
                SectionHeader("Новости")
                Spacer(Modifier.height(8.dp))
                NeonCard(Modifier.fillMaxWidth()) {
                    val latest = state.news.takeLast(4).reversed()
                    if (latest.isEmpty()) {
                        Text("Пока тихо.", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                    latest.forEachIndexed { index, line ->
                        if (index > 0) Spacer(Modifier.height(9.dp))
                        Row {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (index == 0) Cyan else Outline)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(line, style = MaterialTheme.typography.bodySmall, color = if (index == 0) TextMain else TextDim)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Bg, Bg)))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    if (ap > 0 && matchups.isNotEmpty()) confirmWeek = true else onBeginWeek()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        matchups.isNotEmpty() && state.weekResults.isNotEmpty() -> "ПРОДОЛЖИТЬ МАТЧИ"
                        matchups.isNotEmpty() -> "ИГРАТЬ НЕДЕЛЮ"
                        else -> "ЗАВЕРШИТЬ НЕДЕЛЮ"
                    },
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                if (matchups.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text("· ${matchups.size} матч(а)", style = MaterialTheme.typography.labelSmall, color = Bg.copy(alpha = 0.65f))
                }
            }
        }
    }

    when (action) {
        HubAction.TRAIN -> TrainTeamDialog(
            onDismiss = { action = null },
            onConfirm = { d, a -> action = null; onTrainTeam(d, a) }
        )
        HubAction.INDIVIDUAL -> IndividualTrainDialog(
            state = state,
            onDismiss = { action = null },
            onConfirm = { id, a -> action = null; onTrainPlayer(id, a) }
        )
        HubAction.REST -> TeamPickDialog(
            title = "Дать отдых",
            text = "Какой состав восстановить? Усталость снизится, мораль вырастет.",
            onDismiss = { action = null },
            onPick = { d -> action = null; onRest(d) }
        )
        HubAction.SCRIM -> TeamPickDialog(
            title = "Сыграть скримы",
            text = "Химия и форма вырастут, появится бонус к следующему официальному матчу.",
            onDismiss = { action = null },
            onPick = { d -> action = null; onScrim(d) }
        )
        HubAction.MEDIA -> AlertDialog(
            onDismissRequest = { action = null },
            title = { Text("Медиа-день") },
            text = { Text("Провести съёмку и общение с фанатами? +фанаты, +деньги, небольшая усталость состава.") },
            confirmButton = {
                TextButton(onClick = { action = null; onMedia() }) { Text("Провести", color = Cyan) }
            },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Отмена", color = TextDim) } }
        )
        HubAction.SCOUT -> AlertDialog(
            onDismissRequest = { action = null },
            title = { Text("Скаутинг") },
            text = { Text("Обновить список кандидатов на трансферном рынке? Старые предложения исчезнут.") },
            confirmButton = {
                TextButton(onClick = { action = null; onScout() }) { Text("Обновить", color = Cyan) }
            },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Отмена", color = TextDim) } }
        )
        null -> Unit
    }

    if (confirmWeek) {
        AlertDialog(
            onDismissRequest = { confirmWeek = false },
            title = { Text("Начать неделю?") },
            text = { Text("У вас осталось ${state.actionPoints} оч. действий. Они сгорят, когда начнутся матчи.") },
            confirmButton = {
                TextButton(onClick = { confirmWeek = false; onBeginWeek() }) { Text("Начать", color = Cyan) }
            },
            dismissButton = { TextButton(onClick = { confirmWeek = false }) { Text("Ещё потренируюсь", color = TextDim) } }
        )
    }
}

private data class ActionSpec(val title: String, val subtitle: String, val icon: ImageVector, val action: HubAction)

@Composable
private fun ActionRow(specs: List<ActionSpec>, ap: Int, onAction: (HubAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        specs.forEach { spec ->
            val enabled = ap > 0
            NeonCard(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .alpha(if (enabled) 1f else 0.4f),
                padding = 12.dp,
                onClick = if (enabled) ({ onAction(spec.action) }) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(spec.icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.weight(1f))
                    Text("1 ОД", style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
                Spacer(Modifier.height(7.dp))
                Text(spec.title, style = MaterialTheme.typography.titleSmall, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(spec.subtitle, style = MaterialTheme.typography.labelSmall, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MatchupCard(state: GameState, matchup: Matchup) {
    val strength = state.teamStrength(matchup.discipline, matchup.isPlayoff)
    val chance = (winChance(strength, matchup.opponent.strength) * 100).roundToInt()
    NeonCard(
        Modifier.fillMaxWidth(),
        borderColor = if (matchup.isPlayoff) Gold.copy(alpha = 0.45f) else Outline
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(matchup.opponent.name, matchup.opponent.name, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(matchup.discipline.label, style = MaterialTheme.typography.labelSmall, color = Cyan)
                    when {
                        matchup.isCup -> {
                            Spacer(Modifier.width(6.dp))
                            Pill(if (matchup.cupRound >= 2) "ФИНАЛ КУБКА" else "ПОЛУФИНАЛ КУБКА", Gold)
                        }
                        matchup.isPlayoff -> {
                            Spacer(Modifier.width(6.dp))
                            Pill("ПЛЕЙ-ОФФ · BO3", Gold)
                        }
                    }
                }
                Text(
                    matchup.opponent.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Сила соперника: ${matchup.opponent.strength}", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$chance%", style = MaterialTheme.typography.headlineSmall, color = if (chance >= 50) Success else Danger)
                Text("шансы", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

@Composable
private fun ResultRow(result: MatchResult) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (result.win) Success.copy(alpha = 0.08f) else Danger.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (result.win) "В" else "П",
            color = if (result.win) Success else Danger,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.width(9.dp))
        Text(
            "${result.discipline.label}${if (result.isCup) " · кубок" else ""} · ${result.opponentName}",
            style = MaterialTheme.typography.bodySmall,
            color = TextMain,
            modifier = Modifier.weight(1f)
        )
        Text("${result.seriesUs}:${result.seriesThem}", style = MaterialTheme.typography.titleSmall, color = if (result.win) Success else Danger)
    }
}

@Composable
private fun MetaCard(state: GameState) {
    val meta = state.meta
    NeonCard(Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.25f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TrendingUp, null, tint = Cyan, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Мета · ${meta.title}", style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(
                    "Патч влияет на матчи и тренировки",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
            Pill("${meta.weeksLeft} нед.", Cyan)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "В фаворе тактика «${meta.favoredTactic.styleLabel()}» и атрибут «${meta.favoredAttribute.label}».",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Выбирайте эти направления в подготовке к матчу и на тренировках — получите бонус.",
            style = MaterialTheme.typography.labelSmall,
            color = Cyan.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun FinanceStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
    }
}

private fun signedMoney(value: Int): String = if (value >= 0) "+${value.moneyShort()}" else "−${(-value).moneyShort()}"

private fun weeklyNet(s: GameState): Int {
    val tier = max(s.csLeague.tier, s.dotaLeague.tier)
    val sponsor = s.sponsor?.weekly ?: 0
    val merch = (s.org.fans * 0.10 * (1.0 + (s.facilities.merch - 1) * 0.15)).toInt()
    val stipend = 12_000 + tier * 4_000
    val salaries = (s.csTeam.players + s.dotaTeam.players).sumOf { it.salary } + s.staff.sumOf { it.salary }
    return sponsor + merch + stipend - salaries
}

// ---------------------------------------------------------------------------
// Action dialogs
// ---------------------------------------------------------------------------

@Composable
private fun TrainTeamDialog(onDismiss: () -> Unit, onConfirm: (Discipline, Attribute) -> Unit) {
    var discipline by remember { mutableStateOf(Discipline.CS) }
    var attr by remember { mutableStateOf(Attribute.MECHANICS) }
    GameDialog(onDismiss, "Командная тренировка", "Все игроки состава получат прирост выбранного атрибута. Стоимость: 1 ОД.") {
        DisciplineTabs(discipline, { discipline = it })
        Spacer(Modifier.height(12.dp))
        AttributeChips(attr) { attr = it }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(discipline, attr) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
        ) { Text("ТРЕНИРОВАТЬ", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun IndividualTrainDialog(state: GameState, onDismiss: () -> Unit, onConfirm: (String, Attribute) -> Unit) {
    var discipline by remember { mutableStateOf(Discipline.CS) }
    val players = state.teamFor(discipline).players
    var selected by remember(discipline) { mutableStateOf(players.firstOrNull()?.id) }
    var attr by remember { mutableStateOf(Attribute.MECHANICS) }
    GameDialog(onDismiss, "Индивидуальная тренировка", "Один игрок получит заметный прирост. Стоимость: 1 ОД.") {
        DisciplineTabs(discipline, {
            discipline = it
            selected = state.teamFor(it).players.firstOrNull()?.id
        })
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.heightIn(max = 230.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(players) { p ->
                val active = p.id == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Cyan.copy(alpha = 0.1f) else Color.Transparent)
                        .border(1.dp, if (active) Cyan.copy(alpha = 0.5f) else Outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { selected = p.id }
                        .padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatar(p.nick, p.id, 32.dp, p.ovr())
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.nick, style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Text("${p.role.short} · ${p.role.label}", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                    Text("${p.fatigue}%", style = MaterialTheme.typography.labelSmall, color = if (p.fatigue > 60) Danger else TextDim)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AttributeChips(attr) { attr = it }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { selected?.let { onConfirm(it, attr) } },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
        ) { Text("ТРЕНИРОВАТЬ", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AttributeChips(selected: Attribute, onSelect: (Attribute) -> Unit) {
    val attrs = listOf(Attribute.MECHANICS, Attribute.GAMESENSE, Attribute.TEAMWORK, Attribute.MENTALITY, Attribute.STAMINA)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attrs.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { a ->
                    val active = a == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) Cyan.copy(alpha = 0.14f) else Surface2)
                            .border(1.dp, if (active) Cyan else Outline, RoundedCornerShape(9.dp))
                            .clickable { onSelect(a) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(a.label, style = MaterialTheme.typography.labelSmall, color = if (active) Cyan else TextDim, maxLines = 1)
                    }
                }
                if (row.size < 3) {
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TeamPickDialog(title: String, text: String, onDismiss: () -> Unit, onPick: (Discipline) -> Unit) {
    GameDialog(onDismiss, title, text) {
        Discipline.entries.forEach { d ->
            OutlinedButton(
                onClick = { onPick(d) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(d.label, color = TextMain, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun GameDialog(
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Surface)
                .border(1.dp, Outline, RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextMain)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
