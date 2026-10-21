package com.tierone.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tierone.manager.ui.components.NeonBackground
import com.tierone.manager.ui.components.NeonCard
import com.tierone.manager.ui.components.Pill
import com.tierone.manager.ui.components.TeamLogo
import com.tierone.manager.ui.theme.*

private data class DifficultyOption(val title: String, val subtitle: String, val value: Int, val color: Color)

private val difficulties = listOf(
    DifficultyOption("Старт", "Больше денег, соперники слабее", 0, Success),
    DifficultyOption("Баланс", "Классический вызов", 1, Cyan),
    DifficultyOption("Хардкор", "Меньше денег, сильные соперники", 2, Magenta)
)

private val regions = listOf("СНГ", "Европа", "Северная Америка", "Азия", "Бразилия")

@Composable
fun OnboardingScreen(onStart: (String, String, Int, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("СНГ") }
    var difficulty by rememberSaveable { mutableIntStateOf(1) }
    var logoSeed by rememberSaveable { mutableIntStateOf((1000..9999).random()) }

    NeonBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(name.ifBlank { "TIER ONE" }, (name.ifBlank { "TIER ONE" }) + logoSeed, 74.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("TIER ONE", style = MaterialTheme.typography.displayMedium, color = TextMain, fontWeight = FontWeight.Black)
                    Text(
                        "ESPORTS MANAGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                IconButton(
                    onClick = { logoSeed = (1000..9999).random() },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, Outline, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Rounded.Casino, "Сменить эмблему", tint = Gold)
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "Соберите организацию, выведите составы из открытой лиги и станьте легендой CS2 и Dota 2.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("CS2 + Dota 2", Cyan)
                Pill("Офлайн", Magenta)
                Pill("4 действия", Gold)
            }

            Spacer(Modifier.height(22.dp))
            Text("НАЗВАНИЕ ОРГАНИЗАЦИИ", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 18) name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например, Nova Esports", color = TextDim.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = Outline,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    cursorColor = Cyan,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain
                )
            )

            Spacer(Modifier.height(20.dp))
            Text("РЕГИОН", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                regions.forEach { r ->
                    val active = r == region
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (active) Cyan.copy(alpha = 0.15f) else Surface)
                            .border(1.dp, if (active) Cyan else Outline, RoundedCornerShape(11.dp))
                            .clickable { region = r }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(r, color = if (active) Cyan else TextDim, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("СЛОЖНОСТЬ", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            difficulties.forEach { option ->
                val active = option.value == difficulty
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) option.color.copy(alpha = 0.10f) else Surface)
                        .border(1.dp, if (active) option.color.copy(alpha = 0.7f) else Outline, RoundedCornerShape(14.dp))
                        .clickable { difficulty = option.value }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) option.color else Outline)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.title, style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Text(option.subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            NeonCard(Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.3f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Groups, null, tint = Cyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Два состава с первого дня", style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Text(
                            "5 игроков в CS2 и 5 в Dota 2. Общий бюджет, штат и инфраструктура.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onStart(name.trim().ifBlank { "Nova Esports" }, region, difficulty, logoSeed) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg)
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("СОЗДАТЬ ОРГАНИЗАЦИЮ", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Прогресс сохраняется автоматически. Игра полностью офлайн.",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
