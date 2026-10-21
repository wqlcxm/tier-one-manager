package com.tierone.manager.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

object Generator {

    private val nickPartsA = listOf(
        "s1", "k", "v", "n0", "m1", "zy", "neo", "dark", "ice", "flame",
        "void", "rush", "byte", "sh4", "ka", "re", "pro", "apex", "nova", "zero",
        "sky", "hex", "luc", "kro", "fro", "zed", "arc", "oni", "tox", "ven",
        "dr1", "m4", "sn", "cr", "bl", "gl", "ph", "tr", "qu", "xa"
    )
    private val nickPartsB = listOf(
        "ko", "n", "x", "ro", "ce", "fy", "ze", "tor", "on", "ix",
        "us", "er", "ka", "lo", "mi", "ra", "s", "t", "v", "ne",
        "ck", "de", "mo", "ny", "r1", "sh", "ly", "zz", "1k", "7"
    )
    private val firstNames = listOf(
        "Александр", "Даниил", "Максим", "Артём", "Иван", "Кирилл", "Никита", "Егор",
        "Матвей", "Роман", "Дмитрий", "Сергей", "Андрей", "Михаил", "Павел", "Владислав",
        "Lukas", "Oliver", "Noah", "Elias", "Hugo", "Mateo", "Lucas", "Leo",
        "Min-jun", "Wei", "Hao", "Yuki", "Rafael", "Diego", "Gabriel", "Pedro"
    )
    private val lastNames = listOf(
        "Смирнов", "Кузнецов", "Попов", "Соколов", "Лебедев", "Козлов", "Новиков", "Морозов",
        "Волков", "Зайцев", "Павлов", "Семёнов", "Голубев", "Виноградов", "Богданов", "Воробьёв",
        "Nilsson", "Larsson", "Müller", "Schmidt", "Silva", "Santos", "Kim", "Park",
        "Chen", "Zhang", "Tanaka", "Sato", "Garcia", "Lopez", "Costa", "Oliveira"
    )
    private val countries = listOf(
        "RU", "RU", "RU", "UA", "KZ", "BY", "SE", "DK", "FI", "DE",
        "PL", "FR", "GB", "US", "CA", "BR", "AR", "CN", "KR", "JP", "TR", "PT"
    )
    private val teamPrefixes = listOf(
        "Nebula", "Vortex", "Phantom", "Iron", "Solar", "Night", "Apex", "Crimson",
        "Echo", "Quantum", "Rift", "Zenith", "Onyx", "Fable", "Nova", "Havoc",
        "Storm", "Frost", "Ember", "Cipher", "Titan", "Lunar", "Pixel", "Rogue",
        "Vector", "Drift", "Halo", "Karma", "Mirage", "Pulse", "Sable", "Vertex"
    )
    private val teamSuffixes = listOf("Gaming", "Esports", "Club", "Squad", "Team", "GG")

    private val staffNames = listOf(
        "Артём Белов", "Дмитрий Орлов", "Максим Крылов", "Сергей Волков", "Анна Соколова",
        "Егор Лебедев", "Niklas Berg", "Oliver Stone", "Marta Silva", "Kim Min-ho",
        "Владислав Новиков", "Ирина Морозова", "Pavel Novak", "Daniel Reed", "Sofia Costa"
    )

    private val sponsorNames = listOf(
        "Volt Energy", "Nebula Bank", "Aero Cola", "Quantum Peripherals",
        "StarNet", "Forge Motors", "Hyper Cell", "Northwind", "PixelPay",
        "Titan Nutrition", "Orbit Audio", "ZeroCool"
    )

    private fun id(): String = java.util.UUID.randomUUID().toString().take(10)

