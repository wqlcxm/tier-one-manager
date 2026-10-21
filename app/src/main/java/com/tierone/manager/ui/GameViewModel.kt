package com.tierone.manager.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tierone.manager.data.*
import com.tierone.manager.game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

data class MatchDayState(
    val week: Int,
    val queue: List<Matchup>,
    val index: Int = 0
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SaveRepository(app)
    private val saveMutex = Mutex()

    var state by mutableStateOf<GameState?>(null)
        private set

    var matchday by mutableStateOf<MatchDayState?>(null)
        private set

    var live by mutableStateOf<LiveMatch?>(null)
        private set

    var resultApplied by mutableStateOf(false)
        private set

    init {
        state = repo.load()?.let { Engine.ensureSystems(it) }
    }

    private fun commit(newState: GameState) {
        state = newState
        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock { repo.save(newState) }
        }
    }

    fun newGame(name: String, region: String, difficulty: Int, logoSeed: Int) {
        val s = Engine.newGame(name.trim().ifBlank { "TIER ONE" }, region, logoSeed, difficulty)
        matchday = null
        live = null
        resultApplied = false
        commit(s)
    }

    fun resetCareer() {
        repo.clear()
        matchday = null
        live = null
        resultApplied = false
        state = null
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    fun trainTeam(d: Discipline, attr: Attribute) =
        state?.let { commit(Engine.trainTeam(it, d, attr)) }

    fun trainPlayer(playerId: String, attr: Attribute) =
        state?.let { commit(Engine.trainPlayer(it, playerId, attr)) }

    fun restTeam(d: Discipline) =
        state?.let { commit(Engine.restTeam(it, d)) }

    fun scrim(d: Discipline) =
        state?.let { commit(Engine.scrim(it, d)) }

    fun mediaDay() =
        state?.let { commit(Engine.mediaDay(it)) }

    fun scoutMarket() =
        state?.let { commit(Engine.scoutMarket(it)) }

    fun scoutCandidate(playerId: String) =
        state?.let { commit(Engine.scoutCandidate(it, playerId)) }

    fun signPlayer(playerId: String) =
        state?.let { commit(Engine.signPlayer(it, playerId)) }

    fun negotiatePlayer(playerId: String) =
        state?.let { commit(Engine.negotiateSign(it, playerId)) }

    fun acceptIncomingOffer(offerId: String) =
        state?.let { commit(Engine.acceptIncomingOffer(it, offerId)) }

    fun rejectIncomingOffer(offerId: String) =
        state?.let { commit(Engine.rejectIncomingOffer(it, offerId)) }

    fun sellPlayer(playerId: String) =
        state?.let { commit(Engine.sellPlayer(it, playerId)) }

    fun upgradeFacility(type: FacilityType) =
        state?.let { commit(Engine.upgradeFacility(it, type)) }

    fun hireStaff(staffId: String) =
        state?.let { commit(Engine.hireStaff(it, staffId)) }

    fun signSponsor(index: Int) =
        state?.let { commit(Engine.signSponsor(it, index)) }

    fun chooseEvent(index: Int) =
        state?.let { commit(Engine.chooseEvent(it, index)) }

    fun setTactic(d: Discipline, tactic: Tactic) =
        state?.let { commit(Engine.setTactic(it, d, tactic)) }

    fun setSettings(sound: Boolean, vibration: Boolean) =
        state?.let { commit(Engine.setSettings(it, sound, vibration)) }

    fun dismissReport() =
        state?.let { commit(it.copy(lastReport = null)) }

    fun dismissSeasonSummary() =
        state?.let { commit(it.copy(seasonSummary = null)) }

    fun completeTutorial() =
        state?.let { commit(it.copy(tutorialDone = true)) }

    // -----------------------------------------------------------------------
    // Match week
    // -----------------------------------------------------------------------

    fun beginWeek() {
        val s = state ?: return
        if (s.pendingEvent != null || s.lastReport != null || s.seasonSummary != null) return
        if (matchday != null) return
        val queue = Engine.upcomingMatchups(s)
        if (queue.isEmpty()) {
            commit(Engine.finishWeek(s))
            return
        }
        matchday = MatchDayState(s.week, queue, 0)
        prepareLive()
    }

    private fun prepareLive() {
        val s = state ?: return
        val md = matchday ?: return
        val matchup = md.queue.getOrNull(md.index) ?: return
        live = MatchEngine.startLive(s, matchup, s.teamFor(matchup.discipline).tactic)
        resultApplied = false
    }

    fun startMatch(tactic: Tactic, mapIndex: Int, plan: DraftPlan) {
        val s = state ?: return
        val current = live ?: return
        val prepared = MatchEngine.applyPreparation(current, tactic, mapIndex, plan, s)
        live = MatchEngine.playFirstHalf(prepared)
    }

    fun applyDecision(decision: MatchDecision) {
        val s = state ?: return
        val current = live ?: return
        if (!current.decisionPending) return
        live = MatchEngine.applyDecision(current, decision, s)
    }

    fun onMatchAnimationDone() {
        val s = state ?: return
        val current = live ?: return
        if (!current.finished || resultApplied) return
        if (current.result == null) return
        val next = Engine.finishMatch(s, current)
        resultApplied = true
        commit(next)
        live = current.copy(phase = LivePhase.RESULT)
    }

    fun nextAfterResult() {
        val md = matchday ?: return
        val s = state ?: return
        if (md.index + 1 < md.queue.size) {
            matchday = md.copy(index = md.index + 1)
            prepareLive()
        } else {
            val finished = Engine.finishWeek(s)
            matchday = null
            live = null
            resultApplied = false
            commit(finished)
        }
    }

    fun exitMatchday() {
        val current = live
        if (current != null && current.phase == LivePhase.LIVE) return
        if (current != null && current.phase == LivePhase.RESULT && current.finished) {
            matchday = null
            live = null
            resultApplied = false
        } else {
            matchday = null
            live = null
            resultApplied = false
        }
    }
}
