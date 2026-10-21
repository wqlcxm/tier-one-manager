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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
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

private enum class MarketSort(val label: String) { OVR("По OVR"), PRICE("По цене"), POTENTIAL("По потенциалу") }

@Composable
fun MarketScreen(
    state: GameState,
    onScoutCandidate: (String) -> Unit,
    onSignPlayer: (String) -> Unit,
    onNegotiate: (String) -> Unit,
    onAcceptOffer: (String) -> Unit,
    onRejectOffer: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var discipline by remember { mutableStateOf(Discipline.CS) }
    var sort by remember { mutableStateOf(MarketSort.OVR) }
    var confirmCandidate by remember { mutableStateOf<TransferCandidate?>(null) }
    var refreshConfirm by remember { mutableStateOf(false) }
    val roster = state.teamFor(discipline)
    val incoming = state.incomingOffers.mapNotNull { offer ->
        state.playerById(offer.playerId)?.let { offer to it }
    }
    val list = state.market
        .filter { it.player.discipline == discipline }
        .sortedWith(
            when (sort) {
                MarketSort.OVR -> compareByDescending<TransferCandidate> { it.player.ovr() }
                MarketSort.PRICE -> compareBy { it.price }
                MarketSort.POTENTIAL -> compareByDescending { it.player.potential }
            }
        )
    val full = roster.players.size >= 8

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { OrgHeader(state) }
        item {
            Text("Трансферный рынок", style = MaterialTheme.typography.headlineMedium, color = TextMain)
            Text("Скаутинг скрывает часть данных. Подписывайте игроков под стиль состава.", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                InfoPill("Баланс ${state.money.moneyShort()}", Success)
                InfoPill("Состав ${roster.players.size}/8", if (full) Danger else Cyan)
                InfoPill("ОД ${state.actionPoints}", Gold)
                if (incoming.isNotEmpty()) InfoPill("Офферы ${incoming.size}", Gold)
            }
        }
        if (incoming.isNotEmpty()) {
            item {
                SectionHeader("Предложения по вашим игрокам")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    incoming.forEach { (offer, player) ->
                        IncomingOfferCard(
                            offer = offer,
                            player = player,
                            canSell = canSell(state, player.id),
                            onAccept = { onAcceptOffer(offer.id) },
                            onReject = { onRejectOffer(offer.id) }
                        )
                    }
                }
            }
        }
        item { DisciplineTabs(discipline, { discipline = it }) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MarketSort.entries.forEach { s ->
                        val active = s == sort
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) Cyan.copy(alpha = 0.12f) else Surface2)
                                .border(1.dp, if (active) Cyan.copy(alpha = 0.5f) else Outline, RoundedCornerShape(9.dp))
                                .clickable { sort = s }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(s.label, style = MaterialTheme.typography.labelSmall, color = if (active) Cyan else TextDim)
                        }
                    }
                }
                Icon(
                    Icons.Rounded.Refresh,
                    "Обновить рынок",
                    tint = if (state.actionPoints > 0) Cyan else TextDim,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = state.actionPoints > 0) { refreshConfirm = true }
                        .padding(6.dp)
                )
            }
        }
        if (list.isEmpty()) {
            item {
                NeonCard(Modifier.fillMaxWidth()) {
                    Text("На рынке нет игроков этой дисциплины.", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { refreshConfirm = true }, enabled = state.actionPoints > 0) {
                        Text("Обновить рынок · 1 ОД", color = if (state.actionPoints > 0) Cyan else TextDim)
                    }
                }
            }
        }
        items(list) { candidate ->
            CandidateCard(
                candidate = candidate,
                money = state.money,
                rosterFull = full,
                actionPoints = state.actionPoints,
                onScout = { onScoutCandidate(candidate.player.id) },
                onSign = { confirmCandidate = candidate }
            )
        }
        item {
            NeonCard(Modifier.fillMaxWidth()) {
                Text("Как работает скаутинг", style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(
                    "Уровень 0 — известен только диапазон OVR. Уровень 1 — точный OVR и зарплата. Уровень 2 — потенциал и характер. Разведка стоит 1 ОД.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        }
    }

    confirmCandidate?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmCandidate = null },
            title = { Text("Подписать ${c.player.nick}?") },
            text = {
                Text(
                    "${c.player.role.label}, ${c.player.age} лет, OVR ${c.player.ovr()}.\n" +
                        "Трансфер: ${c.price.moneyFull()}\nЗарплата: ${c.player.salary.moneyFull()} / нед.\n\n" +
                        "Можно попробовать сбить цену на 18% за 1 ОД. Агент согласится не всегда."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCandidate = null
                    onSignPlayer(c.player.id)
                }) { Text("Подписать", color = Cyan) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            confirmCandidate = null
                            onNegotiate(c.player.id)
                        },
                        enabled = state.actionPoints > 0
                    ) { Text("Торг −18% · 1 ОД", color = if (state.actionPoints > 0) Gold else TextDim) }
                    TextButton(onClick = { confirmCandidate = null }) { Text("Отмена", color = TextDim) }
                }
            }
        )
    }
    if (refreshConfirm) {
        AlertDialog(
            onDismissRequest = { refreshConfirm = false },
            title = { Text("Обновить рынок?") },
            text = { Text("Текущие кандидаты исчезнут, появятся новые. Стоимость: 1 ОД.") },
            confirmButton = {
                TextButton(onClick = { refreshConfirm = false; onRefresh() }) { Text("Обновить", color = Cyan) }
            },
            dismissButton = { TextButton(onClick = { refreshConfirm = false }) { Text("Отмена", color = TextDim) } }
        )
    }
}

