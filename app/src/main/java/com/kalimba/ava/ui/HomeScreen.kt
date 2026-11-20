package com.kalimba.ava.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalimba.ava.R
import com.kalimba.ava.core.Progress

@Composable
fun HomeScreen(vm: AppViewModel, onOpenLesson: (String) -> Unit, onOpenSong: (String) -> Unit, onTab: (String) -> Unit, onExit: () -> Unit) {
    val s by vm.settings.collectAsState()
    val lp by vm.lessonProgress.collectAsState()
    val sp by vm.songProgress.collectAsState()
    val logs by vm.logs.collectAsState()
    val recs by vm.recordings.collectAsState()
    var confirmExit by remember { mutableStateOf(false) }

    val nextLesson = vm.content.lessons.firstOrNull { lp[it.id]?.completed != true }
    val level = Progress.levelIndex(s.xp)
    val recommended = vm.content.lessons.filter { it.level == level + 1 && lp[it.id]?.completed != true }.getOrNull(1) ?: nextLesson
    val minutes = vm.minutesToday(logs)
    val goalFrac = (minutes.toFloat() / s.dailyGoalMin.coerceAtLeast(1)).coerceIn(0f, 1f)
    val daily = vm.content.songs.let { if (it.isEmpty()) null else it[(vm.today() % it.size).toInt()] }
    val streak = vm.streak(logs)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // hero
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF4A2A18), Color(0xFF211A15), Color(0xFF6B3A20))))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سلام ${s.name} 👋", style = MaterialTheme.typography.titleLarge, color = Color(0xFFF3E3C8), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("به آکادمی کالیمبا آوا خوش آمدی", color = Color(0xFFD9C7AF), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Pill("سطح فعلی: ${Progress.levelName(s.xp)}")
                    Spacer(Modifier.height(6.dp))
                    Text("${faDigits(s.xp)} امتیاز  •  ${faDigits(streak)} روز پیاپی 🔥", color = Color(0xFFD5AA63), style = MaterialTheme.typography.labelLarge)
                }
                Image(painterResource(R.drawable.kalimba_photo), contentDescription = "کالیمبا", modifier = Modifier.height(150.dp), contentScale = ContentScale.Fit)
            }
        }

        // daily goal
        WoodCard {
            Text("هدف تمرین امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { goalFrac }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)))
            Spacer(Modifier.height(6.dp))
            Text("${faDigits(minutes)} از ${faDigits(s.dailyGoalMin)} دقیقه", style = MaterialTheme.typography.bodyMedium)
        }

        // continue learning
        if (nextLesson != null) {
            Button(onClick = { onOpenLesson(nextLesson.id) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp))
                Text("ادامه‌ی یادگیری: ${nextLesson.title}")
            }
        } else {
            WoodCard { Text("🎉 همه‌ی درس‌ها را کامل کرده‌ای! حالا آهنگ‌ها را تمرین کن.", style = MaterialTheme.typography.bodyLarge) }
        }

        if (recommended != null && recommended.id != nextLesson?.id) {
            WoodCard(onClick = { onOpenLesson(recommended.id) }) {
                Text("درس پیشنهادی", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(recommended.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        // daily challenge
        if (daily != null) {
            WoodCard(onClick = { onOpenSong(daily.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("چالش امروز", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("«${daily.titleFa}» را با دقت ۸۰٪ یا بیشتر بنواز", style = MaterialTheme.typography.bodyLarge)
                        val best = sp[daily.id]?.bestAccuracy ?: 0.0
                        if (best > 0) Text("بهترین دقت شما: ${pct(best)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // featured songs
        SectionHeader("آهنگ‌های منتخب")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.content.songs.take(8)) { song ->
                WoodCard(onClick = { onOpenSong(song.id) }, modifier = Modifier.width(160.dp)) {
                    Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                    Text(song.titleFa, maxLines = 2, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(com.kalimba.ava.data.Content.difficultyFa(song.difficulty), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // recent recordings
        SectionHeader("ضبط‌های اخیر")
        if (recs.isEmpty()) Text("هنوز چیزی ضبط نکرده‌ای. از بخش «کالیمبای من» یا ابزارها ضبط کن.", style = MaterialTheme.typography.bodyMedium)
        else recs.take(3).forEach { r ->
            WoodCard(onClick = { onTab("tools") }, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(8.dp))
                    Text(r.name, Modifier.weight(1f)); Text(mmss(r.durationMs), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // weekly progress
        SectionHeader("پیشرفت هفته")
        WeeklyChart(vm.weekly(logs))

        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { confirmExit = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text("خروج از برنامه")
        }
        Text("سازنده: محمد طاها", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
    }

    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("خروج از برنامه") },
        text = { Text("می‌خواهی کالیمبا آوا را ببندی؟ پیشرفت تو ذخیره شده است.") },
        confirmButton = { TextButton(onClick = onExit) { Text("خروج") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("ماندن") } },
    )
}

@Composable
fun WeeklyChart(days: List<DayMinutes>) {
    val max = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(10)
    WoodCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            days.forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                    Text(faDigits(d.minutes), style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.width(22.dp).height((70f * d.minutes / max).coerceAtLeast(3f).dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary))
                    val dow = java.time.LocalDate.ofEpochDay(d.epochDay).dayOfWeek.value // 1 = Monday
                    val idx = (dow + 1) % 7 // Saturday = 0 in the Persian week
                    Text(listOf("ش", "ی", "د", "س", "چ", "پ", "ج")[idx], style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
