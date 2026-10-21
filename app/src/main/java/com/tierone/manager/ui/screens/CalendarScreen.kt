package com.tierone.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.data.*
import com.tierone.manager.game.Engine
import com.tierone.manager.ui.components.*
import com.tierone.manager.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(state: GameState) {
    var discipline by remember { mutableStateOf(Discipline.CS) }
    val league = state.leagueFor(discipline)
    val team = state.teamFor(discipline)
    val sorted = league.sorted()
    val rank = sorted.indexOfFirst { it.isPlayer } + 1
    val record = sorted.firstOrNull { it.isPlayer }
    val matchups = Engine.upcomingMatchups(state).filter { it.discipline == discipline }
    val cup = state.cupFor(discipline)
    val history = state.matchHistory.filter { it.discipline == discipline }.takeLast(10).reversed()
    val played = record?.played ?: 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { OrgHeader(state) }
        item {
            Text("Календарь", style = MaterialTheme.typography.headlineMedium, color = TextMain)
            Text("Сезон, таблица, плей-офф, кубок и история матчей", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        item { DisciplineTabs(discipline, { discipline = it }) }
        item {
            NeonCard(Modifier.fillMaxWidth(), borderColor = if (state.week >= 6) Gold.copy(alpha = 0.35f) else Outline) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(league.tierName(), style = MaterialTheme.typography.titleLarge, color = TextMain)
                        Text(
                            "Сезон ${state.season} · Неделя ${state.week}/7 · ${weekStatus(state.week)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("#$rank", style = MaterialTheme.typography.headlineMedium, color = if (rank <= 2) Success else if (rank <= 4) Cyan else Danger)
                        Text("место", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    MetricBox("Матчи", "$played", TextMain, Modifier.weight(1f))
                    MetricBox("Победы", "${record?.wins ?: 0}", Success, Modifier.weight(1f))
                    MetricBox("Разница", signedInt(record?.mapDiff ?: 0), if ((record?.mapDiff ?: 0) >= 0) Cyan else Danger, Modifier.weight(1f))
                    MetricBox("Форма", "${state.teamStrength(discipline).roundToInt()}", Gold, Modifier.weight(1f))
                }
            }
        }
        item {
            SectionHeader(if (state.week >= 6) "Плей-офф и кубок" else "Матчи недели")
            Spacer(Modifier.height(8.dp))
            if (matchups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchups.forEach { matchup ->
                        NeonCard(
                            Modifier.fillMaxWidth(),
                            borderColor = if (matchup.isPlayoff) Gold.copy(alpha = 0.4f) else Cyan.copy(alpha = 0.25f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogo(matchup.opponent.name, matchup.opponent.name, 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(matchup.opponent.name, style = MaterialTheme.typography.titleMedium, color = TextMain)
                                        if (matchup.isCup) {
                                            Spacer(Modifier.width(6.dp))
                                            Pill(if (matchup.cupRound >= 2) "ФИНАЛ" else "1/2", Gold)
                                        }
                                    }
                                    Text(
                                        when {
                                            matchup.isCup && matchup.cupRound >= 2 -> "Финал Кубка лиги · Bo3"
                                            matchup.isCup -> "Полуфинал Кубка лиги · Bo3"
                                            matchup.isPlayoff -> "Плей-офф · серия до 2 побед"
                                            else -> "Регулярный сезон · Bo1"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextDim
                                    )
                                }
                                val chance = (winChance(state.teamStrength(discipline, matchup.isPlayoff), matchup.opponent.strength) * 100).roundToInt()
                                Text("$chance%", style = MaterialTheme.typography.headlineSmall, color = if (chance >= 50) Success else Danger)
                            }
                        }
                    }
                }
            } else if (state.weekResults.any { it.discipline == discipline }) {
                NeonCard(Modifier.fillMaxWidth()) {
                    Text("Матчи недели сыграны", style = MaterialTheme.typography.titleSmall, color = TextMain)
                    Spacer(Modifier.height(5.dp))
                    state.weekResults.filter { it.discipline == discipline }.forEach { result ->
                        Text(
                            "${if (result.isCup) "Кубок" else "Лига"} · ${result.opponentName} · " +
                                "${if (result.win) "победа" else "поражение"} ${result.seriesUs}:${result.seriesThem}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.win) Success else Danger
                        )
                    }
                }
            } else {
                NeonCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (state.week >= 6) "Ваш состав не участвует в текущей стадии плей-офф."
                        else "Матч уже сыгран или ещё не назначен.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
            }
        }
        item {
            SectionHeader("Турнирная таблица")
            Spacer(Modifier.height(8.dp))
            NeonCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)) {
                    Text("#", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(18.dp))
                    Text("КОМАНДА", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.weight(1f))
                    Text("В-П", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(36.dp))
                    Text("Δ", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(30.dp))
                    Text("О", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(24.dp))
                }
                sorted.forEachIndexed { index, t ->
                    val isPlayer = t.isPlayer
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPlayer) Cyan.copy(alpha = 0.09f) else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPlayer) Cyan else if (index < 4) TextMain else TextDim,
                            modifier = Modifier.width(18.dp)
                        )
                        TeamLogo(t.name, t.name, 24.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            t.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPlayer) Cyan else TextMain,
                            fontWeight = if (isPlayer) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${t.wins}-${t.losses}", style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.width(36.dp))
                        Text(
                            signedInt(t.mapDiff),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (t.mapDiff >= 0) Success else Danger,
                            modifier = Modifier.width(30.dp)
                        )
                        Text("${t.wins * 3}", style = MaterialTheme.typography.labelMedium, color = TextMain, modifier = Modifier.width(24.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Топ-4 выходят в плей-офф. 1-2 место повышает дивизион, 5-6 понижают.", style = MaterialTheme.typography.labelSmall, color = TextDim.copy(alpha = 0.75f))
            }
        }
        if (league.playoffs.isNotEmpty()) {
            item {
                SectionHeader("Сетка плей-офф")
                Spacer(Modifier.height(8.dp))
                NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                    val semis = league.playoffs.filter { it.round == 1 }
                    val final = league.playoffs.firstOrNull { it.round == 2 }
                    Text("1/2 финала", style = MaterialTheme.typography.titleSmall, color = TextMain)
                    Spacer(Modifier.height(6.dp))
                    semis.forEach { s -> SeriesRow(league.teams, s) }
                    if (final != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Финал", style = MaterialTheme.typography.titleSmall, color = Gold)
                        Spacer(Modifier.height(6.dp))
                        SeriesRow(league.teams, final)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("Финал сформируется после полуфиналов.", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                }
            }
        }
        if (cup != null) {
            item {
                SectionHeader("Кубок лиги")
                Spacer(Modifier.height(8.dp))
                NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cup.title(), style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Text(
                                "Полуфиналы — неделя 3 · Финал — неделя 5",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                        }
                        if (cup.champion != null) Pill("ЧЕМПИОН", Gold)
                    }
                    Spacer(Modifier.height(8.dp))
                    val semis = cup.playoffs.filter { it.round == 1 }
                    val final = cup.playoffs.firstOrNull { it.round == 2 }
                    if (semis.isEmpty()) {
                        Text("Сетка кубка сформируется в начале сезона.", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    } else {
                        Text("1/2 финала", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Spacer(Modifier.height(4.dp))
                        semis.forEach { s -> SeriesRow(cup.teams, s) }
                    }
                    if (final != null) {
                        Spacer(Modifier.height(9.dp))
                        Text("Финал", style = MaterialTheme.typography.labelSmall, color = Gold)
                        Spacer(Modifier.height(4.dp))
                        SeriesRow(cup.teams, final)
                    } else if (semis.isNotEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        Text("Финал сформируется после полуфиналов.", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                    if (cup.champion != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Чемпион: ${cup.team(cup.champion!!)?.name ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold
                        )
                    }
                }
            }
        }
        item {
            SectionHeader("История матчей", action = "${state.trophies.size} трофеев")
            Spacer(Modifier.height(8.dp))
            NeonCard(Modifier.fillMaxWidth()) {
                if (history.isEmpty()) {
                    Text("История пока пуста. Сыграйте первый матч.", style = MaterialTheme.typography.bodySmall, color = TextDim)
                } else {
                    history.forEachIndexed { index, result ->
                        if (index > 0) Spacer(Modifier.height(9.dp))
                        HistoryRow(result)
                    }
                }
            }
        }
        if (state.trophies.isNotEmpty()) {
            item {
                NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Трофеи клуба", style = MaterialTheme.typography.titleSmall, color = TextMain)
                    }
                    Spacer(Modifier.height(7.dp))
                    state.trophies.takeLast(6).forEach { trophy ->
                        Text("· $trophy", style = MaterialTheme.typography.bodySmall, color = Gold.copy(alpha = 0.9f))
                    }
                }
            }
        }
    }
}

