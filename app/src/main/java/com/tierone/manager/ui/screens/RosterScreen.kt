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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.data.*
import com.tierone.manager.ui.components.*
import com.tierone.manager.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun RosterScreen(state: GameState, onPlayerClick: (String) -> Unit) {
    var discipline by remember { mutableStateOf(Discipline.CS) }
    val team = state.teamFor(discipline)
    val strength = state.teamStrength(discipline)
    val results = state.matchHistory.filter { it.discipline == discipline }.takeLast(8).map { it.win }
    val roles = Role.forDiscipline(discipline)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { OrgHeader(state) }
        item {
            Text("Состав", style = MaterialTheme.typography.headlineMedium, color = TextMain)
            Text("Управляйте игроками, формой и нагрузкой", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        item { DisciplineTabs(discipline, { discipline = it }) }
        item {
            NeonCard(Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.25f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(team.name, style = MaterialTheme.typography.titleMedium, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${discipline.label} · ${team.players.size} игроков · ${tierLabel(state.leagueFor(discipline).tier)}", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(strength.roundToInt().toString(), style = MaterialTheme.typography.displaySmall, color = Cyan)
                        Text("сила", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Metric("Средний OVR", team.averageOvr().toString(), TextMain, Modifier.weight(1f))
                    Metric("Химия", "${team.chemistry}", if (team.chemistry >= 65) Success else Gold, Modifier.weight(1f))
                    Metric("Тренер", state.staffFor(StaffRole.COACH)?.let { "${it.rating}" } ?: "—", TextMain, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Форма", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    Spacer(Modifier.width(10.dp))
                    FormDots(results)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Groups, null, tint = TextDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "СИСТЕМА РОТАЦИИ: ЛУЧШИЙ ИГРОК НА КАЖДУЮ РОЛЬ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    letterSpacing = 0.8.sp
                )
            }
        }
        roles.forEach { role ->
            val players = team.players.filter { it.role == role }.sortedByDescending { it.ovr() }
            if (players.isNotEmpty()) {
                item(key = "role-${role.name}") {
                    Text(
                        role.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                players.forEach { player ->
                    item(key = player.id) {
                        PlayerCard(player, onClick = { onPlayerClick(player.id) })
                    }
                }
            }
        }
        item {
            NeonCard(Modifier.fillMaxWidth(), onClick = { onPlayerClick(team.players.first().id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Нажмите на игрока, чтобы открыть профиль и тренировку", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(player: Player, onClick: () -> Unit) {
    NeonCard(Modifier.fillMaxWidth(), padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(player.nick, player.id, 46.dp, player.ovr())
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(player.nick, style = MaterialTheme.typography.titleMedium, color = TextMain)
                    Spacer(Modifier.width(5.dp))
                    Text(flagOf(player.country), fontSize = 12.sp)
                }
                Text(
                    "${player.role.short} · ${player.age} лет · ${player.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "ЗП ${player.salary.moneyShort()} · контракт ${player.contractWeeks} нед.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${player.ovr()}", style = MaterialTheme.typography.headlineSmall, color = ColorForOvr(player.ovr()))
                Text("OVR", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniMetric("Форма", player.form, if (player.form >= 60) Success else Gold, Modifier.weight(1f))
            MiniMetric("Мораль", player.morale, if (player.morale >= 60) Cyan else Danger, Modifier.weight(1f))
            MiniMetric("Усталость", player.fatigue, if (player.fatigue >= 60) Danger else if (player.fatigue >= 35) Gold else Success, Modifier.weight(1f), invert = true)
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: Int, color: Color, modifier: Modifier = Modifier, invert: Boolean = false) {
    Column(modifier) {
        Row {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim, modifier = Modifier.weight(1f))
            Text(value.toString(), style = MaterialTheme.typography.labelSmall, color = color)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Outline.copy(alpha = 0.5f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((if (invert) 100 - value else value).coerceIn(0, 100) / 100f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

fun ColorForOvr(ovr: Int): Color = when {
    ovr >= 80 -> Gold
    ovr >= 70 -> Success
    ovr >= 60 -> Cyan
    else -> TextMain
}

@Composable
private fun Metric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
fun PlayerDetailScreen(
    state: GameState,
    playerId: String,
    onBack: () -> Unit,
    onTrain: (String, Attribute) -> Unit,
    onSell: (String) -> Unit
) {
    val player = state.playerById(playerId)
    var trainDialog by remember { mutableStateOf(false) }
    var sellDialog by remember { mutableStateOf(false) }
    if (player == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Игрок не найден", color = TextDim)
        }
        return
    }
    val team = state.teamFor(player.discipline)
    val canSell = team.players.size > 5 && team.players.count { it.role == player.role } > 1
    val sellPrice = (player.marketValue() * 0.7).toInt()
    val ovr = player.ovr()

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BackHeader("Профиль игрока", "${player.discipline.label} · ${player.role.label}", onBack)
        Spacer(Modifier.height(12.dp))
        NeonCard(Modifier.fillMaxWidth(), borderColor = ColorForOvr(ovr).copy(alpha = 0.35f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(player.nick, player.id, 74.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(player.nick, style = MaterialTheme.typography.headlineMedium, color = TextMain)
                    Text("${player.name} · ${player.age} лет ${flagOf(player.country)}", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Pill(player.role.short, Cyan)
                        Pill(player.trait.label, Gold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$ovr", style = MaterialTheme.typography.displayMedium, color = ColorForOvr(ovr))
                    Text("OVR", style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Metric("Потенциал", player.potential.toString(), Gold, Modifier.weight(1f))
                Metric("Зарплата", player.salary.moneyShort(), Success, Modifier.weight(1f))
                Metric("Контракт", "${player.contractWeeks} нед.", TextMain, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Рыночная стоимость: ${player.marketValue().moneyShort()}",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        Spacer(Modifier.height(12.dp))
        NeonCard(Modifier.fillMaxWidth()) {
            Text("Профиль навыков", style = MaterialTheme.typography.titleSmall, color = TextMain)
            RadarChart(
                values = listOf(player.mechanics, player.gamesense, player.teamwork, player.mentality, player.stamina),
                labels = listOf("МЕХ", "ГЕЙМ", "КОМ", "МЕНТ", "ВЫН"),
                modifier = Modifier.fillMaxWidth()
            )
            StatBar("Механика", player.mechanics, Cyan)
            Spacer(Modifier.height(7.dp))
            StatBar("Геймсенс", player.gamesense, Magenta)
            Spacer(Modifier.height(7.dp))
            StatBar("Командность", player.teamwork, Success)
            Spacer(Modifier.height(7.dp))
            StatBar("Ментальность", player.mentality, Gold)
            Spacer(Modifier.height(7.dp))
            StatBar("Выносливость", player.stamina, Color(0xFF8B7BFF))
        }
        Spacer(Modifier.height(12.dp))
        NeonCard(Modifier.fillMaxWidth()) {
            Text("Характер", style = MaterialTheme.typography.titleSmall, color = TextMain)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(player.trait.label, Gold)
                Spacer(Modifier.width(8.dp))
                Text(player.trait.hint, style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Состояние", style = MaterialTheme.typography.titleSmall, color = TextMain, modifier = Modifier.weight(1f))
                Text(
                    "Потенциал ${if (player.ovr() >= player.potential) "исчерпан" else "есть запас"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (player.ovr() >= player.potential) Danger else Success
                )
            }
            Spacer(Modifier.height(8.dp))
            StatBar("Форма", player.form, Success)
            Spacer(Modifier.height(6.dp))
            StatBar("Мораль", player.morale, Cyan)
            Spacer(Modifier.height(6.dp))
            StatBar("Усталость", player.fatigue, if (player.fatigue >= 60) Danger else Gold)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { trainDialog = true },
            enabled = state.actionPoints > 0 && player.ovr() < player.potential,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
        ) {
            Text(
                if (state.actionPoints <= 0) "НЕТ ОЧКОВ ДЕЙСТВИЙ"
                else if (player.ovr() >= player.potential) "ПОТЕНЦИАЛ ИСЧЕРПАН"
                else "ТРЕНИРОВАТЬ · 1 ОД",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { sellDialog = true },
            enabled = canSell,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Rounded.Sell, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                if (canSell) "ПРОДАТЬ ЗА ${sellPrice.moneyShort()}"
                else "НЕЛЬЗЯ ПРОДАТЬ: МИНИМУМ 5 ИГРОКОВ",
                color = if (canSell) Danger else TextDim,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (trainDialog) {
        PlayerTrainDialog(
            state = state,
            player = player,
            onDismiss = { trainDialog = false },
            onConfirm = { attr -> trainDialog = false; onTrain(player.id, attr) }
        )
    }
    if (sellDialog) {
        AlertDialog(
            onDismissRequest = { sellDialog = false },
            title = { Text("Продать игрока?") },
            text = { Text("${player.nick} покинет клуб за ${sellPrice.moneyFull()}. Химия состава немного упадёт.") },
            confirmButton = {
                TextButton(onClick = { sellDialog = false; onSell(player.id) }) { Text("Продать", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { sellDialog = false }) { Text("Отмена", color = TextDim) } }
        )
    }
}

@Composable
private fun PlayerTrainDialog(
    state: GameState,
    player: Player,
    onDismiss: () -> Unit,
    onConfirm: (Attribute) -> Unit
) {
    var attr by remember { mutableStateOf(Attribute.MECHANICS) }
    GameDialog(
        onDismiss,
        "Тренировка ${player.nick}",
        "Выберите атрибут. Прирост зависит от потенциала и тренера. Стоимость: 1 ОД."
    ) {
        AttributeGrid(attr) { attr = it }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onConfirm(attr) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
        ) { Text("ТРЕНИРОВАТЬ", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AttributeGrid(selected: Attribute, onSelect: (Attribute) -> Unit) {
    val attrs = listOf(Attribute.MECHANICS, Attribute.GAMESENSE, Attribute.TEAMWORK, Attribute.MENTALITY, Attribute.STAMINA)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attrs.chunked(2).forEach { row ->
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
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(a.label, style = MaterialTheme.typography.labelSmall, color = if (active) Cyan else TextDim)
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}
