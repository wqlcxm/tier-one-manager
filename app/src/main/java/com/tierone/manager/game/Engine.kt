package com.tierone.manager.game

import com.tierone.manager.data.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

object Engine {

    private fun GameState.withNews(vararg lines: String): GameState =
        copy(news = (news + lines).takeLast(60))

    private fun GameState.recalcBest(): GameState {
        val best = (csTeam.players + dotaTeam.players).maxOfOrNull { it.ovr() } ?: 0
        return copy(bestOvr = max(bestOvr, best))
    }

    fun newGame(orgName: String, region: String, logoSeed: Int, difficulty: Int, rng: Random = Random.Default): GameState {
        val cs = Generator.generateTeam(Discipline.CS, orgName, rng)
        val dota = Generator.generateTeam(Discipline.DOTA, orgName, rng)
        val csLeague = Generator.generateLeague(Discipline.CS, 0, orgName, difficulty, rng)
        val dotaLeague = Generator.generateLeague(Discipline.DOTA, 0, orgName, difficulty, rng)
        val offers = Generator.generateSponsorOffers(0, 5_000, rng)
        val money = 350_000 - difficulty * 50_000
        val meta = Generator.generateMeta(1, rng)
        return GameState(
            org = Org(name = orgName, region = region, logoSeed = logoSeed, fans = 5_000),
            money = money,
            season = 1,
            week = 1,
            actionPoints = 4,
            difficulty = difficulty,
            csTeam = cs,
            dotaTeam = dota,
            facilities = Facilities(),
            staff = emptyList(),
            staffCandidates = Generator.generateStaffCandidates(0, rng),
            sponsor = offers.firstOrNull(),
            sponsorOffers = offers.drop(1),
            csLeague = csLeague,
            dotaLeague = dotaLeague,
            csCup = Generator.generateCup(Discipline.CS, csLeague, rng),
            dotaCup = Generator.generateCup(Discipline.DOTA, dotaLeague, rng),
            meta = meta,
            market = Generator.generateMarket(0, 1, rng),
            pendingEvent = null,
            objectives = Generator.generateObjectives(0, rng),
            weekStats = WeekStats(),
            weekResults = emptyList(),
            playedThisWeek = emptyList(),
            news = Generator.initialNews(orgName) +
                "Подписан первый спонсор: ${offers.firstOrNull()?.name ?: "—"}. " +
                "Кубок лиги: полуфиналы на 3-й неделе, финал — на 5-й.",
            bestOvr = (cs.players + dota.players).maxOfOrNull { it.ovr() } ?: 0
        )
    }

    /**
     * Migration for saves, созданных до «Сезона 2.0». Новые поля имеют значения
     * по умолчанию, а отсутствующие кубки и мету можно безопасно восстановить.
     */
    fun ensureSystems(state: GameState, rng: Random = Random.Default): GameState {
        var updated = state
        if (updated.meta.weeksLeft <= 0) {
            updated = updated.copy(meta = Generator.generateMeta(updated.meta.patchNumber + 1, rng))
        }
        if (updated.csCup == null) {
            var cup = Generator.generateCup(Discipline.CS, updated.csLeague, rng)
            if (updated.week > 3) cup = processCupWeek(cup, 3, rng)
            if (updated.week > 5) cup = processCupWeek(cup, 5, rng)
            updated = updated.copy(csCup = cup)
        }
        if (updated.dotaCup == null) {
            var cup = Generator.generateCup(Discipline.DOTA, updated.dotaLeague, rng)
            if (updated.week > 3) cup = processCupWeek(cup, 3, rng)
            if (updated.week > 5) cup = processCupWeek(cup, 5, rng)
            updated = updated.copy(dotaCup = cup)
        }
        return updated
    }

    // -----------------------------------------------------------------------
    // Weekly actions
    // -----------------------------------------------------------------------

