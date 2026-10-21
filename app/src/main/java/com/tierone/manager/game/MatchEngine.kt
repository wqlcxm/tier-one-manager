package com.tierone.manager.game

import com.tierone.manager.data.*
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class Matchup(
    val discipline: Discipline,
    val opponent: LeagueTeam,
    val isPlayoff: Boolean,
    val series: PlayoffSeries? = null,
    val isCup: Boolean = false,
    val cupRound: Int = 0
)

data class MapChoice(
    val name: String,
    val favoredTactic: Tactic
)

enum class LiveEventKind { SCORE, INFO, OBJECTIVE, MAP_END }

data class LiveEvent(
    val text: String,
    val kind: LiveEventKind,
    val us: Int,
    val them: Int
)

enum class LivePhase { PREGAME, LIVE, RESULT }

enum class MatchDecision { TIMEOUT, CALM, HARSH }

data class LiveMatch(
    val discipline: Discipline,
    val opponentId: String,
    val opponentName: String,
    val opponentStrength: Int,
    val isPlayoff: Boolean,
    val tactic: Tactic,
    val opponentTactic: Tactic,
    val userStrength: Double,
    val userNicks: List<String>,
    val opponentNicks: List<String>,
    val mapName: String,
    val phase: LivePhase = LivePhase.PREGAME,
    val mapNumber: Int = 1,
    val scoreUs: Int = 0,
    val scoreThem: Int = 0,
    val seriesUs: Int = 0,
    val seriesThem: Int = 0,
    val events: List<LiveEvent> = emptyList(),
    val mapScores: List<MapScore> = emptyList(),
    val decisionPending: Boolean = false,
    val decisionTaken: Boolean = false,
    val finished: Boolean = false,
    val postMorale: Int = 0,
    val result: MatchResult? = null,
    val isCup: Boolean = false,
    val cupRound: Int = 0,
    val mapChoices: List<MapChoice> = emptyList(),
    val draftPlan: DraftPlan = DraftPlan.BALANCED,
    val opponentPlan: DraftPlan = DraftPlan.BALANCED
)

object MatchEngine {

    private val csMaps = listOf("Neon", "Foundry", "Dunes", "Terminal", "Frost", "Colosseum")

    fun startLive(state: GameState, matchup: Matchup, tactic: Tactic, rng: Random = Random.Default): LiveMatch {
        val d = matchup.discipline
        val team = state.teamFor(d)
        val lineup = team.lineup()
        val oppTactic = Tactic.entries.random(rng)
        val edge = tacticEdge(tactic, oppTactic)
        val strength = state.teamStrength(d, matchup.isPlayoff) + edge * 1.15
        val mapChoices = if (d == Discipline.CS) {
            csMaps.shuffled(rng).take(3).map { MapChoice(it, Tactic.entries.random(rng)) }
        } else emptyList()
        val mapName = mapChoices.firstOrNull()?.name ?: "Игра 1"
        return LiveMatch(
            discipline = d,
            opponentId = matchup.opponent.id,
            opponentName = matchup.opponent.name,
            opponentStrength = matchup.opponent.strength + if (matchup.isPlayoff) 1 else 0,
            isPlayoff = matchup.isPlayoff,
            tactic = tactic,
            opponentTactic = oppTactic,
            userStrength = strength,
            userNicks = lineup.map { it.nick },
            opponentNicks = matchup.opponent.roster.ifEmpty { List(5) { "enemy${it + 1}" } },
            mapName = mapName,
            isCup = matchup.isCup,
            cupRound = matchup.cupRound,
            mapChoices = mapChoices,
            opponentPlan = DraftPlan.entries.random(rng)
        )
    }

    fun previewStrength(
        state: GameState,
        live: LiveMatch,
        tactic: Tactic,
        mapIndex: Int,
        plan: DraftPlan
    ): Double {
        var bonus = tacticEdge(tactic, live.opponentTactic) * 1.15
        if (state.meta.favoredTactic == tactic) bonus += 0.7
        if (state.staffFor(StaffRole.COACH)?.specialty == tactic) bonus += 0.85
        if (state.staffFor(StaffRole.ANALYST)?.specialty == tactic) bonus += 0.4
        if (live.discipline == Discipline.CS) {
            val choice = live.mapChoices.getOrNull(mapIndex)
            if (choice != null) bonus += if (choice.favoredTactic == tactic) 1.2 else -0.2
        } else {
            bonus += draftEdge(plan, live.opponentPlan) * 0.9
        }
        return (state.teamStrength(live.discipline, live.isPlayoff) + bonus).coerceIn(30.0, 100.0)
    }

