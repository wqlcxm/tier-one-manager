package com.tierone.manager.data

import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.pow

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

enum class Discipline(val label: String, val short: String) {
    CS("CS2", "CS"),
    DOTA("Dota 2", "DOTA")
}

enum class Role(val discipline: Discipline, val label: String, val short: String) {
    IGL(Discipline.CS, "Капитан / IGL", "IGL"),
    AWPER(Discipline.CS, "AWPer", "AWP"),
    RIFLER(Discipline.CS, "Рифлер", "RIF"),
    ENTRY(Discipline.CS, "Энтри-фраггер", "ENT"),
    SUPPORT(Discipline.CS, "Саппорт", "SUP"),
    CARRY(Discipline.DOTA, "Керри", "CARRY"),
    MID(Discipline.DOTA, "Мидер", "MID"),
    OFFLANE(Discipline.DOTA, "Оффлейнер", "OFF"),
    SOFT_SUPPORT(Discipline.DOTA, "4-я позиция", "POS4"),
    HARD_SUPPORT(Discipline.DOTA, "5-я позиция", "POS5");

    companion object {
        fun forDiscipline(d: Discipline): List<Role> = entries.filter { it.discipline == d }
    }
}

enum class PlayerTrait(val label: String, val hint: String) {
    NONE("Стабильный", "Без особых эффектов"),
    LEADER("Лидер", "Держит команду вместе: +к химии"),
    CLUTCH("Клатчер", "Прибавляет в плей-офф"),
    TOXIC("Токсик", "Сильный игрок, но портит атмосферу"),
    GRINDER("Трудяга", "Быстрее растёт на тренировках"),
    PRODIGY("Вундеркинд", "Высокий потенциал развития"),
    VETERAN("Ветеран", "Опыт помогает в важных матчах"),
    FRAGILE("Хрупкий", "Быстрее устаёт")
}

enum class Tactic { AGGRESSIVE, BALANCED, STRATEGIC }

fun Tactic.label(d: Discipline): String = when (this) {
    Tactic.AGGRESSIVE -> if (d == Discipline.CS) "Агрессия" else "Ранний пуш"
    Tactic.BALANCED -> "Баланс"
    Tactic.STRATEGIC -> if (d == Discipline.CS) "Стратегия" else "Лейт-гейм"
}

fun Tactic.hint(d: Discipline): String = when (this) {
    Tactic.AGGRESSIVE -> if (d == Discipline.CS) "Давим темпом и форсим дуэли" else "Забираем ранние объективы"
    Tactic.BALANCED -> "Гибкий план под соперника"
    Tactic.STRATEGIC -> if (d == Discipline.CS) "Контроль карты и поздние раунды" else "Играем на позднюю стадию"
}

fun Tactic.styleLabel(): String = when (this) {
    Tactic.AGGRESSIVE -> "Агрессия"
    Tactic.BALANCED -> "Баланс"
    Tactic.STRATEGIC -> "Стратегия"
}

/** План драфта для Dota 2. Работает как отдельная мини-игра «стиль против стиля». */
enum class DraftPlan(val label: String, val hint: String) {
    EARLY("РАННИЙ ТЕМП", "Забираем пространство до 20-й минуты"),
    BALANCED("БАЛАНС", "Гибкий драфт под оба тайминга"),
    LATE("ПОЗДНЯЯ ИГРА", "Тянем до предметов и добиваем на хайграунде")
}

/** Ранний темп бьёт лейт, лейт — баланс, баланс — ранний темп. */
fun draftEdge(mine: DraftPlan, theirs: DraftPlan): Int = when {
    mine == DraftPlan.EARLY && theirs == DraftPlan.LATE -> 1
    mine == DraftPlan.LATE && theirs == DraftPlan.BALANCED -> 1
    mine == DraftPlan.BALANCED && theirs == DraftPlan.EARLY -> 1
    mine == theirs -> 0
    else -> -1
}