    fun generatedNick(used: MutableSet<String>, rng: Random): String {
        repeat(80) {
            val a = nickPartsA.random(rng)
            val b = if (rng.nextBoolean()) nickPartsB.random(rng) else ""
            val digits = if (rng.nextInt(100) < 22) rng.nextInt(1, 100).toString() else ""
            val candidate = a + b + digits
            if (candidate.length >= 3 && used.add(candidate.lowercase())) return candidate
        }
        val fallback = "player" + rng.nextInt(1000, 9999)
        used.add(fallback)
        return fallback
    }

    fun generatePlayer(
        role: Role,
        targetOvr: Int,
        rng: Random,
        usedNicks: MutableSet<String>,
        potentialBoost: Int = 0
    ): Player {
        val target = targetOvr.coerceIn(42, 90)
        fun attr(offset: Int = 0) = (target + rng.nextInt(-8, 9) + offset).coerceIn(30, 96)
        val age = rng.nextInt(18, 28)
        val traitRoll = rng.nextInt(100)
        val trait = when {
            traitRoll < 58 -> PlayerTrait.NONE
            traitRoll < 66 -> PlayerTrait.LEADER
            traitRoll < 74 -> PlayerTrait.CLUTCH
            traitRoll < 81 -> PlayerTrait.GRINDER
            traitRoll < 87 -> PlayerTrait.PRODIGY
            traitRoll < 92 -> PlayerTrait.VETERAN
            traitRoll < 96 -> PlayerTrait.TOXIC
            else -> PlayerTrait.FRAGILE
        }
        var player = Player(
            id = id(),
            nick = generatedNick(usedNicks, rng),
            name = "${firstNames.random(rng)} ${lastNames.random(rng)}",
            country = countries.random(rng),
            age = age,
            role = role,
            mechanics = attr(if (role == Role.AWPER || role == Role.RIFLER || role == Role.CARRY) 3 else 0),
            gamesense = attr(if (role == Role.IGL || role == Role.MID) 3 else 0),
            teamwork = attr(if (role == Role.SUPPORT || role == Role.HARD_SUPPORT) 4 else 0),
            mentality = attr(if (role == Role.ENTRY || role == Role.OFFLANE) 3 else 0),
            stamina = attr(),
            potential = (target + rng.nextInt(3, 18) + potentialBoost + if (trait == PlayerTrait.PRODIGY) 6 else 0)
                .coerceIn(target, 99),
            form = rng.nextInt(45, 66),
            morale = rng.nextInt(60, 82),
            fatigue = rng.nextInt(0, 12),
            salary = suggestedSalary(target, 0),
            contractWeeks = rng.nextInt(26, 70),
            trait = trait
        )
        var guard = 0
        while (player.ovr() < target - 2 && guard < 20) {
            player = player.copy(
                mechanics = (player.mechanics + 1).coerceAtMost(97),
                gamesense = (player.gamesense + 1).coerceAtMost(97),
                teamwork = (player.teamwork + 1).coerceAtMost(97)
            )
            guard++
        }
        player = player.copy(salary = suggestedSalary(player.ovr(), 0))
        return player
    }

    fun suggestedSalary(ovr: Int, tier: Int): Int =
        (600 + max(1, ovr - 40).toDouble().pow(1.75) * 11 + tier * 450).toInt()

    fun generateTeam(discipline: Discipline, orgName: String, rng: Random): Team {
        val used = mutableSetOf<String>()
        val targetBase = rng.nextInt(56, 61)
        val players = Role.forDiscipline(discipline).map { role ->
            generatePlayer(role, targetBase + rng.nextInt(-2, 3), rng, used)
        }
        val toxic = players.count { it.trait == PlayerTrait.TOXIC }
        return Team(
            discipline = discipline,
            name = orgName,
            players = players,
            chemistry = (rng.nextInt(58, 74) - toxic * 6).coerceIn(20, 95)
        )
    }