    fun trainTeam(state: GameState, discipline: Discipline, attr: Attribute, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) return state
        val team = state.teamFor(discipline)
        val coach = state.staffFor(StaffRole.COACH)
        val updated = team.players.map { p ->
            var gain = trainingGain(p, state, coach, rng)
            if (gain > 0 && attr == state.meta.favoredAttribute) gain += 1
            val np = if (gain > 0) p.withAttribute(attr, gain) else p
            np.copy(
                fatigue = (np.fatigue + fatigueGain(state, 7, p)).coerceIn(0, 100),
                form = (np.form + 1).coerceIn(0, 100)
            )
        }
        val newTeam = team.copy(players = updated, chemistry = (team.chemistry + 1).coerceIn(0, 100))
        val newState = state.withTeam(discipline, newTeam).copy(
            actionPoints = state.actionPoints - 1,
            weekStats = state.weekStats.copy(trainings = state.weekStats.trainings + 1)
        ).recalcBest().withNews("${discipline.label}: командная тренировка «${attr.label}».")
        return checkAchievements(newState)
    }

    fun trainPlayer(state: GameState, playerId: String, attr: Attribute, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) return state
        val team = state.teamFor(state.playerById(playerId)?.discipline ?: return state)
        val player = team.players.firstOrNull { it.id == playerId } ?: return state
        val canGrow = player.ovr() < player.potential
        val metaBonus = if (canGrow && attr == state.meta.favoredAttribute) 1 else 0
        val gain = if (canGrow) 2 + rng.nextInt(0, 3) + metaBonus else 0
        val np = player.withAttribute(attr, gain).copy(
            fatigue = (player.fatigue + fatigueGain(state, 10, player)).coerceIn(0, 100),
            form = (player.form + 1).coerceIn(0, 100)
        )
        val updated = team.players.map { if (it.id == player.id) np else it }
        val newState = state.withTeam(team.discipline, team.copy(players = updated)).copy(
            actionPoints = state.actionPoints - 1,
            weekStats = state.weekStats.copy(trainings = state.weekStats.trainings + 1)
        ).recalcBest().withNews("${player.nick}: индивидуальная работа над «${attr.label}».")
        return checkAchievements(newState)
    }

    fun restTeam(state: GameState, discipline: Discipline, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) return state
        val team = state.teamFor(discipline)
        val psych = state.staffFor(StaffRole.PSYCHOLOGIST)
        val psychBonus = 1.0 + (state.facilities.psychology - 1) * 0.1 + (psych?.let { (it.rating - 50) / 150.0 } ?: 0.0)
        val recovery = (22 * psychBonus).roundToInt()
        val updated = team.players.map {
            it.copy(
                fatigue = (it.fatigue - recovery).coerceIn(0, 100),
                morale = (it.morale + (4 * psychBonus).roundToInt()).coerceIn(0, 100),
                form = (it.form + 1).coerceIn(0, 100)
            )
        }
        return state.withTeam(discipline, team.copy(players = updated)).copy(
            actionPoints = state.actionPoints - 1,
            weekStats = state.weekStats.copy(rests = state.weekStats.rests + 1)
        ).withNews("${discipline.label}: состав отдохнул и восстановил силы.")
    }

    fun scrim(state: GameState, discipline: Discipline, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) return state
        val team = state.teamFor(discipline)
        val updated = team.players.map {
            it.copy(
                fatigue = (it.fatigue + fatigueGain(state, 8, it)).coerceIn(0, 100),
                form = (it.form + 2).coerceIn(0, 100)
            )
        }
        val bonus = min(4.0, team.matchBonus + 1.5)
        val newTeam = team.copy(players = updated, chemistry = (team.chemistry + 3).coerceIn(0, 100), matchBonus = bonus)
        return state.withTeam(discipline, newTeam).copy(
            actionPoints = state.actionPoints - 1,
            weekStats = state.weekStats.copy(scrims = state.weekStats.scrims + 1)
        ).withNews("${discipline.label}: проведены скримы, получено преимущество к следующему матчу.")
    }

    fun mediaDay(state: GameState, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) return state
        val extra = (state.org.fans * 0.02).toInt()
        val fans = 250 + extra + state.leagueFor(Discipline.CS).tier * 150
        val money = 4_000 + state.leagueFor(Discipline.CS).tier * 2_000
        fun lift(team: Team): Team = team.copy(players = team.players.map {
            it.copy(
                fatigue = (it.fatigue + 3).coerceIn(0, 100),
                morale = (it.morale + 1).coerceIn(0, 100)
            )
        })
        return state.copy(
            csTeam = lift(state.csTeam),
            dotaTeam = lift(state.dotaTeam),
            org = state.org.copy(fans = state.org.fans + fans),
            money = state.money + money,
            actionPoints = state.actionPoints - 1,
            weekStats = state.weekStats.copy(media = state.weekStats.media + 1)
        ).withNews("Медиа-день: +${fans} фанатов, +${money.moneyFull()}.")
    }

    fun scoutMarket(state: GameState, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) return state
        val tier = max(state.csLeague.tier, state.dotaLeague.tier)
        val scoutRating = state.staffFor(StaffRole.SCOUT)?.rating ?: 50
        val market = Generator.generateMarket(tier, state.facilities.academy, rng, scoutRating = scoutRating)
        return state.copy(
            market = market,
            actionPoints = state.actionPoints - 1
        ).withNews("Скаут обновил рынок: появились новые кандидаты.")
    }

    fun scoutCandidate(state: GameState, playerId: String): GameState {
        if (state.actionPoints <= 0) return state
        var changed = false
        val market = state.market.map { c ->
            if (c.player.id == playerId && c.scoutLevel < 2) {
                changed = true
                c.copy(scoutLevel = c.scoutLevel + 1, player = c.player.copy(scouted = true))
            } else c
        }
        if (!changed) return state
        return state.copy(market = market, actionPoints = state.actionPoints - 1)
            .withNews("Скаут собрал подробное досье на игрока.")
    }

    fun signPlayer(state: GameState, playerId: String): GameState =
        signPlayerAt(state, playerId, null)

    private fun signPlayerAt(state: GameState, playerId: String, priceOverride: Int?): GameState {
        val candidate = state.market.firstOrNull { it.player.id == playerId } ?: return state
        val d = candidate.player.discipline
        val team = state.teamFor(d)
        if (team.players.size >= 8) return state
        val price = priceOverride ?: candidate.price
        if (state.money < price) return state
        val player = candidate.player.copy(contractWeeks = 52, morale = max(60, candidate.player.morale))
        val newTeam = team.copy(players = team.players + player, chemistry = (team.chemistry - 1).coerceIn(0, 100))
        val newState = state.withTeam(d, newTeam).copy(
            money = state.money - price,
            market = state.market.filterNot { it.player.id == playerId },
            weekStats = state.weekStats.copy(signings = state.weekStats.signings + 1)
        ).recalcBest().withNews("${player.nick} подписан в состав ${d.label} за ${price.moneyFull()}.")
        return checkAchievements(newState)
    }

    /**
     * Попытка сбить цену. Агент может согласиться на 82% стоимости,
     * особенно если у клуба хороший скаут и игрок не является суперзвездой.
     */
    fun negotiateSign(state: GameState, playerId: String, rng: Random = Random.Default): GameState {
        if (state.actionPoints <= 0) {
            return state.withNews("Переговоры: не осталось очков действий.")
        }
        val candidate = state.market.firstOrNull { it.player.id == playerId } ?: return state
        val team = state.teamFor(candidate.player.discipline)
        val offer = (candidate.price * 0.82).toInt().coerceAtLeast(1_000)
        if (team.players.size >= 8) {
            return state.withNews("Переговоры: состав полон, сначала освободите место.")
        }
        if (state.money < offer) {
            return state.withNews("Переговоры: клуб не может позволить даже сниженную цену ${offer.moneyFull()}.")
        }
        val scout = state.staffFor(StaffRole.SCOUT)?.rating ?: 50
        val chance = (0.58 + (scout - 50) / 300.0 + candidate.scoutLevel * 0.05 -
            (candidate.player.ovr() - 65) / 180.0).coerceIn(0.2, 0.88)
        val spent = state.copy(actionPoints = (state.actionPoints - 1).coerceAtLeast(0))
        return if (rng.nextDouble() < chance) {
            signPlayerAt(spent, playerId, offer).withNews(
                "Переговоры успешны: ${candidate.player.nick} подписан со скидкой 18% за ${offer.moneyFull()}."
            )
        } else {
            spent.withNews("Агент ${candidate.player.nick} отказал в скидке. Придётся платить полную цену.")
        }
    }

    fun acceptIncomingOffer(state: GameState, offerId: String): GameState {
        val offer = state.incomingOffers.firstOrNull { it.id == offerId } ?: return state
        val player = state.playerById(offer.playerId)
        if (player == null) {
            return state.copy(incomingOffers = state.incomingOffers.filterNot { it.id == offerId })
        }
        val d = player.discipline
        val team = state.teamFor(d)
        if (team.players.size <= 5) {
            return state.withNews("Нельзя продать ${player.nick}: в составе должно остаться минимум 5 игроков.")
        }
        if (team.players.count { it.role == player.role } <= 1) {
            return state.withNews("Нельзя продать ${player.nick}: позиция ${player.role.short} останется без игрока.")
        }
        val newTeam = team.copy(
            players = team.players.filterNot { it.id == player.id },
            chemistry = (team.chemistry - 2).coerceIn(0, 100)
        )
        val newState = state.withTeam(d, newTeam).copy(
            money = state.money + offer.amount,
            incomingOffers = state.incomingOffers.filterNot { it.id == offerId }
        ).withNews(
            "${player.nick} продан в ${offer.clubName} за ${offer.amount.moneyFull()}. Предложение закрыто."
        )
        return checkAchievements(newState)
    }

    fun rejectIncomingOffer(state: GameState, offerId: String): GameState {
        val offer = state.incomingOffers.firstOrNull { it.id == offerId } ?: return state
        return state.copy(incomingOffers = state.incomingOffers.filterNot { it.id == offerId })
            .withNews("Предложение по ${offer.playerNick} отклонено.")
    }

    fun sellPlayer(state: GameState, playerId: String): GameState {
        val player = state.playerById(playerId) ?: return state
        val d = player.discipline
        val team = state.teamFor(d)
        if (team.players.size <= 5) return state
        if (team.players.count { it.role == player.role } <= 1) return state
        val price = (player.marketValue() * 0.7).toInt()
        return state.withTeam(
            d,
            team.copy(
                players = team.players.filterNot { it.id == playerId },
                chemistry = (team.chemistry - 2).coerceIn(0, 100)
            )
        ).copy(money = state.money + price)
            .withNews("${player.nick} продан за ${price.moneyFull()}. Состав покинул игрок.")
    }

    fun upgradeFacility(state: GameState, type: FacilityType, rng: Random = Random.Default): GameState {
        val level = state.facilities.level(type)
        if (level >= 5) return state
        val cost = facilityUpgradeCost(type, level)
        if (state.money < cost) return state
        val newState = state.copy(
            money = state.money - cost,
            facilities = state.facilities.withLevel(type, level + 1),
            weekStats = state.weekStats.copy(upgrades = state.weekStats.upgrades + 1)
        ).withNews("${type.label} улучшен до ${level + 1} уровня.")
        return checkAchievements(newState)
    }

    fun hireStaff(state: GameState, staffId: String): GameState {
        val candidate = state.staffCandidates.firstOrNull { it.id == staffId } ?: return state
        val cost = candidate.salary * 6
        if (state.money < cost) return state
        val staff = state.staff.filterNot { it.role == candidate.role } + candidate
        return state.copy(
            money = state.money - cost,
            staff = staff,
            staffCandidates = state.staffCandidates.filterNot { it.id == staffId }
        ).withNews("Нанят ${candidate.role.label.lowercase()}: ${candidate.name} (рейтинг ${candidate.rating}).")
    }

    fun signSponsor(state: GameState, index: Int): GameState {
        val offer = state.sponsorOffers.getOrNull(index) ?: return state
        return state.copy(
            sponsor = offer,
            sponsorOffers = state.sponsorOffers.filterIndexed { i, _ -> i != index },
            money = state.money + offer.signingBonus
        ).recalcBest().withNews("Подписан спонсор ${offer.name}: ${offer.weekly.moneyFull()} в неделю.")
    }

    fun setTactic(state: GameState, discipline: Discipline, tactic: Tactic): GameState =
        state.withTeam(discipline, state.teamFor(discipline).copy(tactic = tactic))

    fun setSettings(state: GameState, sound: Boolean, vibration: Boolean): GameState =
        state.copy(settings = Settings(sound = sound, vibration = vibration))

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    fun chooseEvent(state: GameState, index: Int): GameState {
        val event = state.pendingEvent ?: return state
        val choice = event.choices.getOrNull(index) ?: return state
        val e = choice.effects

        fun applyTeam(team: Team, morale: Int, fatigue: Int, chemistry: Int, form: Int): Team {
            val players = team.players.map {
                it.copy(
                    morale = (it.morale + morale).coerceIn(0, 100),
                    fatigue = (it.fatigue + fatigue).coerceIn(0, 100),
                    form = (it.form + form).coerceIn(0, 100)
                )
            }
            return team.copy(players = players, chemistry = (team.chemistry + chemistry).coerceIn(0, 100))
        }

        val newState = state.copy(
            csTeam = applyTeam(state.csTeam, e.moraleCs, e.fatigueCs, e.chemistryCs, e.formCs),
            dotaTeam = applyTeam(state.dotaTeam, e.moraleDota, e.fatigueDota, e.chemistryDota, e.formDota),
            money = state.money + e.money,
            org = state.org.copy(fans = max(0, state.org.fans + e.fans)),
            actionPoints = (state.actionPoints + e.ap).coerceIn(0, 6),
            pendingEvent = null
        )
        val withNews = if (e.news != null) newState.withNews(e.news) else newState
        return checkAchievements(withNews)
    }

    // -----------------------------------------------------------------------
    // Match week
    // -----------------------------------------------------------------------

    fun upcomingMatchups(state: GameState): List<Matchup> {
        if (state.week !in 1..7) return emptyList()
        val matchups = mutableListOf<Matchup>()
        Discipline.entries.forEach { d ->
            val cupRound = when (state.week) {
                3 -> 1
                5 -> 2
                else -> 0
            }
            if (cupRound > 0) {
                val cup = state.cupFor(d)
                val key = "${d.name}:$cupRound"
                val series = cup?.playoffs?.firstOrNull {
                    it.round == cupRound &&
                        (it.teamA == "PLAYER" || it.teamB == "PLAYER") &&
                        it.winner == null
                }
                if (cup != null && series != null && key !in state.playedCupMatches) {
                    val oppId = if (series.teamA == "PLAYER") series.teamB else series.teamA
                    val opp = cup.team(oppId)
                    if (opp != null) {
                        matchups += Matchup(
                            discipline = d,
                            opponent = opp,
                            isPlayoff = true,
                            series = series,
                            isCup = true,
                            cupRound = cupRound
                        )
                    }
                }
            }
            if (d in state.playedThisWeek) return@forEach
            val league = state.leagueFor(d)
            if (state.week <= 5) {
                val oppId = league.schedule.getOrNull(state.week - 1)
                if (oppId != null) {
                    val opp = league.team(oppId)
                    if (opp != null) matchups += Matchup(d, opp, false)
                }
            } else {
                val round = if (state.week == 6) 1 else 2
                val series = league.playoffs.firstOrNull {
                    it.round == round && (it.teamA == "PLAYER" || it.teamB == "PLAYER") && it.winner == null
                }
                if (series != null) {
                    val oppId = if (series.teamA == "PLAYER") series.teamB else series.teamA
                    val opp = league.team(oppId)
                    if (opp != null) matchups += Matchup(d, opp, true, series)
                }
            }
        }
        return matchups
    }

    fun finishMatch(state: GameState, live: LiveMatch, rng: Random = Random.Default): GameState {
        val result = live.result ?: return state
        if (state.weekResults.any { it.id == result.id }) return state
        val d = result.discipline
        val team = state.teamFor(d)
        val lineupIds = team.lineup().map { it.id }.toSet()
        val updatedPlayers = team.players.map { p ->
            val starter = p.id in lineupIds
            p.copy(
                morale = (p.morale + (if (result.win) 4 else -4) + live.postMorale).coerceIn(0, 100),
                form = (p.form + if (result.win) 4 else -3).coerceIn(0, 100),
                fatigue = (p.fatigue + if (starter) 7 else 3).coerceIn(0, 100)
            )
        }
        val newTeam = team.copy(
            players = updatedPlayers,
            chemistry = (team.chemistry + if (result.win) 1 else -1).coerceIn(0, 100),
            matchBonus = 0.0
        )
        val cupBefore = if (result.isCup) state.cupFor(d) else null
        val newCup = if (result.isCup && cupBefore != null) applyCupResult(cupBefore, result) else null
        val cupWon = newCup != null && result.cupRound >= 2 &&
            newCup.champion == "PLAYER" && cupBefore?.champion != "PLAYER"
        var newState = state.withTeam(d, newTeam)
        if (!result.isCup) {
            val newLeague = if (result.isPlayoff) {
                applyPlayoffResult(state.leagueFor(d), result)
            } else {
                applyRegularResult(state.leagueFor(d), result)
            }
            newState = newState.withLeague(d, newLeague)
        }
        if (newCup != null) newState = newState.withCup(d, newCup)
        val cupPrize = if (cupWon) 60_000 + (newCup?.tier ?: 0) * 20_000 else 0
        val cupFans = if (cupWon) 3_000 else 0
        val cupTrophy = if (cupWon) {
            "Кубок лиги · ${d.label} · Сезон ${state.season}"
        } else null
        newState = newState.copy(
            money = state.money + result.money + cupPrize,
            totalEarned = state.totalEarned + result.money + cupPrize,
            org = state.org.copy(fans = state.org.fans + result.fans + cupFans),
            weekResults = state.weekResults + result,
            playedThisWeek = if (result.isCup) state.playedThisWeek else (state.playedThisWeek + d).distinct(),
            playedCupMatches = if (result.isCup) {
                (state.playedCupMatches + "${d.name}:${result.cupRound}").distinct()
            } else state.playedCupMatches,
            matchHistory = (state.matchHistory + result).takeLast(80),
            trophies = if (cupTrophy != null) state.trophies + cupTrophy else state.trophies,
            careerWins = state.careerWins + if (result.win) 1 else 0,
            careerLosses = state.careerLosses + if (result.win) 0 else 1,
            weekStats = state.weekStats.copy(
                matches = state.weekStats.matches + 1,
                wins = state.weekStats.wins + if (result.win) 1 else 0
            )
        ).recalcBest()
        val scored = "${result.seriesUs}:${result.seriesThem}"
        val stage = when {
            result.isCup && result.cupRound >= 2 -> " (финал кубка)"
            result.isCup -> " (полуфинал кубка)"
            result.isPlayoff -> " (плей-офф)"
            else -> ""
        }
        val news = mutableListOf(
            "${d.label}: ${if (result.win) "победа" else "поражение"} над ${result.opponentName} $scored$stage"
        )
        if (cupWon) {
            news += "Кубок лиги ${d.label} — наш! Трофей добавлен в зал славы, бонус ${cupPrize.moneyFull()}."
        }
        return checkAchievements(newState.withNews(*news.toTypedArray()))
    }

    private fun applyRegularResult(league: League, result: MatchResult): League {
        val teams = league.teams.map { t ->
            when {
                t.isPlayer -> t.copy(
                    wins = t.wins + if (result.win) 1 else 0,
                    losses = t.losses + if (result.win) 0 else 1,
                    mapDiff = t.mapDiff + result.seriesUs - result.seriesThem
                )
                t.id == result.opponentId -> t.copy(
                    wins = t.wins + if (result.win) 0 else 1,
                    losses = t.losses + if (result.win) 1 else 0,
                    mapDiff = t.mapDiff + result.seriesThem - result.seriesUs
                )
                else -> t
            }
        }
        return league.copy(teams = teams)
    }

    private fun applyPlayoffResult(league: League, result: MatchResult): League {
        val round = if (result.week == 6) 1 else 2
        val playoffs = league.playoffs.map { s ->
            if (s.round == round &&
                ((s.teamA == "PLAYER" && s.teamB == result.opponentId) ||
                    (s.teamB == "PLAYER" && s.teamA == result.opponentId)) &&
                s.winner == null
            ) {
                val aIsPlayer = s.teamA == "PLAYER"
                val winsA = if (aIsPlayer) result.seriesUs else result.seriesThem
                val winsB = if (aIsPlayer) result.seriesThem else result.seriesUs
                val winner = if (winsA >= 2) s.teamA else if (winsB >= 2) s.teamB else null
                s.copy(winsA = winsA, winsB = winsB, winner = winner)
            } else s
        }
        return league.copy(playoffs = playoffs)
    }

    private fun applyCupResult(cup: Cup, result: MatchResult): Cup {
        val round = result.cupRound
        val playoffs = cup.playoffs.map { s ->
            if (s.round == round &&
                ((s.teamA == "PLAYER" && s.teamB == result.opponentId) ||
                    (s.teamB == "PLAYER" && s.teamA == result.opponentId)) &&
                s.winner == null
            ) {
                val aIsPlayer = s.teamA == "PLAYER"
                val winsA = if (aIsPlayer) result.seriesUs else result.seriesThem
                val winsB = if (aIsPlayer) result.seriesThem else result.seriesUs
                val winner = if (winsA >= 2) s.teamA else if (winsB >= 2) s.teamB else null
                s.copy(winsA = winsA, winsB = winsB, winner = winner)
            } else s
        }
        val final = playoffs.firstOrNull { it.round >= 2 }
        return cup.copy(
            playoffs = playoffs,
            champion = if (round >= 2) final?.winner ?: cup.champion else cup.champion
        )
    }

    fun finishWeek(state: GameState, rng: Random = Random.Default): GameState {
        if (upcomingMatchups(state).isNotEmpty()) return state
        val currentWeek = state.week
        var updated = state
        Discipline.entries.forEach { d ->
            var league = updated.leagueFor(d)
            league = when {
                currentWeek <= 5 -> simulateAiRegular(league, state, rng)
                currentWeek == 6 -> processPlayoffs(league, 6, rng)
                currentWeek == 7 -> processPlayoffs(league, 7, rng)
                else -> league
            }
            updated = updated.withLeague(d, league)
            var cup = updated.cupFor(d)
            if (cup != null) {
                cup = processCupWeek(cup, currentWeek, rng)
                updated = updated.withCup(d, cup)
            }
        }

        val tier = max(updated.csLeague.tier, updated.dotaLeague.tier)
        val activeSponsor = updated.sponsor
        val sponsorIncome = activeSponsor?.weekly ?: 0
        val merch = (updated.org.fans * 0.10 * (1.0 + (updated.facilities.merch - 1) * 0.15)).toInt()
        val stipend = 12_000 + tier * 4_000
        val salaries = (updated.csTeam.players + updated.dotaTeam.players).sumOf { it.salary } +
            updated.staff.sumOf { it.salary }
        val objectiveLines = mutableListOf<String>()
        var objectiveMoney = 0
        var objectiveFans = 0
        updated.objectives.forEach { obj ->
            if (updated.weekStats.progress(obj.type) >= obj.target) {
                objectiveMoney += obj.rewardMoney
                objectiveFans += obj.rewardFans
                objectiveLines += "✓ ${obj.text} (+${obj.rewardMoney.moneyFull()}, +${obj.rewardFans} фанатов)"
            }
        }
        val income = sponsorIncome + merch + stipend + objectiveMoney
        val fanDelta = objectiveFans + updated.weekResults.sumOf { it.fans }
        val net = income - salaries
        var money = updated.money + net
        val notes = mutableListOf<String>()
        notes += "Спонсор: ${sponsorIncome.moneyFull()}"
        notes += "Мерч: ${merch.moneyFull()}"
        notes += "Стипендия лиги: ${stipend.moneyFull()}"
        notes += "Зарплаты: −${salaries.moneyFull()}"
        if (currentWeek == 5) {
            updated.csCup?.champion?.let { champ ->
                notes += "Кубок CS2: ${updated.csCup?.team(champ)?.name ?: "—"}."
            }
            updated.dotaCup?.champion?.let { champ ->
                notes += "Кубок Dota 2: ${updated.dotaCup?.team(champ)?.name ?: "—"}."
            }
        }
        if (activeSponsor != null) {
            val weeks = activeSponsor.weeks - 1
            updated = if (weeks <= 0) {
                notes += "Контракт спонсора ${activeSponsor.name} истёк."
                updated.copy(sponsor = null)
            } else updated.copy(sponsor = activeSponsor.copy(weeks = weeks))
        }
        if (money < 0) {
            money += 90_000
            notes += "Инвестор выделил экстренные $90 000, чтобы клуб не остановился."
            updated = updated.copy(org = updated.org.copy(fans = max(0, updated.org.fans - 300)))
        }
        val report = WeekReport(
            season = updated.season,
            week = currentWeek,
            results = updated.weekResults,
            income = income,
            expenses = salaries,
            fanDelta = fanDelta,
            objectiveLines = objectiveLines,
            notes = notes
        )
        updated = updated.copy(
            money = money,
            org = updated.org.copy(fans = updated.org.fans + objectiveFans)
        )

        if (currentWeek >= 7) {
            return endSeason(updated, rng, report)
        }

        val nextWeek = currentWeek + 1
        val metaAdvance = advanceMeta(updated.meta, rng)
        val activeOffers = updated.incomingOffers
            .map { it.copy(weeksLeft = it.weeksLeft - 1) }
            .filter { it.weeksLeft > 0 }
        var next = updated.copy(
            week = nextWeek,
            actionPoints = 4,
            weekStats = WeekStats(),
            weekResults = emptyList(),
            playedThisWeek = emptyList(),
            playedCupMatches = emptyList(),
            meta = metaAdvance.first,
            incomingOffers = activeOffers,
            objectives = Generator.generateObjectives(tier, rng),
            lastReport = report
        )
        if (metaAdvance.second) {
            next = next.withNews("Новая мета: ${next.meta.title}. В фаворе «${next.meta.favoredTactic.styleLabel()}» и ${next.meta.favoredAttribute.label}.")
        }
        if (nextWeek == 6) {
            next = next.copy(
                csLeague = createSemis(next.csLeague),
                dotaLeague = createSemis(next.dotaLeague)
            )
        }
        next = maybeGenerateTransferOffer(next, rng)
        next = next.copy(pendingEvent = Generator.generateEvent(next, rng))
        return checkAchievements(next.withNews("Неделя $nextWeek началась. Запланируйте подготовку."))
    }

    private fun advanceMeta(meta: MetaState, rng: Random): Pair<MetaState, Boolean> {
        val left = meta.weeksLeft - 1
        return if (left <= 0) {
            Generator.generateMeta(meta.patchNumber + 1, rng) to true
        } else {
            meta.copy(weeksLeft = left) to false
        }
    }

    private fun maybeGenerateTransferOffer(state: GameState, rng: Random): GameState {
        if (state.incomingOffers.size >= 2) return state
        if (rng.nextDouble() > 0.45) return state
        val candidates = Discipline.entries.flatMap { d ->
            val team = state.teamFor(d)
            team.players.filter { p ->
                team.players.size > 5 && team.players.count { it.role == p.role } > 1
            }.map { it to d }
        }
        if (candidates.isEmpty()) return state
        val (player, d) = candidates.random(rng)
        if (state.incomingOffers.any { it.playerId == player.id }) return state
        val opponents = state.leagueFor(d).teams.filter { !it.isPlayer }
        if (opponents.isEmpty()) return state
        val club = opponents.random(rng)
        val amount = (player.marketValue() * rng.nextDouble(0.8, 1.35)).toInt()
        val offer = TransferOffer(
            id = "offer-${player.id}-${state.season}-${state.week}",
            playerId = player.id,
            playerNick = player.nick,
            clubName = club.name,
            amount = amount,
            weeksLeft = 2
        )
        return state.copy(incomingOffers = state.incomingOffers + offer).withNews(
            "${club.name} предлагает ${amount.moneyFull()} за ${player.nick}. Предложение действует 2 недели."
        )
    }

    private fun simulateAiRegular(league: League, state: GameState, rng: Random): League {
        val userOpponent = state.weekResults.filter { it.discipline == league.discipline }.map { it.opponentId }.toSet()
        val scheduled = league.schedule.getOrNull(state.week - 1)
        val excluded = userOpponent + listOfNotNull(scheduled)
        val pool = league.teams.filter { !it.isPlayer && it.id !in excluded }.shuffled(rng)
        var teams = league.teams.toList()
        pool.chunked(2).forEach { pair ->
            if (pair.size == 2) {
                val a = pair[0]
                val b = pair[1]
                val p = 1.0 / (1.0 + kotlin.math.exp(-(a.strength - b.strength) / 6.0))
                val aWins = rng.nextDouble() < p
                teams = teams.map { t ->
                    when (t.id) {
                        a.id -> t.copy(
                            wins = t.wins + if (aWins) 1 else 0,
                            losses = t.losses + if (aWins) 0 else 1,
                            mapDiff = t.mapDiff + if (aWins) 1 else -1
                        )
                        b.id -> t.copy(
                            wins = t.wins + if (aWins) 0 else 1,
                            losses = t.losses + if (aWins) 1 else 0,
                            mapDiff = t.mapDiff + if (aWins) -1 else 1
                        )
                        else -> t
                    }
                }
            }
        }
        return league.copy(teams = teams)
    }

    private fun createSemis(league: League): League {
        if (league.playoffs.isNotEmpty()) return league
        val top = league.sorted().take(4)
        if (top.size < 4) return league
        return league.copy(
            playoffs = listOf(
                PlayoffSeries(round = 1, teamA = top[0].id, teamB = top[3].id),
                PlayoffSeries(round = 1, teamA = top[1].id, teamB = top[2].id)
            )
        )
    }

    private fun processPlayoffs(league: League, week: Int, rng: Random): League {
        var updated = league
        if (week == 6) {
            updated = updated.copy(playoffs = updated.playoffs.map { s ->
                if (s.round == 1 && s.winner == null) simulateSeries(s, updated.teams, rng) else s
            })
            if (updated.playoffs.none { it.round == 2 }) {
                val semis = updated.playoffs.filter { it.round == 1 }
                if (semis.size == 2 && semis.all { it.winner != null }) {
                    updated = updated.copy(
                        playoffs = updated.playoffs + PlayoffSeries(
                            round = 2,
                            teamA = semis[0].winner!!,
                            teamB = semis[1].winner!!
                        )
                    )
                }
            }
        }
        if (week == 7) {
            updated = updated.copy(playoffs = updated.playoffs.map { s ->
                if (s.round == 2 && s.winner == null) simulateSeries(s, updated.teams, rng) else s
            })
            val final = updated.playoffs.lastOrNull { it.round == 2 }
            if (final?.winner != null) updated = updated.copy(champion = final.winner)
        }
        return updated
    }

    private fun processCupWeek(cup: Cup, week: Int, rng: Random): Cup {
        var updated = cup
        if (week >= 3) {
            updated = updated.copy(playoffs = updated.playoffs.map { s ->
                if (s.round == 1 && s.winner == null) simulateSeries(s, updated.teams, rng) else s
            })
            if (updated.playoffs.none { it.round == 2 }) {
                val semis = updated.playoffs.filter { it.round == 1 }
                if (semis.size == 2 && semis.all { it.winner != null }) {
                    updated = updated.copy(
                        playoffs = updated.playoffs + PlayoffSeries(
                            round = 2,
                            teamA = semis[0].winner!!,
                            teamB = semis[1].winner!!
                        )
                    )
                }
            }
        }
        if (week >= 5) {
            updated = updated.copy(playoffs = updated.playoffs.map { s ->
                if (s.round == 2 && s.winner == null) simulateSeries(s, updated.teams, rng) else s
            })
            val final = updated.playoffs.lastOrNull { it.round == 2 }
            if (final?.winner != null) updated = updated.copy(champion = final.winner)
        }
        return updated
    }

    private fun simulateSeries(series: PlayoffSeries, teams: List<LeagueTeam>, rng: Random): PlayoffSeries {
        val a = teams.firstOrNull { it.id == series.teamA }
        val b = teams.firstOrNull { it.id == series.teamB }
        val sa = a?.strength?.toDouble() ?: 60.0
        val sb = b?.strength?.toDouble() ?: 60.0
        val p = 1.0 / (1.0 + kotlin.math.exp(-(sa - sb) / 6.0))
        var wa = 0
        var wb = 0
        while (wa < 2 && wb < 2) {
            if (rng.nextDouble() < p) wa++ else wb++
        }
        return series.copy(
            winsA = wa,
            winsB = wb,
            winner = if (wa > wb) series.teamA else series.teamB
        )
    }

    // -----------------------------------------------------------------------
    // Season rollover
    // -----------------------------------------------------------------------

    private fun placementFor(league: League): Int {
        val final = league.playoffs.firstOrNull { it.round == 2 }
        if (final != null) {
            if (final.winner == "PLAYER") return 1
            if (final.teamA == "PLAYER" || final.teamB == "PLAYER") return 2
        }
        val semi = league.playoffs.firstOrNull {
            it.round == 1 && (it.teamA == "PLAYER" || it.teamB == "PLAYER")
        }
        if (semi != null) {
            val seed = league.sorted().take(4).indexOfFirst { it.isPlayer }
            return if (seed <= 1) 3 else 4
        }
        return league.sorted().indexOfFirst { it.isPlayer } + 1
    }

    private fun prizeFor(placement: Int, tier: Int): Int {
        val base = listOf(250_000, 150_000, 80_000, 60_000, 35_000, 20_000)
        val b = base.getOrElse(placement - 1) { 15_000 }
        return (b * (1.0 + tier * 1.2)).toInt()
    }

    private fun fanPrize(placement: Int, tier: Int): Int = when (placement) {
        1 -> 20_000 * (tier + 1)
        2 -> 10_000 * (tier + 1)
        3, 4 -> 5_000 * (tier + 1)
        else -> 1_000
    }

    private fun adjustTier(tier: Int, placement: Int): Int = when {
        placement <= 2 && tier < 3 -> tier + 1
        placement >= 5 && tier > 0 -> tier - 1
        else -> tier
    }

    private fun developTeam(team: Team, rng: Random): Pair<Team, List<String>> {
        val notes = mutableListOf<String>()
        val renewed = mutableListOf<String>()
        val players = team.players.map { p ->
            var np = p.copy(
                age = p.age + 1,
                form = 55,
                morale = 65,
                fatigue = 5,
                contractWeeks = p.contractWeeks - 7
            )
            if (np.contractWeeks <= 0) {
                np = np.copy(contractWeeks = 52, salary = (np.salary * 1.1).toInt())
                renewed += np.nick
            }
            if (np.age <= 23 && np.ovr() < np.potential) {
                repeat(2) {
                    val attr = listOf(Attribute.MECHANICS, Attribute.GAMESENSE, Attribute.TEAMWORK, Attribute.MENTALITY).random(rng)
                    np = np.withAttribute(attr, 1)
                }
                if (np.age <= 20) np = np.copy(potential = min(99, np.potential + 1))
            } else if (np.age >= 29) {
                repeat(2) {
                    val attr = listOf(Attribute.MECHANICS, Attribute.GAMESENSE, Attribute.TEAMWORK, Attribute.MENTALITY).random(rng)
                    np = np.withAttribute(attr, -1)
                }
            }
            if (np.trait == PlayerTrait.PRODIGY && np.age <= 22) {
                np = np.withAttribute(Attribute.MECHANICS, 1)
            }
            np
        }
        if (renewed.isNotEmpty()) notes += "Контракты продлены автоматически: ${renewed.joinToString()}."
        val chem = ((team.chemistry * 0.75) + 60 * 0.25).toInt()
        return team.copy(players = players, chemistry = chem, matchBonus = 0.0, tactic = Tactic.BALANCED) to notes
    }

    private fun endSeason(state: GameState, rng: Random, report: WeekReport): GameState {
        val season = state.season
        val csPlacement = placementFor(state.csLeague)
        val dotaPlacement = placementFor(state.dotaLeague)
        val csChampion = state.csLeague.champion?.let { state.csLeague.team(it)?.name } ?: "—"
        val dotaChampion = state.dotaLeague.champion?.let { state.dotaLeague.team(it)?.name } ?: "—"
        val csTier = state.csLeague.tier
        val dotaTier = state.dotaLeague.tier
        val promotedCs = csPlacement <= 2 && csTier < 3
        val relegatedCs = csPlacement >= 5 && csTier > 0
        val promotedDota = dotaPlacement <= 2 && dotaTier < 3
        val relegatedDota = dotaPlacement >= 5 && dotaTier > 0
        val prize = prizeFor(csPlacement, csTier) + prizeFor(dotaPlacement, dotaTier)
        val fanGain = fanPrize(csPlacement, csTier) + fanPrize(dotaPlacement, dotaTier)
        val leagueTrophies = mutableListOf<String>()
        if (state.csLeague.champion == "PLAYER") {
            leagueTrophies += "CS2 · ${tierLabel(csTier)} · Сезон $season"
        }
        if (state.dotaLeague.champion == "PLAYER") {
            leagueTrophies += "Dota 2 · ${tierLabel(dotaTier)} · Сезон $season"
        }
        val cupTrophies = mutableListOf<String>()
        if (state.csCup?.champion == "PLAYER") {
            cupTrophies += "Кубок лиги · CS2 · Сезон $season"
        }
        if (state.dotaCup?.champion == "PLAYER") {
            cupTrophies += "Кубок лиги · Dota 2 · Сезон $season"
        }
        val newTrophies = leagueTrophies + cupTrophies
        val notes = mutableListOf<String>()
        notes += "CS2: ${csPlacement} место. Чемпион: $csChampion."
        notes += "Dota 2: ${dotaPlacement} место. Чемпион: $dotaChampion."
        state.csCup?.champion?.let { champ ->
            notes += "Кубок CS2: ${state.csCup?.team(champ)?.name ?: "—"}."
        }
        state.dotaCup?.champion?.let { champ ->
            notes += "Кубок Dota 2: ${state.dotaCup?.team(champ)?.name ?: "—"}."
        }
        if (promotedCs) notes += "Состав CS2 выходит в ${tierLabel(csTier + 1)}!"
        if (promotedDota) notes += "Состав Dota 2 выходит в ${tierLabel(dotaTier + 1)}!"
        if (relegatedCs) notes += "CS2 понижается в ${tierLabel(csTier - 1)}."
        if (relegatedDota) notes += "Dota 2 понижается в ${tierLabel(dotaTier - 1)}."
        notes += "Призовые: ${prize.moneyFull()}, новые фанаты: +$fanGain."

        val (newCs, csNotes) = developTeam(state.csTeam, rng)
        val (newDota, dotaNotes) = developTeam(state.dotaTeam, rng)
        notes += csNotes
        notes += dotaNotes

        val newCsTier = adjustTier(csTier, csPlacement)
        val newDotaTier = adjustTier(dotaTier, dotaPlacement)
        val newCsLeague = Generator.generateLeague(Discipline.CS, newCsTier, state.org.name, state.difficulty, rng)
        val newDotaLeague = Generator.generateLeague(Discipline.DOTA, newDotaTier, state.org.name, state.difficulty, rng)
        val newCsCup = Generator.generateCup(Discipline.CS, newCsLeague, rng)
        val newDotaCup = Generator.generateCup(Discipline.DOTA, newDotaLeague, rng)
        val newTier = max(newCsTier, newDotaTier)
        val sponsor = state.sponsor
        val achievementSet = state.achievements.toMutableSet()
        if (newTrophies.isNotEmpty()) achievementSet += Achievement.FIRST_TROPHY
        if (cupTrophies.isNotEmpty()) achievementSet += Achievement.CUP_WINNER
        if (promotedCs || promotedDota) achievementSet += Achievement.PROMOTED
        val newSeason = season + 1
        var newState = state.copy(
            season = newSeason,
            week = 1,
            actionPoints = 4,
            money = state.money + prize,
            totalEarned = state.totalEarned + prize,
            org = state.org.copy(fans = state.org.fans + fanGain),
            csTeam = newCs,
            dotaTeam = newDota,
            csLeague = newCsLeague,
            dotaLeague = newDotaLeague,
            csCup = newCsCup,
            dotaCup = newDotaCup,
            meta = Generator.generateMeta(state.meta.patchNumber + 1, rng),
            incomingOffers = emptyList(),
            market = Generator.generateMarket(
                newTier,
                state.facilities.academy,
                rng,
                scoutRating = state.staffFor(StaffRole.SCOUT)?.rating ?: 50
            ),
            staffCandidates = Generator.generateStaffCandidates(newTier, rng),
            sponsor = sponsor,
            sponsorOffers = Generator.generateSponsorOffers(newTier, state.org.fans + fanGain, rng),
            pendingEvent = null,
            objectives = Generator.generateObjectives(newTier, rng),
            weekStats = WeekStats(),
            weekResults = emptyList(),
            playedThisWeek = emptyList(),
            playedCupMatches = emptyList(),
            lastReport = null,
            matchHistory = state.matchHistory,
            trophies = state.trophies + leagueTrophies,
            achievements = achievementSet.toList(),
            news = state.news,
            careerWins = state.careerWins,
            careerLosses = state.careerLosses,
            bestOvr = 0
        ).recalcBest()
        if (newState.money < 0) {
            newState = newState.copy(money = newState.money + 90_000)
            notes += "Инвестор покрыл долг клуба и выделил $90 000 на новый сезон."
        }
        val summary = SeasonSummary(
            season = season,
            csPlacement = csPlacement,
            dotaPlacement = dotaPlacement,
            csChampion = csChampion,
            dotaChampion = dotaChampion,
            prize = prize,
            fanGain = fanGain,
            promotedCs = promotedCs,
            relegatedCs = relegatedCs,
            promotedDota = promotedDota,
            relegatedDota = relegatedDota,
            trophiesWon = newTrophies,
            notes = notes
        )
        newState = newState.copy(
            seasonSummary = summary,
            pendingEvent = Generator.generateEvent(newState, rng),
            news = (newState.news + notes + "Сезон $newSeason начался. Новые цели, новые соперники.").takeLast(60)
        )
        return checkAchievements(newState)
    }

    // -----------------------------------------------------------------------
    // Achievements
    // -----------------------------------------------------------------------

    fun checkAchievements(state: GameState): GameState {
        val unlocked = state.achievements.toMutableSet()
        val newOnes = mutableListOf<Achievement>()
        fun add(a: Achievement) {
            if (unlocked.add(a)) newOnes += a
        }
        if (state.careerWins > 0) add(Achievement.FIRST_WIN)
        if (state.trophies.isNotEmpty()) add(Achievement.FIRST_TROPHY)
        if (state.csCup?.champion == "PLAYER" || state.dotaCup?.champion == "PLAYER") add(Achievement.CUP_WINNER)
        if (state.org.fans >= 50_000) add(Achievement.FANS_50K)
        if (state.org.fans >= 500_000) add(Achievement.FANS_500K)
        if (state.money >= 1_000_000) add(Achievement.RICH_1M)
        if ((state.csTeam.players + state.dotaTeam.players).any { it.ovr() >= 80 }) add(Achievement.OVR_80)
        val anyMax = listOf(
            state.facilities.training, state.facilities.analytics, state.facilities.psychology,
            state.facilities.bootcamp, state.facilities.merch, state.facilities.academy
        ).any { it >= 5 }
        if (anyMax) add(Achievement.MAX_FACILITY)
        if (state.season >= 3) add(Achievement.SEASON_3)
        if (state.weekResults.size >= 2 && state.weekResults.all { it.win }) add(Achievement.PERFECT_WEEK)
        if (newOnes.isEmpty()) return state
        return state.copy(
            achievements = unlocked.toList(),
            news = (state.news + newOnes.map { "Достижение: ${it.title} — ${it.description}" }).takeLast(60)
        )
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private fun trainingGain(p: Player, state: GameState, coach: Staff?, rng: Random): Int {
        if (p.ovr() >= p.potential) return 0
        var gain = 1 + rng.nextInt(0, 3)
        if (coach != null && coach.rating >= 68) gain += 1
        gain = (gain * (1.0 + (state.facilities.training - 1) * 0.08)).roundToInt()
        if (p.age <= 21) gain += 1
        if (p.trait == PlayerTrait.GRINDER) gain += 1
        return gain.coerceAtLeast(1)
    }

    private fun fatigueGain(state: GameState, base: Int, p: Player): Int {
        var v = base * (1.0 - (state.facilities.bootcamp - 1) * 0.06)
        if (p.trait == PlayerTrait.FRAGILE) v *= 1.35
        return max(1, v.roundToInt())
    }
}