/** Камень-ножницы-бумага: агрессия бьёт стратегию, стратегия — баланс, баланс — агрессию. */
fun tacticEdge(mine: Tactic, theirs: Tactic): Int = when {
    mine == Tactic.AGGRESSIVE && theirs == Tactic.STRATEGIC -> 1
    mine == Tactic.STRATEGIC && theirs == Tactic.BALANCED -> 1
    mine == Tactic.BALANCED && theirs == Tactic.AGGRESSIVE -> 1
    mine == theirs -> 0
    else -> -1
}

enum class Attribute(val label: String) {
    MECHANICS("Механика"),
    GAMESENSE("Геймсенс"),
    TEAMWORK("Командность"),
    MENTALITY("Ментальность"),
    STAMINA("Выносливость")
}

enum class StaffRole(val label: String) {
    COACH("Тренер"),
    ANALYST("Аналитик"),
    PSYCHOLOGIST("Психолог"),
    SCOUT("Скаут")
}

enum class FacilityType(val label: String, val description: String, val baseCost: Int) {
    TRAINING("Тренировочная база", "+8% к приросту на тренировках за уровень", 55_000),
    ANALYTICS("Аналитический отдел", "+0.35 к силе состава в матчах за уровень", 60_000),
    PSYCHOLOGY("Кабинет психолога", "+10% к восстановлению морали за уровень", 45_000),
    BOOTCAMP("Буткемп", "−6% накопления усталости за уровень", 50_000),
    MERCH("Мерч-студия", "+15% к доходу от фанатов за уровень", 45_000),
    ACADEMY("Академия", "Лучше скаутинг и сильнее новички за уровень", 65_000)
}

enum class ObjectiveType { PLAY_MATCHES, WIN_MATCHES, TRAIN, REST, SIGN_PLAYER, UPGRADE, MEDIA }

enum class Achievement(val title: String, val description: String) {
    FIRST_WIN("Первая кровь", "Выиграйте свой первый матч"),
    FIRST_TROPHY("Есть контакт с металлом", "Выиграйте первый титул"),
    PROMOTED("На повышение", "Выйдите в следующий дивизион"),
    FANS_50K("Своя аудитория", "Наберите 50 000 фанатов"),
    FANS_500K("Медийная империя", "Наберите 500 000 фанатов"),
    RICH_1M("Первый миллион", "Накопите $1 000 000"),
    OVR_80("Звезда", "Доведите игрока до OVR 80"),
    MAX_FACILITY("База мечты", "Прокачайте любой объект до 5 уровня"),
    SEASON_3("Долгая история", "Доиграйте до третьего сезона"),
    PERFECT_WEEK("Идеальная неделя", "Выиграйте оба матча за неделю"),
    CUP_WINNER("Кубок наш", "Выиграйте Кубок лиги")
}

// ---------------------------------------------------------------------------
// Core data
// ---------------------------------------------------------------------------

