package com.example.voronezhfishing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// 1. БАЗА ДАННЫХ ВОДОЕМОВ И РЫБ
data class WaterBody(val name: String, val fishes: List<String>)

val voronezhWaterBodies = listOf(
    WaterBody("Река Дон (вкл. Лиски)", listOf("Щука", "Окунь", "Судак", "Лещ", "Сом", "Жерех", "Раки")),
    WaterBody("Воронежское водохранилище", listOf("Окунь", "Плотва", "Карась", "Щука", "Лещ", "Толстолобик")),
    WaterBody("Река Усманка", listOf("Щука-травянка", "Плотва", "Красноперка", "Линь", "Окунь")),
    WaterBody("Река Воронеж", listOf("Голавль", "Язь", "Щука", "Сом", "Лещ", "Плотва"))
)

// 2. АЛГОРИТМ ОЦЕНКИ КЛЕВА
fun calculateBiteActivity(pressure: Int, temp: Int, moonPhase: String): Int {
    var score = 100
    // Идеальное давление для Воронежской области ~745-755 мм рт.ст.
    if (pressure !in 745..755) score -= 25
    // Экстремальная температура воды снижает активность
    if (temp < 4 || temp > 25) score -= 20
    // Луна: Полнолуние часто снижает клев мирной рыбы
    if (moonPhase == "Полнолуние") score -= 15
    
    return score.coerceIn(0, 100)
}

fun getBiteStatus(score: Int): String {
    return when {
        score >= 80 -> "Фаза активного клёва (Жор)"
        score >= 50 -> "Умеренный клёв (Норма)"
        else -> "Спокойствие (Клёв слабый)"
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FishingApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingApp() {
    var expanded by remember { mutableStateOf(false) }
    var selectedBody by remember { mutableStateOf(voronezhWaterBodies[0]) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Имитация загруженных данных (в реальном приложении здесь работает Retrofit + Open-Meteo API)
    val todayScore = calculateBiteActivity(750, 18, "Растущая")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Прогноз клёва: Воронеж") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showBottomSheet = true }) {
                Text("Оценить клёв")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            
            // Выбор водоема
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedBody.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Выберите водоем") },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    voronezhWaterBodies.forEach { body ->
                        DropdownMenuItem(
                            text = { Text(body.name) },
                            onClick = { 
                                selectedBody = body
                                expanded = false 
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // 3-дневный календарь
            Text("Прогноз на 3 дня", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForecastCard(LocalDate.now(), "Сегодня", todayScore)
                ForecastCard(LocalDate.now().plusDays(1), "Завтра", calculateBiteActivity(740, 19, "Растущая"))
                ForecastCard(LocalDate.now().plusDays(2), "Послезавтра", calculateBiteActivity(765, 17, "Полнолуние"))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Возможный улов:", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(selectedBody.fishes) { fish ->
                    ListItem(headlineContent = { Text(fish) })
                }
            }
        }

        // Всплывающее окно с аналитикой
        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Анализ на сегодня", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Активность: $todayScore%")
                    Text("Статус: ${getBiteStatus(todayScore)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Давление: 750 мм рт. ст. (Стабильное)")
                    Text("Температура: 18°C")
                    Text("Луна: Растущая")
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ForecastCard(date: LocalDate, title: String, score: Int) {
    Card(modifier = Modifier.width(110.dp).height(100.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text("${date.dayOfMonth}.${date.monthValue}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.weight(1f))
            Text("$score%", style = MaterialTheme.typography.titleLarge)
        }
    }
}