    fun generateLeague(
        discipline: Discipline,
        tier: Int,
        orgName: String,
        difficulty: Int,
        rng: Random
    ): League {
        val usedNames = mutableSetOf(orgName.lowercase())
        val teams = mutableListOf(
            LeagueTeam(id = "PLAYER", name = orgName, strength = 0, isPlayer = true)
        )
        val base = listOf(51, 59, 67, 75)[tier.coerceIn(0, 3)] + difficulty * 2
        repeat(5) {
            var name: String
            var guard = 0
            do {
                name = teamPrefixes.random(rng) + " " + teamSuffixes.random(rng)
                guard++
            } while (!usedNames.add(name.lowercase()) && guard < 40)
            val nicks = mutableSetOf<String>()
            val roster = List(5) { generatedNick(nicks, rng) }
            teams += LeagueTeam(
                id = id(),
                name = name,
                strength = (base + rng.nextInt(-4, 6)).coerceIn(40, 95),
                roster = roster
            )
        }
        val aiIds = teams.filter { !it.isPlayer }.map { it.id }.shuffled(rng)
        return League(
            discipline = discipline,
            tier = tier,
            teams = teams,
            schedule = aiIds
        )
    }

    fun generateMarket(
        tier: Int,
        academyLevel: Int,
        rng: Random,
        count: Int = 12,
        scoutRating: Int = 50
    ): List<TransferCandidate> {
        val used = mutableSetOf<String>()
        val base = listOf(55, 62, 69, 76)[tier.coerceIn(0, 3)] + rng.nextInt(-3, 5)
        val roles = (Role.forDiscipline(Discipline.CS) + Role.forDiscipline(Discipline.DOTA))
        val scoutBonus = when {
            scoutRating >= 78 -> 2
            scoutRating >= 62 -> 1
            else -> 0
        }
        val effectiveCount = count + if (scoutRating >= 75) 2 else 0
        return List(effectiveCount) { index ->
            val role = roles[index % roles.size]
            val p = generatePlayer(
                role = role,
                targetOvr = (base + rng.nextInt(-6, 6)).coerceIn(45, 88),
                rng = rng,
                usedNicks = used,
                potentialBoost = academyLevel - 1 + scoutBonus
            ).copy(salary = suggestedSalary(base, tier))
            TransferCandidate(
                player = p,
                price = p.marketValue(),
                scoutLevel = 0
            )
        }
    }

    fun refreshMarketPlayer(candidate: TransferCandidate, tier: Int, rng: Random): TransferCandidate {
        val used = mutableSetOf(candidate.player.nick)
        val base = listOf(55, 62, 69, 76)[tier.coerceIn(0, 3)] + rng.nextInt(-4, 6)
        val p = generatePlayer(
            role = candidate.player.role,
            targetOvr = (base + rng.nextInt(-4, 5)).coerceIn(45, 88),
            rng = rng,
            usedNicks = used
        )
        return candidate.copy(player = p, price = p.marketValue(), scoutLevel = 0)
    }

    fun generateStaffCandidates(tier: Int, rng: Random, count: Int = 8): List<Staff> {
        val roles = StaffRole.entries
        return List(count) { index ->
            val role = roles[index % roles.size]
            val rating = (46 + tier * 6 + rng.nextInt(0, 16)).coerceIn(40, 92)
            Staff(
                id = id(),
                role = role,
                name = staffNames.random(rng),
                rating = rating,
                salary = (700 + (rating - 40).toDouble().pow(1.55) * 11).toInt(),
                specialty = if ((role == StaffRole.COACH || role == StaffRole.ANALYST) && rng.nextInt(100) < 72) {
                    Tactic.entries.random(rng)
                } else null
            )
        }
    }

    fun generateCup(discipline: Discipline, league: League, rng: Random): Cup {
        val player = league.playerTeam()
        val ai = league.teams.filter { !it.isPlayer }.shuffled(rng).take(3)
        val participants = (listOfNotNull(player) + ai).shuffled(rng)
        val semis = if (participants.size >= 4) {
            listOf(
                PlayoffSeries(round = 1, teamA = participants[0].id, teamB = participants[1].id),
                PlayoffSeries(round = 1, teamA = participants[2].id, teamB = participants[3].id)
            )
        } else emptyList()
        return Cup(
            discipline = discipline,
            tier = league.tier,
            teams = participants,
            playoffs = semis
        )
    }