@Serializable
data class Player(
    val id: String,
    val nick: String,
    val name: String,
    val country: String,
    val age: Int,
    val role: Role,
    val mechanics: Int,
    val gamesense: Int,
    val teamwork: Int,
    val mentality: Int,
    val stamina: Int,
    val potential: Int,
    val form: Int = 55,
    val morale: Int = 70,
    val fatigue: Int = 8,
    val salary: Int = 1_000,
    val contractWeeks: Int = 52,
    val trait: PlayerTrait = PlayerTrait.NONE,
    val scouted: Boolean = true
) {
    val discipline: Discipline get() = role.discipline

    fun ovr(): Int {
        val (wm, wg, wt, wmnt) = role.weights()
        return (mechanics * wm + gamesense * wg + teamwork * wt + mentality * wmnt).toInt()
    }

    fun effective(playoff: Boolean = false): Double {
        var v = ovr().toDouble()
        v += (form - 50) * 0.12
        v += (morale - 50) * 0.08
        val resist = 1.0 - stamina / 260.0
        v -= fatigue * 0.13 * resist
        v += when (trait) {
            PlayerTrait.LEADER -> 0.8
            PlayerTrait.CLUTCH -> if (playoff) 1.8 else 0.2
            PlayerTrait.TOXIC -> -0.6
            PlayerTrait.GRINDER -> 0.3
            PlayerTrait.PRODIGY -> 0.6
            PlayerTrait.VETERAN -> if (playoff) 1.0 else 0.4
            PlayerTrait.FRAGILE -> -0.4
            PlayerTrait.NONE -> 0.0
        }
        if (age <= 19) v -= 1.0
        if (age >= 28) v -= (age - 27) * 0.4
        return v.coerceIn(25.0, 99.0)
    }

    fun marketValue(): Int {
        val base = ((ovr() - 40).coerceAtLeast(0).toDouble().pow(2.1) * 120.0)
        val pot = potential * 260.0
        val youth = (29 - age).coerceAtLeast(0) * 2_500.0
        return (base + pot + youth).toInt()
    }

    fun withAttribute(a: Attribute, delta: Int): Player = when (a) {
        Attribute.MECHANICS -> copy(mechanics = (mechanics + delta).coerceIn(1, 99))
        Attribute.GAMESENSE -> copy(gamesense = (gamesense + delta).coerceIn(1, 99))
        Attribute.TEAMWORK -> copy(teamwork = (teamwork + delta).coerceIn(1, 99))
        Attribute.MENTALITY -> copy(mentality = (mentality + delta).coerceIn(1, 99))
        Attribute.STAMINA -> copy(stamina = (stamina + delta).coerceIn(1, 99))
    }

    fun attr(a: Attribute): Int = when (a) {
        Attribute.MECHANICS -> mechanics
        Attribute.GAMESENSE -> gamesense
        Attribute.TEAMWORK -> teamwork
        Attribute.MENTALITY -> mentality
        Attribute.STAMINA -> stamina
    }
}

fun Role.weights(): List<Double> = when (this) {
    Role.IGL -> listOf(0.15, 0.40, 0.35, 0.10)
    Role.AWPER -> listOf(0.50, 0.30, 0.10, 0.10)
    Role.RIFLER -> listOf(0.55, 0.25, 0.10, 0.10)
    Role.ENTRY -> listOf(0.50, 0.15, 0.15, 0.20)
    Role.SUPPORT -> listOf(0.20, 0.35, 0.35, 0.10)
    Role.CARRY -> listOf(0.55, 0.30, 0.05, 0.10)
    Role.MID -> listOf(0.55, 0.30, 0.10, 0.05)
    Role.OFFLANE -> listOf(0.30, 0.25, 0.25, 0.20)
    Role.SOFT_SUPPORT -> listOf(0.25, 0.30, 0.35, 0.10)
    Role.HARD_SUPPORT -> listOf(0.15, 0.40, 0.35, 0.10)
}

@Serializable
data class Team(
    val discipline: Discipline,
    val name: String,
    val players: List<Player> = emptyList(),
    val chemistry: Int = 62,
    val tactic: Tactic = Tactic.BALANCED,
    val matchBonus: Double = 0.0
) {
    fun lineup(): List<Player> {
        val byRole = players.groupBy { it.role }
        val result = Role.forDiscipline(discipline).mapNotNull { role ->
            byRole[role]?.maxByOrNull { it.effective() }
        }.toMutableList()
        if (result.size < 5) {
            players.sortedByDescending { it.effective() }.forEach { p ->
                if (result.none { it.id == p.id } && result.size < 5) result += p
            }
        }
        return result
    }

    fun averageOvr(): Int = if (players.isEmpty()) 0 else players.map { it.ovr() }.average().toInt()

    fun lineupOvr(): Int = if (lineup().isEmpty()) 0 else lineup().map { it.ovr() }.average().toInt()
}

@Serializable
data class Org(
    val name: String,
    val region: String,
    val logoSeed: Int,
    val fans: Int = 5_000
)

@Serializable
data class Staff(
    val id: String,
    val role: StaffRole,
    val name: String,
    val rating: Int,
    val salary: Int,
    val specialty: Tactic? = null
)