private fun weekStatus(week: Int): String = when {
    week <= 5 -> "регулярный сезон"
    week == 6 -> "полуфиналы"
    else -> "финал"
}

private fun signedInt(v: Int): String = if (v > 0) "+$v" else v.toString()

@Composable
private fun MetricBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun SeriesRow(teams: List<LeagueTeam>, series: PlayoffSeries) {
    val a = teams.firstOrNull { it.id == series.teamA }
    val b = teams.firstOrNull { it.id == series.teamB }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                a?.name ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = if (series.winner == series.teamA) Success else if (a?.isPlayer == true) Cyan else TextMain,
                fontWeight = if (series.winner == series.teamA) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                b?.name ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = if (series.winner == series.teamB) Success else if (b?.isPlayer == true) Cyan else TextMain,
                fontWeight = if (series.winner == series.teamB) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${series.winsA}", style = MaterialTheme.typography.titleSmall, color = if (series.winner == series.teamA) Success else TextDim)
            Text("${series.winsB}", style = MaterialTheme.typography.titleSmall, color = if (series.winner == series.teamB) Success else TextDim)
        }
    }
}

@Composable
private fun HistoryRow(result: MatchResult) {
    val stage = when {
        result.isCup -> "кубок"
        result.isPlayoff -> "плей-офф"
        else -> "лига"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (result.win) Success.copy(alpha = 0.15f) else Danger.copy(alpha = 0.15f))
                .border(1.dp, if (result.win) Success.copy(alpha = 0.5f) else Danger.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (result.win) "В" else "П", color = if (result.win) Success else Danger, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(result.opponentName, style = MaterialTheme.typography.bodySmall, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${result.dateLabel} · $stage · MVP ${result.mvp}",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        Text(
            "${result.seriesUs}:${result.seriesThem}",
            style = MaterialTheme.typography.titleSmall,
            color = if (result.win) Success else Danger
        )
    }
}
