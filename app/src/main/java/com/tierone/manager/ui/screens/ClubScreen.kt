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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.data.*
import com.tierone.manager.ui.components.*
import com.tierone.manager.ui.theme.*

@Composable
fun ClubScreen(
    state: GameState,
    onUpgrade: (FacilityType) -> Unit,
    onHireStaff: (String) -> Unit,
    onSignSponsor: (Int) -> Unit,
    onSettings: (Boolean, Boolean) -> Unit,
    onNewCareer: () -> Unit
) {
    var staffDialog by remember { mutableStateOf(false) }
    var sponsorConfirm by remember { mutableStateOf<Int?>(null) }
    var newCareerConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { OrgHeader(state) }
        item {
            Text("Клуб", style = MaterialTheme.typography.headlineMedium, color = TextMain)
            Text("Инфраструктура, штат, спонсоры и наследие", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        item {
            NeonCard(Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.22f)) {
                Row {
                    ClubStat("Баланс", state.money.moneyShort(), Success, Modifier.weight(1f))
                    ClubStat("Фанаты", state.org.fans.fansShort(), Magenta, Modifier.weight(1f))
                    ClubStat("Лучший OVR", "${state.bestOvr}", Gold, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    ClubStat("Победы", "${state.careerWins}", Success, Modifier.weight(1f))
                    ClubStat("Поражения", "${state.careerLosses}", Danger, Modifier.weight(1f))
                    ClubStat("Заработано", state.totalEarned.moneyShort(), Cyan, Modifier.weight(1f))
                }
            }
        }

        item { SectionHeader("Инфраструктура") }
        items(FacilityType.entries) { type ->
            FacilityRow(state, type, onUpgrade)
        }

        item {
            SectionHeader("Штат", action = "Кандидаты", onAction = { staffDialog = true })
            Spacer(Modifier.height(8.dp))
            NeonCard(Modifier.fillMaxWidth()) {
                StaffRole.entries.forEachIndexed { index, role ->
                    if (index > 0) Spacer(Modifier.height(9.dp))
                    val member = state.staffFor(role)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (role == StaffRole.COACH) Icons.Rounded.Person else Icons.Rounded.BarChart,
                            null,
                            tint = Cyan,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(role.label, style = MaterialTheme.typography.labelSmall, color = TextDim)
                            Text(
                                member?.name ?: "Вакансия",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (member == null) TextDim else TextMain
                            )
                            member?.specialty?.let { spec ->
                                Text(
                                    "Специализация: ${spec.styleLabel()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (member != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${member.rating}", style = MaterialTheme.typography.titleSmall, color = Gold)
                                Text("${member.salary.moneyShort()}/нед", style = MaterialTheme.typography.labelSmall, color = TextDim)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { staffDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp)
                            ) { Text("Нанять", color = Cyan, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Спонсоры")
            Spacer(Modifier.height(8.dp))
            val sponsor = state.sponsor
            NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                if (sponsor == null) {
                    Text("Активного спонсора нет", style = MaterialTheme.typography.titleSmall, color = Danger)
                    Text("Подпишите контракт ниже — это еженедельный доход.", style = MaterialTheme.typography.bodySmall, color = TextDim)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamLogo(sponsor.name, sponsor.name, 38.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sponsor.name, style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Text("${sponsor.weekly.moneyShort()} / неделя · осталось ${sponsor.weeks} нед.", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        }
                        Pill("АКТИВЕН", Success)
                    }
                }
                state.sponsorOffers.forEachIndexed { index, offer ->
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(offer.name, style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Text(
                                "${offer.weekly.moneyShort()}/нед · бонус ${offer.signingBonus.moneyShort()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                        }
                        Button(
                            onClick = { sponsorConfirm = index },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)
                        ) { Text("ПОДПИСАТЬ", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        item {
            SectionHeader("Достижения", action = "${state.achievements.size}/${Achievement.entries.size}")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Achievement.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { achievement ->
                            AchievementCard(
                                achievement,
                                unlocked = achievement in state.achievements,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (state.trophies.isNotEmpty()) {
            item {
                SectionHeader("Зал славы")
                Spacer(Modifier.height(8.dp))
                NeonCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                    state.trophies.reversed().forEachIndexed { index, trophy ->
                        if (index > 0) Spacer(Modifier.height(7.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(trophy, style = MaterialTheme.typography.bodySmall, color = Gold.copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Настройки")
            Spacer(Modifier.height(8.dp))
            NeonCard(Modifier.fillMaxWidth()) {
                SettingRow("Звук", "Системные клики и сигналы", state.settings.sound) {
                    onSettings(it, state.settings.vibration)
                }
                Spacer(Modifier.height(8.dp))
                SettingRow("Вибрация", "Отклик на важные события", state.settings.vibration) {
                    onSettings(state.settings.sound, it)
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { newCareerConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("НАЧАТЬ НОВУЮ КАРЬЕРУ", color = Danger, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(10.dp))
                Text(
                    "TIER ONE · версия 1.0 · офлайн-менеджер киберспортивной организации. Все команды и игроки вымышлены.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (staffDialog) {
        StaffDialog(state, onDismiss = { staffDialog = false }, onHire = { id ->
            staffDialog = false
            onHireStaff(id)
        })
    }
    sponsorConfirm?.let { index ->
        val offer = state.sponsorOffers.getOrNull(index)
        if (offer != null) {
            AlertDialog(
                onDismissRequest = { sponsorConfirm = null },
                title = { Text("Подписать ${offer.name}?") },
                text = {
                    Text(
                        "Еженедельно: ${offer.weekly.moneyFull()}\n" +
                            "Бонус при подписании: ${offer.signingBonus.moneyFull()}\n" +
                            "Срок: ${offer.weeks} недель"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { sponsorConfirm = null; onSignSponsor(index) }) { Text("Подписать", color = Gold) }
                },
                dismissButton = { TextButton(onClick = { sponsorConfirm = null }) { Text("Отмена", color = TextDim) } }
            )
        }
    }
    if (newCareerConfirm) {
        AlertDialog(
            onDismissRequest = { newCareerConfirm = false },
            title = { Text("Начать заново?") },
            text = { Text("Текущая карьера, трофеи и сохранение будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { newCareerConfirm = false; onNewCareer() }) { Text("Удалить и начать", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { newCareerConfirm = false }) { Text("Отмена", color = TextDim) } }
        )
    }
}

@Composable
private fun ClubStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun FacilityRow(state: GameState, type: FacilityType, onUpgrade: (FacilityType) -> Unit) {
    val level = state.facilities.level(type)
    val cost = facilityUpgradeCost(type, level)
    val maxed = level >= 5
    val canUpgrade = !maxed && state.money >= cost
    val icon: ImageVector = when (type) {
        FacilityType.TRAINING -> Icons.Rounded.FitnessCenter
        FacilityType.ANALYTICS -> Icons.Rounded.BarChart
        FacilityType.PSYCHOLOGY -> Icons.Rounded.Favorite
        FacilityType.BOOTCAMP -> Icons.Rounded.Home
        FacilityType.MERCH -> Icons.Rounded.ShoppingBag
        FacilityType.ACADEMY -> Icons.Rounded.School
    }
    NeonCard(Modifier.fillMaxWidth(), padding = 13.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Cyan.copy(alpha = 0.1f))
                    .border(1.dp, Cyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Cyan, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(type.label, style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(type.description, style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(5) { i ->
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (i < level) Cyan else Outline)
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                if (maxed) {
                    Text("МАКС", style = MaterialTheme.typography.labelSmall, color = Gold)
                } else {
                    Button(
                        onClick = { onUpgrade(type) },
                        enabled = canUpgrade,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg, disabledContainerColor = Surface3, disabledContentColor = TextDim)
                    ) { Text(cost.moneyShort(), style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Bg,
                checkedTrackColor = Cyan,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = Surface3,
                uncheckedBorderColor = Outline
            )
        )
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (unlocked) Gold.copy(alpha = 0.08f) else Surface)
            .border(1.dp, if (unlocked) Gold.copy(alpha = 0.4f) else Outline, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Icon(
            if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            null,
            tint = if (unlocked) Gold else TextDim.copy(alpha = 0.5f),
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(achievement.title, style = MaterialTheme.typography.labelMedium, color = if (unlocked) Gold else TextMain, maxLines = 2)
        Text(achievement.description, style = MaterialTheme.typography.labelSmall, color = TextDim, maxLines = 3, lineHeight = 13.sp)
    }
}

@Composable
private fun StaffDialog(state: GameState, onDismiss: () -> Unit, onHire: (String) -> Unit) {
    GameDialog(
        onDismiss,
        "Кандидаты в штат",
        "Тренер и аналитик усиливают игру, скаут улучшает рынок, психолог ускоряет восстановление. " +
            "Специализация тренера или аналитика даёт бонус, когда команда играет в этой тактике. " +
            "Найм заменяет сотрудника на той же должности; стоимость — 6 недельных зарплат."
    ) {
        if (state.staffCandidates.isEmpty()) {
            Text("Свободных кандидатов нет. Новые появятся в следующем сезоне.", style = MaterialTheme.typography.bodySmall, color = TextDim)
        } else {
            LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(state.staffCandidates.sortedBy { it.role.ordinal }) { staff ->
                    val cost = staff.salary * 6
                    val canHire = state.money >= cost
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(Surface2)
                            .border(1.dp, Outline, RoundedCornerShape(11.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${staff.role.label} · ${staff.name}", style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Text(
                                "Рейтинг ${staff.rating} · ${staff.salary.moneyShort()}/нед · найм ${cost.moneyShort()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                            Text(
                                staff.specialty?.let { "Специализация: ${it.styleLabel()}" }
                                    ?: "Без специализации",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (staff.specialty != null) Cyan else TextDim
                            )
                        }
                        Button(
                            onClick = { onHire(staff.id) },
                            enabled = canHire,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg, disabledContainerColor = Surface3, disabledContentColor = TextDim)
                        ) { Text("НАНЯТЬ", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}