@Serializable
data class Facilities(
    val training: Int = 1,
    val analytics: Int = 1,
    val psychology: Int = 1,
    val bootcamp: Int = 1,
    val merch: Int = 1,
    val academy: Int = 1
) {
    fun level(type: FacilityType): Int = when (type) {
        FacilityType.TRAINING -> training
        FacilityType.ANALYTICS -> analytics
        FacilityType.PSYCHOLOGY -> psychology
        FacilityType.BOOTCAMP -> bootcamp
        FacilityType.MERCH -> merch
        FacilityType.ACADEMY -> academy
    }

    fun withLevel(type: FacilityType, level: Int): Facilities = when (type) {
        FacilityType.TRAINING -> copy(training = level)
        FacilityType.ANALYTICS -> copy(analytics = level)
        FacilityType.PSYCHOLOGY -> copy(psychology = level)
        FacilityType.BOOTCAMP -> copy(bootcamp = level)
        FacilityType.MERCH -> copy(merch = level)
        FacilityType.ACADEMY -> copy(academy = level)
    }
}

fun facilityUpgradeCost(type: FacilityType, currentLevel: Int): Int =
    (type.baseCost * (1.0 + (currentLevel - 1) * 0.85)).toInt()

@Serializable
data class Sponsor(
    val name: String,
    val weekly: Int,
    val signingBonus: Int,
    val weeks: Int = 52
)

@Serializable
data class LeagueTeam(
    val id: String,
    val name: String,
    val strength: Int,
    val isPlayer: Boolean = false,
    val roster: List<String> = emptyList(),
    val wins: Int = 0,
    val losses: Int = 0,
    val mapDiff: Int = 0
) {
    val played: Int get() = wins + losses
    fun winPct(): Double = if (played == 0) 0.0 else wins.toDouble() / played
}

@Serializable
data class PlayoffSeries(
    val round: Int,
    val teamA: String,
    val teamB: String,
    val winsA: Int = 0,
    val winsB: Int = 0,
    val winner: String? = null
)

@Serializable
data class League(
    val discipline: Discipline,
    val tier: Int,
    val teams: List<LeagueTeam>,
    val schedule: List<String> = emptyList(),
    val playoffs: List<PlayoffSeries> = emptyList(),
    val champion: String? = null,
    val userPlacement: Int = 0
) {
    fun team(id: String): LeagueTeam? = teams.firstOrNull { it.id == id }
    fun playerTeam(): LeagueTeam? = teams.firstOrNull { it.isPlayer }

    fun sorted(): List<LeagueTeam> = teams.sortedWith(
        compareByDescending<LeagueTeam> { it.winPct() }
            .thenByDescending { it.mapDiff }
            .thenByDescending { it.wins }
            .thenBy { it.name }
    )

    fun userRank(): Int = sorted().indexOfFirst { it.isPlayer } + 1
    fun tierName(): String = tierLabel(tier)
}

fun tierLabel(tier: Int): String = when (tier) {
    0 -> "Открытая лига"
    1 -> "Contender League"
    2 -> "Pro League"
    else -> "Tier 1"
}

/**
 * Кубок лиги: четыре приглашённых состава, полуфиналы на 3-й неделе и финал на 5-й.
 * Команды — те же LeagueTeam, чтобы не дублировать силу и названия.
 */
@Serializable
data class Cup(
    val discipline: Discipline,
    val tier: Int,
    val teams: List<LeagueTeam>,
    val playoffs: List<PlayoffSeries> = emptyList(),
    val champion: String? = null
) {
    fun team(id: String): LeagueTeam? = teams.firstOrNull { it.id == id }
    fun playerTeam(): LeagueTeam? = teams.firstOrNull { it.isPlayer }
    fun title(): String = "Кубок лиги"
}

@Serializable
data class TransferOffer(
    val id: String,
    val playerId: String,
    val playerNick: String,
    val clubName: String,
    val amount: Int,
    val weeksLeft: Int = 2
)