    fun applyPreparation(
        live: LiveMatch,
        tactic: Tactic,
        mapIndex: Int,
        plan: DraftPlan,
        state: GameState
    ): LiveMatch {
        val mapName = if (live.discipline == Discipline.CS) {
            live.mapChoices.getOrNull(mapIndex)?.name ?: live.mapName
        } else live.mapName
        return live.copy(
            tactic = tactic,
            mapName = mapName,
            draftPlan = plan,
            userStrength = previewStrength(state, live, tactic, mapIndex, plan)
        )
    }

    fun applyTactic(live: LiveMatch, tactic: Tactic, state: GameState): LiveMatch =
        applyPreparation(live, tactic, 0, live.draftPlan, state)

    fun playFirstHalf(live: LiveMatch, rng: Random = Random.Default): LiveMatch {
        if (live.phase != LivePhase.PREGAME) return live
        val p = winChance(live.userStrength, live.opponentStrength)
        val events = mutableListOf<LiveEvent>()
        var us = 0
        var them = 0
        if (live.discipline == Discipline.CS) {
            repeat(12) {
                val won = rng.nextDouble() < p
                if (won) us++ else them++
                events += csRoundEvent(won, us, them, live.userNicks, live.opponentNicks, rng)
            }
            events += LiveEvent("Перерыв: $us:$them. Нужно решение по игре.", LiveEventKind.INFO, us, them)
        } else {
            for (minute in 1..10) {
                val won = rng.nextDouble() < p
                val kills = rng.nextInt(1, 5)
                if (won) us += kills else them += kills
                events += dotaEvent(won, us, them, minute * 2, live.userNicks, live.opponentNicks, rng)
            }
            events += LiveEvent("20:00 — счёт по убийствам $us:$them. Пора определить план на концовку.", LiveEventKind.INFO, us, them)
        }
        return live.copy(
            phase = LivePhase.LIVE,
            scoreUs = us,
            scoreThem = them,
            events = events,
            decisionPending = true
        )
    }

    fun applyDecision(live: LiveMatch, decision: MatchDecision, state: GameState, rng: Random = Random.Default): LiveMatch {
        if (!live.decisionPending) return live
        val mod = when (decision) {
            MatchDecision.TIMEOUT -> 1.2
            MatchDecision.CALM -> 0.5
            MatchDecision.HARSH -> 2.2
        }
        val moraleMod = when (decision) {
            MatchDecision.TIMEOUT -> 0
            MatchDecision.CALM -> 1
            MatchDecision.HARSH -> -4
        }
        val p = winChance(live.userStrength + mod, live.opponentStrength)
        val done = if (live.discipline == Discipline.CS) {
            finishCs(live, p, state, rng)
        } else {
            finishDota(live, p, state, rng)
        }
        return done.copy(
            decisionPending = false,
            decisionTaken = true,
            postMorale = moraleMod
        )
    }

    private fun finishCs(live: LiveMatch, p: Double, state: GameState, rng: Random): LiveMatch {
        var us = live.scoreUs
        var them = live.scoreThem
        val events = live.events.toMutableList()
        val maps = live.mapScores.toMutableList()
        var seriesUs = live.seriesUs
        var seriesThem = live.seriesThem
        var guard = 0
        while (us < 13 && them < 13 && guard < 40) {
            val won = rng.nextDouble() < p
            if (won) us++ else them++
            events += csRoundEvent(won, us, them, live.userNicks, live.opponentNicks, rng)
            guard++
        }
        val mapWon = us > them
        if (mapWon) seriesUs++ else seriesThem++
        maps += MapScore(live.mapName, us, them, mapWon)
        events += LiveEvent(
            "Карта ${live.mapName}: ${if (mapWon) "победа" else "поражение"} $us:$them",
            LiveEventKind.MAP_END, us, them
        )
        var mapNumber = 1
        while (live.isPlayoff && seriesUs < 2 && seriesThem < 2) {
            mapNumber++
            val name = csMaps[(csMaps.indexOf(live.mapName) + mapNumber).mod(csMaps.size)]
            var qu = 0
            var qt = 0
            var g = 0
            while (qu < 13 && qt < 13 && g < 45) {
                if (rng.nextDouble() < p) qu++ else qt++
                g++
            }
            val qWon = qu > qt
            if (qWon) seriesUs++ else seriesThem++
            maps += MapScore(name, qu, qt, qWon)
            events += LiveEvent(
                "Карта $name: ${if (qWon) "победа" else "поражение"} $qu:$qt",
                LiveEventKind.MAP_END, qu, qt
            )
        }
        val updated = live.copy(
            scoreUs = us,
            scoreThem = them,
            seriesUs = seriesUs,
            seriesThem = seriesThem,
            mapScores = maps,
            events = events,
            finished = true,
            phase = LivePhase.RESULT
        )
        val stats = buildStats(updated, state, rng)
        val result = buildResult(updated, state, stats, rng)
        return updated.copy(result = result)
    }