    fun generateMeta(patchNumber: Int, rng: Random): MetaState {
        val names = listOf(
            "Перелом", "Шторм", "Рассвет", "Тихий патч", "Гонка вооружений",
            "Новая ось", "Холодный расчёт", "Открытая мета", "Смена ритма"
        )
        return MetaState(
            patchNumber = patchNumber,
            title = "Патч $patchNumber.0 · ${names.random(rng)}",
            favoredTactic = Tactic.entries.random(rng),
            favoredAttribute = Attribute.entries.random(rng),
            weeksLeft = 4
        )
    }

    fun generateSponsorOffers(tier: Int, fans: Int, rng: Random): List<Sponsor> {
        val names = sponsorNames.shuffled(rng).take(3)
        return names.map { name ->
            val weekly = ((12_000 + tier * 10_000 + fans / 1200) * rng.nextDouble(0.85, 1.3)).toInt()
            Sponsor(name = name, weekly = weekly, signingBonus = weekly * 3)
        }
    }

    fun generateObjectives(tier: Int, rng: Random): List<Objective> {
        val pool = listOf(
            Objective(ObjectiveType.TRAIN, 2, 8_000 + tier * 2_000, 0, "Проведите 2 тренировки"),
            Objective(ObjectiveType.REST, 1, 5_000 + tier * 1_000, 100, "Дайте составу отдохнуть"),
            Objective(ObjectiveType.PLAY_MATCHES, 2, 10_000 + tier * 2_500, 0, "Сыграйте 2 официальных матча"),
            Objective(ObjectiveType.WIN_MATCHES, 1, 12_000 + tier * 3_000, 400 + tier * 150, "Выиграйте официальный матч"),
            Objective(ObjectiveType.MEDIA, 1, 7_000 + tier * 1_500, 200, "Проведите медиа-день"),
            Objective(ObjectiveType.SIGN_PLAYER, 1, 0, 600 + tier * 200, "Подпишите нового игрока"),
            Objective(ObjectiveType.UPGRADE, 1, 6_000, 100, "Улучшите инфраструктуру клуба")
        )
        return pool.shuffled(rng).take(2)
    }