@Serializable
data class MetaState(
    val patchNumber: Int = 1,
    val title: String = "Патч 1.0 · Стартовая мета",
    val favoredTactic: Tactic = Tactic.BALANCED,
    val favoredAttribute: Attribute = Attribute.GAMESENSE,
    val weeksLeft: Int = 4
)

@Serializable
data class TransferCandidate(
    val player: Player,
    val price: Int,
    val scoutLevel: Int = 0
)

@Serializable
data class Objective(
    val type: ObjectiveType,
    val target: Int,
    val rewardMoney: Int,
    val rewardFans: Int,
    val text: String
)

@Serializable
data class WeekStats(
    val trainings: Int = 0,
    val rests: Int = 0,
    val matches: Int = 0,
    val wins: Int = 0,
    val signings: Int = 0,
    val upgrades: Int = 0,
    val media: Int = 0,
    val scrims: Int = 0
)

fun WeekStats.progress(type: ObjectiveType): Int = when (type) {
    ObjectiveType.TRAIN -> trainings
    ObjectiveType.REST -> rests
    ObjectiveType.PLAY_MATCHES -> matches
    ObjectiveType.WIN_MATCHES -> wins
    ObjectiveType.SIGN_PLAYER -> signings
    ObjectiveType.UPGRADE -> upgrades
    ObjectiveType.MEDIA -> media
}

@Serializable
data class EventEffects(
    val money: Int = 0,
    val fans: Int = 0,
    val moraleCs: Int = 0,
    val moraleDota: Int = 0,
    val fatigueCs: Int = 0,
    val fatigueDota: Int = 0,
    val chemistryCs: Int = 0,
    val chemistryDota: Int = 0,
    val formCs: Int = 0,
    val formDota: Int = 0,
    val ap: Int = 0,
    val news: String? = null
)

@Serializable
data class EventChoice(
    val label: String,
    val hint: String,
    val effects: EventEffects
)

@Serializable
data class GameEvent(
    val title: String,
    val text: String,
    val choices: List<EventChoice>
)

@Serializable
data class MapScore(
    val name: String,
    val us: Int,
    val them: Int,
    val win: Boolean
)

@Serializable
data class PlayerStat(
    val nick: String,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val rating: Double,
    val isUser: Boolean
)

@Serializable
data class MatchResult(
    val id: String,
    val discipline: Discipline,
    val season: Int,
    val week: Int,
    val opponentId: String,
    val opponentName: String,
    val opponentStrength: Int,
    val isPlayoff: Boolean,
    val win: Boolean,
    val seriesUs: Int,
    val seriesThem: Int,
    val maps: List<MapScore>,
    val mvp: String,
    val mvpIsUser: Boolean,
    val stats: List<PlayerStat>,
    val money: Int,
    val fans: Int,
    val dateLabel: String,
    val isCup: Boolean = false,
    val cupRound: Int = 0
)

@Serializable
data class WeekReport(
    val season: Int,
    val week: Int,
    val results: List<MatchResult>,
    val income: Int,
    val expenses: Int,
    val fanDelta: Int,
    val objectiveLines: List<String>,
    val notes: List<String>
)

@Serializable
data class SeasonSummary(
    val season: Int,
    val csPlacement: Int,
    val dotaPlacement: Int,
    val csChampion: String,
    val dotaChampion: String,
    val prize: Int,
    val fanGain: Int,
    val promotedCs: Boolean,
    val relegatedCs: Boolean,
    val promotedDota: Boolean,
    val relegatedDota: Boolean,
    val trophiesWon: List<String>,
    val notes: List<String>
)

@Serializable
data class Settings(
    val sound: Boolean = true,
    val vibration: Boolean = true
)