private fun canSell(state: GameState, playerId: String): Boolean {
    val player = state.playerById(playerId) ?: return false
    val team = state.teamFor(player.discipline)
    return team.players.size > 5 && team.players.count { it.role == player.role } > 1
}

@Composable
private fun IncomingOfferCard(
    offer: TransferOffer,
    player: Player,
    canSell: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    NeonCard(Modifier.fillMaxWidth(), padding = 12.dp, borderColor = Gold.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(player.nick, player.id, 42.dp, player.ovr())
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(player.nick, style = MaterialTheme.typography.titleSmall, color = TextMain)
                    Spacer(Modifier.width(5.dp))
                    Pill("${offer.weeksLeft} нед.", Gold)
                }
                Text(
                    "${player.role.short} · ${player.role.label} · ${player.discipline.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
                Text("Покупатель: ${offer.clubName}", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(offer.amount.moneyShort(), style = MaterialTheme.typography.titleMedium, color = Gold)
                Text("предложение", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
        if (!canSell) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Продажа недоступна: в составе должно остаться 5 игроков, а позиция ${player.role.short} не должна опустеть.",
                style = MaterialTheme.typography.labelSmall,
                color = Danger
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(
                onClick = onAccept,
                enabled = canSell,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Bg,
                    disabledContainerColor = Surface3,
                    disabledContentColor = TextDim
                )
            ) { Text("ПРОДАТЬ", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(vertical = 5.dp)
            ) { Text("ОТКЛОНИТЬ", style = MaterialTheme.typography.labelSmall, color = TextDim) }
        }
    }
}

@Composable
private fun InfoPill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun CandidateCard(
    candidate: TransferCandidate,
    money: Int,
    rosterFull: Boolean,
    actionPoints: Int,
    onScout: () -> Unit,
    onSign: () -> Unit
) {
    val p = candidate.player
    val known = candidate.scoutLevel
    val canBuy = money >= candidate.price && !rosterFull
    NeonCard(Modifier.fillMaxWidth(), padding = 13.dp, borderColor = if (p.ovr() >= 72) Gold.copy(alpha = 0.3f) else Outline) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(p.nick, p.id, 46.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.nick, style = MaterialTheme.typography.titleMedium, color = TextMain)
                    Spacer(Modifier.width(5.dp))
                    Text(flagOf(p.country), fontSize = 12.sp)
                    if (known >= 2) {
                        Spacer(Modifier.width(5.dp))
                        Pill(p.trait.label, Gold)
                    }
                }
                Text("${p.role.short} · ${p.age} лет · ${p.role.label}", style = MaterialTheme.typography.labelSmall, color = TextDim)
                Text(
                    if (known >= 1) "ЗП ${p.salary.moneyShort()} / нед." else "Зарплата: ?",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim.copy(alpha = 0.8f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (known == 0) "${(p.ovr() - 4).coerceAtLeast(30)}–${(p.ovr() + 4).coerceAtMost(99)}" else "${p.ovr()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ColorForOvr(p.ovr())
                )
                Text("OVR", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (known >= 2) "Потенциал ${p.potential}" else if (known == 1) "Потенциал ${(p.potential - 5).coerceAtLeast(p.ovr())}–${(p.potential + 3).coerceAtMost(99)}" else "Потенциал: ?",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold
                )
                Text("Цена ${candidate.price.moneyFull()}", style = MaterialTheme.typography.labelSmall, color = TextMain)
            }
            if (known < 2) {
                OutlinedButton(
                    onClick = onScout,
                    enabled = actionPoints > 0,
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, modifier = Modifier.size(13.dp), tint = if (actionPoints > 0) Cyan else TextDim)
                    Spacer(Modifier.width(4.dp))
                    Text("РАЗВЕДАТЬ", style = MaterialTheme.typography.labelSmall, color = if (actionPoints > 0) Cyan else TextDim)
                }
                Spacer(Modifier.width(6.dp))
            }
            Button(
                onClick = onSign,
                enabled = canBuy,
                shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg, disabledContainerColor = Surface3, disabledContentColor = TextDim)
            ) {
                Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ПОДПИСАТЬ", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (rosterFull) {
            Spacer(Modifier.height(5.dp))
            Text("Состав полон (8 игроков). Сначала продайте кого-то.", style = MaterialTheme.typography.labelSmall, color = Danger)
        } else if (money < candidate.price) {
            Spacer(Modifier.height(5.dp))
            Text("Недостаточно денег.", style = MaterialTheme.typography.labelSmall, color = Danger)
        }
    }
}
