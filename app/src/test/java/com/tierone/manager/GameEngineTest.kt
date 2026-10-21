package com.tierone.manager

import com.tierone.manager.data.Achievement
import com.tierone.manager.data.Attribute
import com.tierone.manager.data.Discipline
import com.tierone.manager.data.DraftPlan
import com.tierone.manager.data.GameState
import com.tierone.manager.data.Generator
import com.tierone.manager.data.MatchResult
import com.tierone.manager.data.MetaState
import com.tierone.manager.data.PlayoffSeries
import com.tierone.manager.data.Staff
import com.tierone.manager.data.StaffRole
import com.tierone.manager.data.Tactic
import com.tierone.manager.data.TransferOffer
import com.tierone.manager.data.tierLabel
import com.tierone.manager.game.Engine
import com.tierone.manager.game.LiveMatch
import com.tierone.manager.game.MapChoice
import com.tierone.manager.game.MatchDecision
import com.tierone.manager.game.MatchEngine
import com.tierone.manager.game.Matchup
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }

    private fun newGame(seed: Int = 7): GameState =
        Engine.newGame("Test Org", "RU", 42, 1, Random(seed))

    @Test
    fun newGameHasTwoPlayableRostersAndLeagues() {
        val state = newGame()

        assertEquals(1, state.season)
        assertEquals(1, state.week)
        assertEquals(4, state.actionPoints)
        assertTrue(state.money > 0)

        assertEquals(5, state.csTeam.players.size)
        assertEquals(5, state.dotaTeam.players.size)
        assertEquals(5, state.csTeam.lineup().size)
        assertEquals(5, state.dotaTeam.lineup().size)
        assertEquals(setOf(Discipline.CS), state.csTeam.players.map { it.discipline }.toSet())
        assertEquals(setOf(Discipline.DOTA), state.dotaTeam.players.map { it.discipline }.toSet())

        assertEquals(6, state.csLeague.teams.size)
        assertEquals(6, state.dotaLeague.teams.size)
        assertEquals(5, state.csLeague.schedule.size)
        assertEquals(5, state.dotaLeague.schedule.size)
        assertTrue(state.csLeague.teams.any { it.isPlayer })
        assertTrue(state.dotaLeague.teams.any { it.isPlayer })
        assertEquals(0, state.csLeague.tier)
        assertEquals(0, state.dotaLeague.tier)

        assertNotNull(state.csCup)
        assertNotNull(state.dotaCup)
        assertEquals(4, state.csCup!!.teams.size)
        assertEquals(2, state.csCup!!.playoffs.size)
        assertTrue(state.meta.weeksLeft > 0)
    }

    @Test
    fun saveRoundTripPreservesEntireState() {
        var state = newGame(seed = 11)

        state = Engine.trainTeam(state, Discipline.CS, com.tierone.manager.data.Attribute.GAMESENSE, Random(12))
        state = Engine.restTeam(state, Discipline.DOTA, Random(13))
        state = Engine.mediaDay(state, Random(14))

        val encoded = json.encodeToString(GameState.serializer(), state)
        val decoded = json.decodeFromString(GameState.serializer(), encoded)

        assertEquals(state, decoded)
        assertEquals(state.csTeam.players, decoded.csTeam.players)
        assertEquals(state.csLeague, decoded.csLeague)
        assertEquals(state.dotaLeague, decoded.dotaLeague)
        assertTrue(encoded.contains("\"season\":1"))
    }

    @Test
    fun trainingSpendsActionPointAndImprovesAttribute() {
        val state = newGame(seed = 3)
        val player = state.csTeam.players.first()
        val before = player.attr(com.tierone.manager.data.Attribute.GAMESENSE)

        val updated = Engine.trainPlayer(state, player.id, com.tierone.manager.data.Attribute.GAMESENSE, Random(4))
        val after = updated.playerById(player.id)!!.attr(com.tierone.manager.data.Attribute.GAMESENSE)

        assertEquals(state.actionPoints - 1, updated.actionPoints)
        assertTrue(after >= before)
    }

    @Test
    fun analystAndScoutStavesHaveRealEffects() {
        val state = newGame(seed = 17)
        val strengthWithout = state.teamStrength(Discipline.CS)
        val withAnalyst = state.copy(
            staff = listOf(
                Staff(id = "analyst-1", role = StaffRole.ANALYST, name = "Test Analyst", rating = 85, salary = 2_000)
            )
        )

        assertTrue(withAnalyst.teamStrength(Discipline.CS) > strengthWithout)

        val regularMarket = Generator.generateMarket(0, 1, Random(18))
        val scoutedMarket = Generator.generateMarket(0, 1, Random(18), scoutRating = 85)
        assertEquals(12, regularMarket.size)
        assertEquals(14, scoutedMarket.size)
    }

    @Test
    fun finishWeekDoesNotAdvanceBeforeScheduledMatchesArePlayed() {
        val state = newGame(seed = 5)
        assertTrue(Engine.upcomingMatchups(state).isNotEmpty())

        val unchanged = Engine.finishWeek(state, Random(6))

        assertEquals(state.week, unchanged.week)
        assertEquals(state.weekResults, unchanged.weekResults)
    }

    @Test
    fun liveMatchProducesValidResultAndStats() {
        val state = newGame(seed = 9)
        val rng = Random(10)
        val matchup = Engine.upcomingMatchups(state).first()

        var live = MatchEngine.startLive(state, matchup, com.tierone.manager.data.Tactic.BALANCED, rng)
        assertEquals(5, live.userNicks.size)
        assertEquals(5, live.opponentNicks.size)

        live = MatchEngine.playFirstHalf(live, rng)
        assertTrue(live.decisionPending)
        assertTrue(live.events.isNotEmpty())

        live = MatchEngine.applyDecision(live, MatchDecision.CALM, state, rng)
        assertTrue(live.finished)
        assertNotNull(live.result)

        val result = live.result!!
        assertEquals(10, result.stats.size)
        assertTrue(result.maps.isNotEmpty())
        if (matchup.isPlayoff) {
            assertTrue(result.seriesUs == 2 || result.seriesThem == 2)
        } else {
            assertEquals(1, result.seriesUs + result.seriesThem)
        }
        assertTrue(result.mvp.isNotBlank())
    }

    @Test
    fun cupSemifinalIsScheduledAndDoesNotChangeLeagueTable() {
        val state = newGame(seed = 51).copy(week = 3)
        val cup = state.csCup!!
        val series = cup.playoffs.first {
            it.round == 1 && (it.teamA == "PLAYER" || it.teamB == "PLAYER")
        }
        val opponentId = if (series.teamA == "PLAYER") series.teamB else series.teamA
        val opponent = cup.team(opponentId)!!
        val beforeLeague = state.csLeague

        val after = Engine.finishMatch(
            state,
            cupLive(state, Discipline.CS, opponentId, opponent.name, 1, true),
            Random(52)
        )

        assertEquals(beforeLeague, after.csLeague)
        assertTrue(after.playedThisWeek.isEmpty())
        assertTrue(after.playedCupMatches.contains("CS:1"))
        val updatedSeries = after.csCup!!.playoffs.first {
            it.round == 1 && (it.teamA == "PLAYER" || it.teamB == "PLAYER")
        }
        assertEquals("PLAYER", updatedSeries.winner)
    }

    @Test
    fun cupBracketUsesWeekThreeAndWeekFive() {
        val week3 = newGame(seed = 53).copy(week = 3)
        val cupMatches = Engine.upcomingMatchups(week3).filter { it.isCup }
        assertEquals(2, cupMatches.size)
        assertTrue(cupMatches.all { it.cupRound == 1 })
        assertTrue(cupMatches.all { it.isPlayoff })

        val state = newGame(seed = 54).copy(week = 5)
        val cup = state.csCup!!
        val player = cup.playerTeam()!!
        val opponent = cup.teams.first { !it.isPlayer }
        val final = PlayoffSeries(round = 2, teamA = player.id, teamB = opponent.id)
        val withFinal = state.copy(csCup = cup.copy(playoffs = cup.playoffs + final))
        val finalMatch = Engine.upcomingMatchups(withFinal).first {
            it.isCup && it.discipline == Discipline.CS
        }
        assertEquals(2, finalMatch.cupRound)
        assertEquals(opponent.id, finalMatch.opponent.id)
    }

    @Test
    fun cupFinalWinAddsTrophyAndAchievement() {
        val state = newGame(seed = 55).copy(week = 5)
        val cup = state.csCup!!
        val player = cup.playerTeam()!!
        val opponent = cup.teams.first { !it.isPlayer }
        val final = PlayoffSeries(round = 2, teamA = player.id, teamB = opponent.id)
        val prepared = state.copy(csCup = cup.copy(playoffs = cup.playoffs + final))

        val after = Engine.finishMatch(
            prepared,
            cupLive(prepared, Discipline.CS, opponent.id, opponent.name, 2, true),
            Random(56)
        )

        assertEquals("PLAYER", after.csCup!!.champion)
        assertTrue(after.trophies.any { it.contains("Кубок лиги") })
        assertTrue(after.achievements.contains(Achievement.CUP_WINNER))
        assertTrue(after.money > prepared.money)
        assertTrue(after.playedCupMatches.contains("CS:2"))
        assertEquals(prepared.csLeague, after.csLeague)
    }

    @Test
    fun mapAndDraftPreparationChangePreviewStrength() {
        val state = newGame(seed = 57)
        val matchup = Engine.upcomingMatchups(state).first { it.discipline == Discipline.CS }
        val live = MatchEngine.startLive(state, matchup, Tactic.AGGRESSIVE, Random(58)).copy(
            opponentTactic = Tactic.BALANCED,
            opponentPlan = DraftPlan.LATE,
            mapChoices = listOf(
                MapChoice("Карта A", Tactic.AGGRESSIVE),
                MapChoice("Карта B", Tactic.STRATEGIC)
            )
        )

        val synergy = MatchEngine.previewStrength(state, live, Tactic.AGGRESSIVE, 0, DraftPlan.EARLY)
        val offMap = MatchEngine.previewStrength(state, live, Tactic.AGGRESSIVE, 1, DraftPlan.EARLY)
        assertTrue(synergy > offMap)

        val dotaLive = live.copy(discipline = Discipline.DOTA, mapChoices = emptyList())
        val earlyDraft = MatchEngine.previewStrength(state, dotaLive, Tactic.AGGRESSIVE, 0, DraftPlan.EARLY)
        val lateDraft = MatchEngine.previewStrength(state, dotaLive, Tactic.AGGRESSIVE, 0, DraftPlan.LATE)
        assertTrue(earlyDraft > lateDraft)
    }

    @Test
    fun metaAndCoachSpecialtyBoostMatchPreparation() {
        val state = newGame(seed = 59)
        val matchup = Engine.upcomingMatchups(state).first { it.discipline == Discipline.CS }
        val baseLive = MatchEngine.startLive(state, matchup, Tactic.AGGRESSIVE, Random(60)).copy(
            discipline = Discipline.DOTA,
            opponentTactic = Tactic.BALANCED,
            opponentPlan = DraftPlan.LATE,
            mapChoices = emptyList()
        )
        val neutralCoach = Staff(
            id = "coach-neutral",
            role = StaffRole.COACH,
            name = "Neutral Coach",
            rating = 55,
            salary = 1_000,
            specialty = null
        )
        val specialist = neutralCoach.copy(id = "coach-specialist", specialty = Tactic.AGGRESSIVE)
        val neutralState = state.copy(staff = listOf(neutralCoach), meta = MetaState(favoredTactic = Tactic.STRATEGIC))
        val specialistState = state.copy(staff = listOf(specialist), meta = MetaState(favoredTactic = Tactic.STRATEGIC))

        val neutral = MatchEngine.previewStrength(neutralState, baseLive, Tactic.AGGRESSIVE, 0, DraftPlan.EARLY)
        val boosted = MatchEngine.previewStrength(specialistState, baseLive, Tactic.AGGRESSIVE, 0, DraftPlan.EARLY)
        assertTrue(boosted > neutral)

        val metaOff = state.copy(staff = emptyList(), meta = MetaState(favoredTactic = Tactic.STRATEGIC))
        val metaOn = state.copy(staff = emptyList(), meta = MetaState(favoredTactic = Tactic.AGGRESSIVE))
        val withoutMeta = MatchEngine.previewStrength(metaOff, baseLive, Tactic.AGGRESSIVE, 0, DraftPlan.EARLY)
        val withMeta = MatchEngine.previewStrength(metaOn, baseLive, Tactic.AGGRESSIVE, 0, DraftPlan.EARLY)
        assertTrue(withMeta > withoutMeta)
    }

    @Test
    fun incomingOffersCanBeAcceptedOrRejected() {
        val state = newGame(seed = 61)
        val extra = state.csTeam.players.first().copy(id = "offer-bench", nick = "OfferBench")
        val withExtra = state.copy(csTeam = state.csTeam.copy(players = state.csTeam.players + extra))
        val offer = TransferOffer(
            id = "offer-test",
            playerId = extra.id,
            playerNick = extra.nick,
            clubName = "Rival Club",
            amount = 123_456,
            weeksLeft = 2
        )
        val withOffer = withExtra.copy(incomingOffers = listOf(offer))

        val accepted = Engine.acceptIncomingOffer(withOffer, offer.id)
        assertEquals(withOffer.money + offer.amount, accepted.money)
        assertTrue(accepted.csTeam.players.none { it.id == extra.id })
        assertTrue(accepted.incomingOffers.isEmpty())

        val rejected = Engine.rejectIncomingOffer(withOffer, offer.id)
        assertEquals(withOffer.money, rejected.money)
        assertTrue(rejected.csTeam.players.any { it.id == extra.id })
        assertTrue(rejected.incomingOffers.isEmpty())
    }

    @Test
    fun negotiationCanSignAtADiscount() {
        val state = newGame(seed = 63)
        val candidate = state.market.first()
        var signed: GameState? = null
        for (seed in 0..300) {
            val tried = Engine.negotiateSign(state, candidate.player.id, Random(seed))
            if (tried.market.none { it.player.id == candidate.player.id }) {
                signed = tried
                break
            }
        }
        assertNotNull(signed)
        val result = signed!!
        val paid = state.money - result.money
        assertTrue(paid in 1 until candidate.price)
        assertEquals(state.actionPoints - 1, result.actionPoints)
        assertTrue(result.teamFor(candidate.player.discipline).players.any {
            it.id == candidate.player.id
        })
    }

    @Test
    fun ensureSystemsBackfillsCupsAndMetaForOldSave() {
        val old = newGame(seed = 65).copy(
            csCup = null,
            dotaCup = null,
            meta = MetaState(weeksLeft = 0)
        )
        val migrated = Engine.ensureSystems(old, Random(66))

        assertNotNull(migrated.csCup)
        assertNotNull(migrated.dotaCup)
        assertEquals(2, migrated.csCup!!.playoffs.size)
        assertTrue(migrated.meta.weeksLeft > 0)
    }

    @Test
    fun favoredMetaAttributeGivesExtraTrainingGain() {
        val state = newGame(seed = 67)
        val player = state.csTeam.players.minByOrNull { it.ovr() }!!
        assertTrue(player.ovr() < player.potential)
        val before = player.attr(Attribute.GAMESENSE)
        val offMeta = state.copy(meta = MetaState(favoredAttribute = Attribute.MENTALITY))
        val onMeta = state.copy(meta = MetaState(favoredAttribute = Attribute.GAMESENSE))

        val off = Engine.trainPlayer(offMeta, player.id, Attribute.GAMESENSE, Random(68))
        val on = Engine.trainPlayer(onMeta, player.id, Attribute.GAMESENSE, Random(68))
        val offGain = off.playerById(player.id)!!.attr(Attribute.GAMESENSE) - before
        val onGain = on.playerById(player.id)!!.attr(Attribute.GAMESENSE) - before

        assertEquals(offGain + 1, onGain)
    }

    @Test
    fun fullSeasonCompletesAndRollsOver() {
        var state = newGame(seed = 2026)
        val rng = Random(2027)
        val sponsorWeeksAtStart = state.sponsor?.weeks ?: 0
        var weeksPlayed = 0

        while (state.season == 1) {
            weeksPlayed++
            assertTrue("Season did not finish in time", weeksPlayed <= 10)

            var guard = 0
            while (Engine.upcomingMatchups(state).isNotEmpty()) {
                guard++
                assertTrue("Too many matchdays", guard <= 4)
                val beforeCount = Engine.upcomingMatchups(state).size
                val matchup: Matchup = Engine.upcomingMatchups(state).first()
                var live = MatchEngine.startLive(state, matchup, state.teamFor(matchup.discipline).tactic, rng)
                live = MatchEngine.playFirstHalf(live, rng)
                live = MatchEngine.applyDecision(live, MatchDecision.CALM, state, rng)
                state = Engine.finishMatch(state, live, rng)
                assertTrue(Engine.upcomingMatchups(state).size < beforeCount)
            }

            state = Engine.finishWeek(state, rng)
        }

        assertEquals(2, state.season)
        assertEquals(1, state.week)
        assertEquals(4, state.actionPoints)
        assertNotNull(state.seasonSummary)
        val summary = state.seasonSummary!!
        assertTrue(summary.notes.any { it.contains("Кубок CS2") })
        assertTrue(summary.notes.any { it.contains("Кубок Dota 2") })
        assertNotNull(state.csCup)
        assertNotNull(state.dotaCup)
        assertNull(state.csCup!!.champion)
        assertNull(state.dotaCup!!.champion)
        assertNull(state.lastReport)
        assertTrue(state.matchHistory.isNotEmpty())
        assertTrue(state.careerWins + state.careerLosses > 0)
        assertTrue(state.csLeague.tier in 0..3)
        assertTrue(state.dotaLeague.tier in 0..3)
        assertEquals(5, state.csLeague.schedule.size)
        assertEquals(5, state.dotaLeague.schedule.size)
        assertEquals(state.csLeague.tier, state.csLeague.tier)
        assertEquals(tierLabel(state.csLeague.tier), state.csLeague.tierName())

        assertTrue(sponsorWeeksAtStart > 0)
        assertEquals(sponsorWeeksAtStart - 7, state.sponsor?.weeks)

        val encoded = json.encodeToString(GameState.serializer(), state)
        assertEquals(state, json.decodeFromString(GameState.serializer(), encoded))
    }

    private fun cupLive(
        state: GameState,
        discipline: Discipline,
        opponentId: String,
        opponentName: String,
        round: Int,
        win: Boolean
    ): LiveMatch {
        val result = MatchResult(
            id = "cup-${discipline.name}-$round-${state.week}-$opponentId",
            discipline = discipline,
            season = state.season,
            week = state.week,
            opponentId = opponentId,
            opponentName = opponentName,
            opponentStrength = 60,
            isPlayoff = true,
            win = win,
            seriesUs = if (win) 2 else 1,
            seriesThem = if (win) 1 else 2,
            maps = emptyList(),
            mvp = "—",
            mvpIsUser = false,
            stats = emptyList(),
            money = 10_000,
            fans = 100,
            dateLabel = "S${state.season} · W${state.week}",
            isCup = true,
            cupRound = round
        )
        return LiveMatch(
            discipline = discipline,
            opponentId = opponentId,
            opponentName = opponentName,
            opponentStrength = 60,
            isPlayoff = true,
            tactic = Tactic.BALANCED,
            opponentTactic = Tactic.BALANCED,
            userStrength = 60.0,
            userNicks = state.teamFor(discipline).lineup().map { it.nick },
            opponentNicks = emptyList(),
            mapName = "Mirage",
            isCup = true,
            cupRound = round,
            result = result
        )
    }
}