    fun generateEvent(state: GameState, rng: Random): GameEvent {
        val tier = max(state.csLeague.tier, state.dotaLeague.tier)
        val star = (state.csTeam.players + state.dotaTeam.players)
            .maxByOrNull { it.ovr() }
        val starName = star?.nick ?: "Звезда"
        val starSalary = star?.salary ?: 3_000
        val events = listOf(
            GameEvent(
                title = "Разговор о деньгах",
                text = "$starName приходит к вам после тренировки. Его агент считает, что зарплата не соответствует уровню игры, и просит прибавку. В раздевалке ждут вашего ответа.",
                choices = listOf(
                    EventChoice(
                        "Поднять зарплату", "−${(starSalary * 4).moneyFull()} · мораль и форма вырастут",
                        EventEffects(
                            money = -starSalary * 4,
                            moraleCs = 6, moraleDota = 6,
                            formCs = 2, formDota = 2,
                            news = "$starName получил прибавку и обещает тащить."
                        )
                    ),
                    EventChoice(
                        "Отказать", "Мораль упадёт, но деньги останутся",
                        EventEffects(
                            moraleCs = -7, moraleDota = -7,
                            chemistryCs = -2, chemistryDota = -2,
                            news = "Агент $starName остался недоволен разговором."
                        )
                    ),
                    EventChoice(
                        "Бонус за результат", "−${(starSalary * 2).moneyFull()} · форма +, мораль чуть выше",
                        EventEffects(
                            money = -starSalary * 2,
                            moraleCs = 3, moraleDota = 3,
                            formCs = 3, formDota = 3,
                            news = "Команда получила бонус за результат."
                        )
                    )
                )
            ),
            GameEvent(
                title = "Патч меняет мету",
                text = "Разработчики выпустили большой патч. Привычные схемы работают хуже, соперники уже тестируют новинки. Нужно решить, как провести неделю.",
                choices = listOf(
                    EventChoice(
                        "Разобрать новый патч", "Форма +4, но усталость +6",
                        EventEffects(formCs = 4, formDota = 4, fatigueCs = 6, fatigueDota = 6,
                            news = "Штаб разобрал новый патч до мелочей.")
                    ),
                    EventChoice(
                        "Играть на старых схемах", "Химия +3, форма −1",
                        EventEffects(chemistryCs = 3, chemistryDota = 3, formCs = -1, formDota = -1,
                            news = "Команда решила не ломать свои сильные стороны.")
                    ),
                    EventChoice(
                        "Дать выходной", "Усталость −10, форма −2",
                        EventEffects(fatigueCs = -10, fatigueDota = -10, formCs = -2, formDota = -2,
                            news = "Состав перезагрузился перед решающими матчами.")
                    )
                )
            ),
            GameEvent(
                title = "Спонсорский ивент",
                text = "Известный бренд предлагает провести рекламную съёмку с составом. График плотный, но отказ может ударить по репутации.",
                choices = listOf(
                    EventChoice(
                        "Согласиться", "+${(18_000 + tier * 8_000).moneyFull()} · усталость +5",
                        EventEffects(money = 18_000 + tier * 8_000, fatigueCs = 5, fatigueDota = 5,
                            news = "Рекламная съёмка пополнила бюджет клуба.")
                    ),
                    EventChoice(
                        "Сделать контент для фанатов", "Фанаты +1500 · усталость +3",
                        EventEffects(fans = 1_500 + tier * 500, fatigueCs = 3, fatigueDota = 3,
                            news = "Фанаты оценили новый контент с игроками.")
                    ),
                    EventChoice(
                        "Отказаться", "Мораль +2, без усталости",
                        EventEffects(moraleCs = 2, moraleDota = 2,
                            news = "Клуб отказался от съёмок ради тренировок.")
                    )
                )
            ),
            GameEvent(
                title = "Конфликт на буткемпе",
                text = "После серии поражений на командном ужине вспыхивает ссора. Два игрока не сходятся во взглядах на игру. Все смотрят на вас.",
                choices = listOf(
                    EventChoice(
                        "Жёстко разобрать ошибки", "Химия −5 · форма +5",
                        EventEffects(chemistryCs = -5, chemistryDota = -5, formCs = 5, formDota = 5,
                            news = "Жёсткий разбор помог команде собраться.")
                    ),
                    EventChoice(
                        "Поговорить тет-а-тет", "Химия +5 · мораль +4",
                        EventEffects(chemistryCs = 5, chemistryDota = 5, moraleCs = 4, moraleDota = 4,
                            news = "Конфликт удалось погасить до матчей.")
                    ),
                    EventChoice(
                        "Игнорировать", "Мораль −5 · химия −2",
                        EventEffects(moraleCs = -5, moraleDota = -5, chemistryCs = -2, chemistryDota = -2,
                            news = "Напряжение в команде осталось.")
                    )
                )
            ),
            GameEvent(
                title = "Трансферный слух",
                text = "Инсайдер пишет, что вашего игрока хотят купить в другой клуб. Журналисты уже звонят. Нужно срочно сформулировать позицию.",
                choices = listOf(
                    EventChoice(
                        "Публично опровергнуть", "Фанаты +800 · мораль +2",
                        EventEffects(fans = 800 + tier * 300, moraleCs = 2, moraleDota = 2,
                            news = "Клуб публично закрыл тему трансфера.")
                    ),
                    EventChoice(
                        "Промолчать", "Мораль −3 · форма +2",
                        EventEffects(moraleCs = -3, moraleDota = -3, formCs = 2, formDota = 2,
                            news = "Слухи о трансфере не утихают.")
                    ),
                    EventChoice(
                        "Открыть переговоры", "+${(30_000 + tier * 15_000).moneyFull()} · мораль −6",
                        EventEffects(money = 30_000 + tier * 15_000, moraleCs = -6, moraleDota = -6,
                            news = "Клуб получил компенсацию за переговоры, но раздевалка нервничает.")
                    )
                )
            ),
            GameEvent(
                title = "Медиа-день",
                text = "Популярный стример предлагает провести совместный стрим с составом. Это хороший шанс для роста аудитории, но время тренировок придётся сдвинуть.",
                choices = listOf(
                    EventChoice(
                        "Провести стрим", "Фанаты +2000 · усталость +7",
                        EventEffects(fans = 2_000 + tier * 600, fatigueCs = 7, fatigueDota = 7,
                            news = "Стрим с составом собрал рекордную аудиторию.")
                    ),
                    EventChoice(
                        "Только короткое интервью", "Фанаты +700 · усталость +2",
                        EventEffects(fans = 700, fatigueCs = 2, fatigueDota = 2,
                            news = "Игроки дали короткое интервью и вернулись к тренировкам.")
                    ),
                    EventChoice(
                        "Отказаться", "Форма +2",
                        EventEffects(formCs = 2, formDota = 2,
                            news = "Клуб отказался от стрима ради подготовки.")
                    )
                )
            ),
            GameEvent(
                title = "Проблемы с базой",
                text = "В тренировочном помещении сломался интернет. Провайдер обещает починить только через неделю. Нужно как-то спасать подготовку.",
                choices = listOf(
                    EventChoice(
                        "Арендовать коворкинг", "−${(12_000 + tier * 6_000).moneyFull()} · форма +3",
                        EventEffects(money = -(12_000 + tier * 6_000), formCs = 3, formDota = 3,
                            news = "Клуб арендовал помещение с нормальным интернетом.")
                    ),
                    EventChoice(
                        "Играть на мобильном", "Химия +3 · форма −2",
                        EventEffects(chemistryCs = 3, chemistryDota = 3, formCs = -2, formDota = -2,
                            news = "Игроки с юмором пережили неделю на мобильном интернете.")
                    ),
                    EventChoice(
                        "Устроить теорию", "Геймсенс растёт · усталость −3",
                        EventEffects(formCs = 1, formDota = 1, fatigueCs = -3, fatigueDota = -3,
                            news = "Штаб провёл неделю теории без практики.")
                    )
                )
            )
        )
        return events.random(rng)
    }

    fun initialNews(orgName: String): List<String> = listOf(
        "$orgName основана. Добро пожаловать в киберспорт!",
        "Штаб сформировал составы по CS2 и Dota 2.",
        "Цель на сезон: выйти из открытой лиги."
    )
}

fun flagOf(code: String): String = when (code.uppercase()) {
    "RU" -> "🇷🇺"
    "UA" -> "🇺🇦"
    "KZ" -> "🇰🇿"
    "BY" -> "🇧🇾"
    "SE" -> "🇸🇪"
    "DK" -> "🇩🇰"
    "FI" -> "🇫🇮"
    "DE" -> "🇩🇪"
    "PL" -> "🇵🇱"
    "FR" -> "🇫🇷"
    "GB" -> "🇬🇧"
    "US" -> "🇺🇸"
    "CA" -> "🇨🇦"
    "BR" -> "🇧🇷"
    "AR" -> "🇦🇷"
    "CN" -> "🇨🇳"
    "KR" -> "🇰🇷"
    "JP" -> "🇯🇵"
    "TR" -> "🇹🇷"
    "PT" -> "🇵🇹"
    else -> "🌍"
}
