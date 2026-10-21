package com.tierone.manager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tierone.manager.ui.components.NeonBackground
import com.tierone.manager.ui.screens.*
import com.tierone.manager.ui.theme.*

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("hub", "Штаб", Icons.Rounded.Dashboard),
    NavItem("roster", "Состав", Icons.Rounded.Groups),
    NavItem("calendar", "Календарь", Icons.Rounded.CalendarMonth),
    NavItem("market", "Рынок", Icons.Rounded.Storefront),
    NavItem("club", "Клуб", Icons.Rounded.Business)
)

@Composable
fun TierOneApp(vm: GameViewModel = viewModel()) {
    val state = vm.state
    if (state == null) {
        OnboardingScreen { name, region, difficulty, logoSeed ->
            vm.newGame(name, region, difficulty, logoSeed)
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route == null || !route.startsWith("player")

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Surface.copy(alpha = 0.98f),
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = route == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Cyan,
                                    selectedTextColor = Cyan,
                                    indicatorColor = Cyan.copy(alpha = 0.12f),
                                    unselectedIconColor = TextDim,
                                    unselectedTextColor = TextDim
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "hub",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("hub") {
                    HubScreen(
                        state = state,
                        onBeginWeek = vm::beginWeek,
                        onTrainTeam = vm::trainTeam,
                        onTrainPlayer = vm::trainPlayer,
                        onRest = vm::restTeam,
                        onScrim = vm::scrim,
                        onMedia = vm::mediaDay,
                        onScout = vm::scoutMarket,
                        onGoClub = { navController.navigate("club") }
                    )
                }
                composable("roster") {
                    RosterScreen(state = state, onPlayerClick = { id ->
                        navController.navigate("player/$id")
                    })
                }
                composable("calendar") { CalendarScreen(state) }
                composable("market") {
                    MarketScreen(
                        state = state,
                        onScoutCandidate = vm::scoutCandidate,
                        onSignPlayer = vm::signPlayer,
                        onNegotiate = vm::negotiatePlayer,
                        onAcceptOffer = vm::acceptIncomingOffer,
                        onRejectOffer = vm::rejectIncomingOffer,
                        onRefresh = vm::scoutMarket
                    )
                }
                composable("club") {
                    ClubScreen(
                        state = state,
                        onUpgrade = vm::upgradeFacility,
                        onHireStaff = vm::hireStaff,
                        onSignSponsor = vm::signSponsor,
                        onSettings = vm::setSettings,
                        onNewCareer = vm::resetCareer
                    )
                }
                composable("player/{playerId}") { entry ->
                    val id = entry.arguments?.getString("playerId") ?: ""
                    PlayerDetailScreen(
                        state = state,
                        playerId = id,
                        onBack = { navController.popBackStack() },
                        onTrain = vm::trainPlayer,
                        onSell = { playerId ->
                            vm.sellPlayer(playerId)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    when {
        vm.matchday != null -> MatchDayScreen(
            state = state,
            matchday = vm.matchday!!,
            live = vm.live,
            resultApplied = vm.resultApplied,
            onStart = vm::startMatch,
            onDecision = vm::applyDecision,
            onAnimationDone = vm::onMatchAnimationDone,
            onNext = vm::nextAfterResult,
            onExit = vm::exitMatchday
        )
        state.seasonSummary != null -> SeasonSummaryScreen(
            state = state,
            summary = state.seasonSummary,
            onContinue = vm::dismissSeasonSummary
        )
        state.lastReport != null -> WeekReportScreen(
            state = state,
            report = state.lastReport,
            onContinue = vm::dismissReport
        )
        state.pendingEvent != null -> EventDialog(
            event = state.pendingEvent,
            onChoose = vm::chooseEvent
        )
        !state.tutorialDone -> TutorialDialog(onClose = vm::completeTutorial)
    }
}