    private fun finishDota(live: LiveMatch, p: Double, state: GameState, rng: Random): LiveMatch {
        var us = live.scoreUs
        var them = live.scoreThem
        val events = live.events.toMutableList()
        val maps = live.mapScores.toMutableList()
        var seriesUs = live.seriesUs
        var seriesThem = live.seriesThem
        for (minute in 11..20) {
            val won = rng.nextDouble() < p
            val kills = rng.nextInt(1, 5)
            if (won) us += kills else them += kills
            events += dotaEvent(won, us, them, minute * 2, live.userNicks, live.opponentNicks, rng)
        }
        val finalP = (p + (us - them) * 0.006).coerceIn(0.05, 0.95)
        val mapWon = rng.nextDouble() < finalP
        if (mapWon) seriesUs++ else seriesThem++
        maps += MapScore(live.mapName, us, them, mapWon)
        events += LiveEvent(
            if (mapWon) "Финальный файт у Древнего — мы забираем игру $us:$them!"
            else "Соперник дожимает у Древнего. Поражение $us:$them.",
            LiveEventKind.MAP_END, us, them
        )
        var gameNumber = 1
        while (live.isPlayoff && seriesUs < 2 && seriesThem < 2) {
            gameNumber++
            var qu = 0
            var qt = 0
            repeat(20) {
                val w = rng.nextDouble() < p
                val k = rng.nextInt(1, 5)
                if (w) qu += k else qt += k
            }
            val qWon = rng.nextDouble() < (p + (qu - qt) * 0.004).coerceIn(0.05, 0.95)
            if (qWon) seriesUs++ else seriesThem++
            maps += MapScore("Игра $gameNumber", qu, qt, qWon)
            events += LiveEvent(
                "Игра $gameNumber: ${if (qWon) "победа" else "поражение"} ($qu:$qt по убийствам)",
                LiveEventKind.MAP_END, qu, qt
            )
        }
        val updated = live.copy(
            scoreUs = us,
            scoreThem = them,
            seriesUs = seriesUs,
            seriesThem = seriesThem,
            mapScores = maps,
            events = events,
            finished = true,
            phase = LivePhase.RESULT
        )
        val stats = buildStats(updated, state, rng)
        val result = buildResult(updated, state, stats, rng)
        return updated.copy(result = result)
    }

    private fun csRoundEvent(
        usWon: Boolean,
        us: Int,
        them: Int,
        users: List<String>,
        opponents: List<String>,
        rng: Random
    ): LiveEvent {
        val nick = if (usWon) users.random(rng) else opponents.random(rng)
        val text = if (usWon) {
            listOf(
                "$nick забирает клатч и раунд!",
                "$nick открывает счёт хедшотом.",
                "Тимфайт в нашу пользу — $nick забирает двоих.",
                "Соперник ошибся на ретейке, $nick наказывает.",
                "$nick с AWP снимает двоих на длинной.",
                "Размен 5 на 4 — раунд остаётся нам."
            ).random(rng)
        } else {
            listOf(
                "$nick выигрывает дуэль и забирает раунд.",
                "Неудачный ретейк — раунд у соперника.",
                "$nick ловит нас на выходе с базы.",
                "Соперник забирает пистолетный раунд.",
                "Размен 1 в 2 — инициатива у соперника.",
                "$nick закрывает 1v1 и сравнивает счёт."
            ).random(rng)
        }
        return LiveEvent(text, if (usWon) LiveEventKind.SCORE else LiveEventKind.INFO, us, them)
    }

    private fun dotaEvent(
        usWon: Boolean,
        usKills: Int,
        themKills: Int,
        minute: Int,
        users: List<String>,
        opponents: List<String>,
        rng: Random
    ): LiveEvent {
        val nick = if (usWon) users.random(rng) else opponents.random(rng)
        val text = if (usWon) {
            listOf(
                "$nick забирает первую кровь на миду.",
                "Размен 4 на 2 в нашу пользу — сносим T1.",
                "$nick ловит врага в лесу и забирает Рошана.",
                "Наш керри фармит без давления, преимущество растёт.",
                "Драка у руны — забираем два килла.",
                "$nick делает дабл-килл в тимфайте."
            ).random(rng)
        } else {
            listOf(
                "Соперник забирает Рошана — инициатива у них.",
                "$nick ловит нашего саппорта в лесу.",
                "Проигрываем драку 2 на 4 у средней линии.",
                "Соперник сносит T2 и уходит на хайграунд.",
                "$nick делает тройное убийство.",
                "Неудачный ганг — теряем двоих."
            ).random(rng)
        }
        return LiveEvent("$minute:00 · $text", if (usWon) LiveEventKind.SCORE else LiveEventKind.INFO, usKills, themKills)
    }

