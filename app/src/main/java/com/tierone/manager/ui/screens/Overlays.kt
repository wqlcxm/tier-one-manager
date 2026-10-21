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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.data.*
import com.tierone.manager.ui.components.*
import com.tierone.manager.ui.theme.*

@Composable
fun EventDialog(event: GameEvent, onChoose: (Int) -> Unit) {
    GameDialog(
        onDismiss = {},
        title = event.title,
        subtitle = "Событие недели"
    ) {
        Text(event.text, style = MaterialTheme.typography.bodyMedium, color = TextDim)
        Spacer(Modifier.height(14.dp))
        event.choices.forEachIndexed { index, choice ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .border(1.dp, Outline, RoundedCornerShape(12.dp))
                    .padding(11.dp)
            ) {
                Text(choice.label, style = MaterialTheme.typography.titleSmall, color = Cyan)
                Text(choice.hint, style = MaterialTheme.typography.labelSmall, color = TextDim)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onChoose(index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan.copy(alpha = 0.14f), contentColor = Cyan),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("ВЫБРАТЬ", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun WeekReportScreen(state: GameState, report: WeekReport, onContinue: () -> Unit) {
    NeonBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(22.dp))
            Text("ИТОГИ НЕДЕЛИ", style = MaterialTheme.typography.labelSmall, color = Cyan, letterSpacing = 2.sp)
            Text(
                "Сезон ${report.season} · Неделя ${report.week}",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMain
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                if (report.results.isNotEmpty()) {
                    item {
                        NeonCard(Modifier.fillMaxWidth()) {
                            Text("Матчи", style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Spacer(Modifier.height(8.dp))
                            report.results.forEachIndexed { index, result ->
                                if (index > 0) Spacer(Modifier.height(9.dp))
                                val stage = when {
                                    result.isCup && result.cupRound >= 2 -> "финал кубка"
                                    result.isCup -> "полуфинал кубка"
                                    result.isPlayoff -> "плей-офф"
                                    else -> "лига"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .width(4.dp)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (result.win) Success else Danger)
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${result.discipline.label}${if (result.isCup) " · КУБОК" else ""} · ${result.opponentName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextMain
                                        )
                                        Text(
                                            "${if (result.win) "Победа" else "Поражение"} · $stage · MVP ${result.mvp} · +${result.fans} фанатов",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextDim
                                        )
                                    }
                                    Text(
                                        "${result.seriesUs}:${result.seriesThem}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (result.win) Success else Danger
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.25f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Финансы", style = MaterialTheme.typography.titleSmall, color = TextMain, modifier = Modifier.weight(1f))
                            Text(
                                if (report.income - report.expenses >= 0) "+${(report.income - report.expenses).moneyShort()}" else "−${(report.expenses - report.income).moneyShort()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (report.income - report.expenses >= 0) Success else Danger
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            ReportStat("Доход", report.income.moneyShort(), Success, Modifier.weight(1f))
                            ReportStat("Расход", report.expenses.moneyShort(), Danger, Modifier.weight(1f))
                            ReportStat("Фанаты", "+${report.fanDelta}", Magenta, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(9.dp))
                        report.notes.forEach { note ->
                            Text("· $note", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        }
                    }
                }
                if (report.objectiveLines.isNotEmpty()) {
                    item {
                        NeonCard(Modifier.fillMaxWidth(), borderColor = Success.copy(alpha = 0.25f)) {
                            Text("Выполненные задачи", style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Spacer(Modifier.height(6.dp))
                            report.objectiveLines.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall, color = Success)
                            }
                        }
                    }
                }
                item {
                    NeonCard(Modifier.fillMaxWidth()) {
                        Text("Баланс клуба", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(state.money.moneyFull(), style = MaterialTheme.typography.headlineMedium, color = if (state.money >= 0) Success else Danger)
                    }
                }
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
            ) {
                Text("ПРОДОЛЖИТЬ", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ReportStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
fun SeasonSummaryScreen(state: GameState, summary: SeasonSummary, onContinue: () -> Unit) {
    val champion = summary.trophiesWon.isNotEmpty()
    NeonBackground {
        Confetti(active = champion, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("СЕЗОН ${summary.season} ЗАВЕРШЁН", style = MaterialTheme.typography.labelSmall, color = Gold, letterSpacing = 2.sp)
                    Text(
                        if (champion) "ЕСТЬ ТРОФЕЙ!" else "Сезон в архиве",
                        style = MaterialTheme.typography.displayMedium,
                        color = if (champion) Gold else TextMain
                    )
                    Text(
                        "Новый сезон уже сформирован. Составы стали на год старше, рынок обновился.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlacementCard("CS2", summary.csPlacement, summary.csChampion, summary.promotedCs, summary.relegatedCs, Modifier.weight(1f))
                        PlacementCard("Dota 2", summary.dotaPlacement, summary.dotaChampion, summary.promotedDota, summary.relegatedDota, Modifier.weight(1f))
                    }
                }
                item {
                    NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                        Text("Награды", style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            ReportStat("Призовые", summary.prize.moneyShort(), Success, Modifier.weight(1f))
                            ReportStat("Фанаты", "+${summary.fanGain}", Magenta, Modifier.weight(1f))
                            ReportStat("Трофеи", "${summary.trophiesWon.size}", Gold, Modifier.weight(1f))
                        }
                    }
                }
                if (summary.trophiesWon.isNotEmpty()) {
                    item {
                        NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.4f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = Gold)
                                Spacer(Modifier.width(8.dp))
                                Text("Новые трофеи", style = MaterialTheme.typography.titleSmall, color = Gold)
                            }
                            Spacer(Modifier.height(7.dp))
                            summary.trophiesWon.forEach { trophy ->
                                Text("· $trophy", style = MaterialTheme.typography.bodySmall, color = Gold.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
                item {
                    NeonCard(Modifier.fillMaxWidth()) {
                        Text("Отчёт сезона", style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Spacer(Modifier.height(7.dp))
                        summary.notes.forEach { note ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text("·", color = Cyan)
                                Spacer(Modifier.width(6.dp))
                                Text(note, style = MaterialTheme.typography.bodySmall, color = TextDim)
                            }
                        }
                    }
                }
                item {
                    NeonCard(Modifier.fillMaxWidth()) {
                        Text("Клуб", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(state.org.name, style = MaterialTheme.typography.titleLarge, color = TextMain)
                        Text(
                            "Сезон ${state.season} · ${tierLabel(state.csLeague.tier)} / ${tierLabel(state.dotaLeague.tier)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cyan
                        )
                    }
                }
            }
            Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)
                ) {
                    Text("НОВЫЙ СЕЗОН", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun TutorialDialog(onClose: () -> Unit) {
    GameDialog(
        onDismiss = {},
        title = "TIER ONE",
        subtitle = "Быстрый старт · Сезон 2.0"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("МЕНЕДЖЕР", Cyan)
            Pill("ОФЛАЙН", Success)
            Pill("CS2 + DOTA 2", Magenta)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TutorialLine(
                "01", "Клуб",
                "Улучшайте инфраструктуру, нанимайте штат и подписывайте спонсоров. " +
                    "Специализация тренера или аналитика усиливает команду в выбранной тактике."
            )
            TutorialLine(
                "02", "Подготовка",
                "Перед матчем выберите тактику. В CS2 дополнительно выберите карту, " +
                    "в Dota 2 — план драфта. Мета и аналитик подскажут сильные стороны."
            )
            TutorialLine(
                "03", "Кубок",
                "Полуфиналы Кубка лиги проходят на 3-й неделе, финал — на 5-й. " +
                    "Кубковые матчи не меняют таблицу лиги, но дают призовые и трофей."
            )
            TutorialLine(
                "04", "Трансферы",
                "Скаут обновляет рынок. Можно подписать игрока сразу или поторговаться " +
                    "за 1 ОД и сбить цену на 18%. Входящие предложения по своим игрокам — во вкладке «Рынок»."
            )
            TutorialLine(
                "05", "Неделя",
                "Сначала сыграйте все матчи недели, затем завершите её. " +
                    "Игра сохраняется автоматически — интернет не нужен."
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)
        ) {
            Text("ПОЕХАЛИ", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun TutorialLine(number: String, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 11.dp)
    ) {
        Box(
            Modifier
                .size(25.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Cyan.copy(alpha = 0.12f))
                .border(1.dp, Cyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = Cyan)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
            Text(text, style = MaterialTheme.typography.labelSmall, color = TextDim, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun PlacementCard(
    discipline: String,
    placement: Int,
    champion: String,
    promoted: Boolean,
    relegated: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        placement == 1 -> Gold
        placement <= 4 -> Cyan
        else -> Danger
    }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(discipline, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text("$placement", style = MaterialTheme.typography.displaySmall, color = color)
        Text("место", style = MaterialTheme.typography.labelSmall, color = TextDim)
        Spacer(Modifier.height(7.dp))
        Text("Чемпион: $champion", style = MaterialTheme.typography.labelSmall, color = TextMain, maxLines = 2)
        if (promoted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TrendingUp, null, tint = Success, modifier = Modifier.width(14.dp))
                Text(" Повышение", style = MaterialTheme.typography.labelSmall, color = Success)
            }
        }
        if (relegated) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TrendingDown, null, tint = Danger, modifier = Modifier.width(14.dp))
                Text(" Понижение", style = MaterialTheme.typography.labelSmall, color = Danger)
            }
        }
    }
}