@Serializable
data class GameState(
    val version: Int = 2,
    val org: Org,
    val money: Int,
    val season: Int,
    val week: Int,
    val actionPoints: Int,
    val difficulty: Int,
    val csTeam: Team,
    val dotaTeam: Team,
    val facilities: Facilities,
    val staff: List<Staff> = emptyList(),
    val staffCandidates: List<Staff> = emptyList(),
    val sponsor: Sponsor? = null,
    val sponsorOffers: List<Sponsor> = emptyList(),
    val csLeague: League,
    val dotaLeague: League,
    val market: List<TransferCandidate> = emptyList(),
    val pendingEvent: GameEvent? = null,
    val objectives: List<Objective> = emptyList(),
    val weekStats: WeekStats = WeekStats(),
    val weekResults: List<MatchResult> = emptyList(),
    val playedThisWeek: List<Discipline> = emptyList(),
    val playedCupMatches: List<String> = emptyList(),
    val csCup: Cup? = null,
    val dotaCup: Cup? = null,
    val incomingOffers: List<TransferOffer> = emptyList(),
    val meta: MetaState = MetaState(),
    val tutorialDone: Boolean = false,
    val lastReport: WeekReport? = null,
    val seasonSummary: SeasonSummary? = null,
    val matchHistory: List<MatchResult> = emptyList(),
    val trophies: List<String> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val news: List<String> = emptyList(),
    val settings: Settings = Settings(),
    val careerWins: Int = 0,
    val careerLosses: Int = 0,
    val totalEarned: Int = 0,
    val bestOvr: Int = 0
) {
    fun teamFor(d: Discipline): Team = if (d == Discipline.CS) csTeam else dotaTeam
    fun leagueFor(d: Discipline): League = if (d == Discipline.CS) csLeague else dotaLeague
    fun cupFor(d: Discipline): Cup? = if (d == Discipline.CS) csCup else dotaCup

    fun withTeam(d: Discipline, team: Team): GameState =
        if (d == Discipline.CS) copy(csTeam = team) else copy(dotaTeam = team)

    fun withLeague(d: Discipline, league: League): GameState =
        if (d == Discipline.CS) copy(csLeague = league) else copy(dotaLeague = league)

    fun withCup(d: Discipline, cup: Cup): GameState =
        if (d == Discipline.CS) copy(csCup = cup) else copy(dotaCup = cup)

    fun staffFor(role: StaffRole): Staff? = staff.firstOrNull { it.role == role }

    fun teamStrength(d: Discipline, playoff: Boolean = false): Double {
        val team = teamFor(d)
        val lineup = team.lineup()
        if (lineup.isEmpty()) return 40.0
        val base = lineup.map { it.effective(playoff) }.average()
        val chem = (team.chemistry - 50) / 30.0
        val coach = staffFor(StaffRole.COACH)?.let { (it.rating - 55) / 18.0 } ?: 0.0
        val analyst = staffFor(StaffRole.ANALYST)?.let { (it.rating - 55) / 24.0 } ?: 0.0
        val analytics = facilities.analytics * 0.35
        return (base + chem + coach + analyst + analytics + team.matchBonus).coerceIn(30.0, 100.0)
    }

    fun playerById(id: String): Player? =
        (csTeam.players + dotaTeam.players).firstOrNull { it.id == id }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fun Int.moneyFull(): String {
    val s = kotlin.math.abs(this).toString().reversed().chunked(3).joinToString(" ").reversed()
    return (if (this < 0) "-$" else "$") + s
}

fun Int.moneyShort(): String = when {
    this >= 1_000_000 -> "$" + String.format(Locale.US, "%.2fM", this / 1_000_000.0)
    this >= 10_000 -> "$" + String.format(Locale.US, "%.0fK", this / 1_000.0)
    else -> moneyFull()
}

fun Int.fansShort(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
    this >= 10_000 -> String.format(Locale.US, "%.0fK", this / 1_000.0)
    else -> toString()
}

fun winChance(myStrength: Double, oppStrength: Int): Double {
    val p = 1.0 / (1.0 + kotlin.math.exp(-(myStrength - oppStrength) / 5.5))
    return p.coerceIn(0.03, 0.97)
}

val ACHIEVEMENT_LIST: List<Achievement> = Achievement.entries