    private fun buildStats(live: LiveMatch, state: GameState, rng: Random): List<PlayerStat> {
        val stats = mutableListOf<PlayerStat>()
        val roundsUs = live.mapScores.sumOf { it.us }
        val roundsThem = live.mapScores.sumOf { it.them }
        if (live.discipline == Discipline.CS) {
            live.userNicks.forEach { nick ->
                val k = (roundsUs * rng.nextDouble(0.55, 1.0) + 3).toInt()
                val dth = (roundsThem * rng.nextDouble(0.5, 0.95) + 2).toInt()
                val a = (k * rng.nextDouble(0.2, 0.45)).toInt()
                stats += PlayerStat(nick, k, dth, a, (0.65 + (k - dth) * 0.035 + rng.nextDouble(0.0, 0.35)).coerceIn(0.3, 1.9), true)
            }
            live.opponentNicks.forEach { nick ->
                val k = (roundsThem * rng.nextDouble(0.55, 1.0) + 3).toInt()
                val dth = (roundsUs * rng.nextDouble(0.5, 0.95) + 2).toInt()
                val a = (k * rng.nextDouble(0.2, 0.45)).toInt()
                stats += PlayerStat(nick, k, dth, a, (0.65 + (k - dth) * 0.035 + rng.nextDouble(0.0, 0.35)).coerceIn(0.3, 1.9), false)
            }
        } else {
            val totalUs = live.mapScores.sumOf { it.us }.coerceAtLeast(5)
            val totalThem = live.mapScores.sumOf { it.them }.coerceAtLeast(5)
            fun shares(total: Int): List<Int> {
                val weights = List(5) { rng.nextDouble(0.7, 1.4) }
                val sum = weights.sum()
                val raw = weights.map { (it / sum * total).roundToInt() }
                val diff = total - raw.sum()
                return raw.toMutableList().also { it[0] += diff }
            }
            val usKills = shares(totalUs)
            val themKills = shares(totalThem)
            live.userNicks.forEachIndexed { i, nick ->
                val k = usKills.getOrElse(i) { 1 }
                val dth = themKills.getOrElse((i + 2) % 5) { 1 }
                val a = (k * rng.nextDouble(1.0, 2.2)).toInt()
                stats += PlayerStat(nick, k, dth, a, (0.7 + (k + a * 0.4 - dth * 0.6) / 22.0 + rng.nextDouble(0.0, 0.3)).coerceIn(0.3, 2.2), true)
            }
            live.opponentNicks.forEachIndexed { i, nick ->
                val k = themKills.getOrElse(i) { 1 }
                val dth = usKills.getOrElse((i + 1) % 5) { 1 }
                val a = (k * rng.nextDouble(1.0, 2.2)).toInt()
                stats += PlayerStat(nick, k, dth, a, (0.7 + (k + a * 0.4 - dth * 0.6) / 22.0 + rng.nextDouble(0.0, 0.3)).coerceIn(0.3, 2.2), false)
            }
        }
        return stats
    }

    private fun buildResult(
        live: LiveMatch,
        state: GameState,
        stats: List<PlayerStat>,
        rng: Random
    ): MatchResult {
        val win = live.seriesUs > live.seriesThem
        val tier = state.leagueFor(live.discipline).tier
        val money = when {
            live.isCup && live.cupRound >= 2 -> if (win) 65_000 + tier * 18_000 else 12_000 + tier * 3_000
            live.isCup -> if (win) 24_000 + tier * 10_000 else 6_000 + tier * 2_000
            live.isPlayoff -> if (win) 22_000 + tier * 12_000 else 7_000 + tier * 2_000
            else -> if (win) 8_000 + tier * 4_000 else 2_000 + tier * 500
        }
        val fans = when {
            live.isCup && live.cupRound >= 2 -> if (win) 3_500 + tier * 900 else 150
            live.isCup -> if (win) 1_800 + tier * 500 else 100
            live.isPlayoff -> if (win) 2_000 + tier * 600 else 80
            win -> 450 + tier * 250
            else -> 80
        }
        val mvp = stats.maxByOrNull { it.rating }
        return MatchResult(
            id = UUID.randomUUID().toString(),
            discipline = live.discipline,
            season = state.season,
            week = state.week,
            opponentId = live.opponentId,
            opponentName = live.opponentName,
            opponentStrength = live.opponentStrength,
            isPlayoff = live.isPlayoff,
            win = win,
            seriesUs = live.seriesUs,
            seriesThem = live.seriesThem,
            maps = live.mapScores,
            mvp = mvp?.nick ?: "—",
            mvpIsUser = mvp?.isUser ?: false,
            stats = stats,
            money = money,
            fans = fans,
            dateLabel = "S${state.season} · W${state.week}",
            isCup = live.isCup,
            cupRound = live.cupRound
        )
    }
}
