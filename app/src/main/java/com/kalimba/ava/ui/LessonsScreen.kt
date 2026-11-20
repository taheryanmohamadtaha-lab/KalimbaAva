package com.kalimba.ava.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalimba.ava.core.NoteEvent
import com.kalimba.ava.core.SongParser
import com.kalimba.ava.data.Content
import com.kalimba.ava.data.Lesson
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun LessonsScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val lp by vm.lessonProgress.collectAsState()
    val lessons = vm.content.lessons
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("آموزش کالیمبا", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        for (level in 1..4) {
            val inLevel = lessons.filter { it.level == level }
            if (inLevel.isEmpty()) continue
            val done = inLevel.count { lp[it.id]?.completed == true }
            item {
                Spacer(Modifier.height(6.dp))
                Text(Content.LEVEL_TITLES.getValue(level), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { done.toFloat() / inLevel.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(8.dp))
                Text("${faDigits(done)} از ${faDigits(inLevel.size)} درس", style = MaterialTheme.typography.labelMedium)
            }
            items(inLevel, key = { it.id }) { l ->
                val completed = lp[l.id]?.completed == true
                WoodCard(onClick = { onOpen(l.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                            tint = if (completed) com.kalimba.ava.ui.Wood.Good else MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(10.dp))
                        Text(l.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (l.exercise != null) Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonDetailScreen(vm: AppViewModel, lessonId: String, onBack: () -> Unit, onPractice: (String) -> Unit) {
    val lesson: Lesson = vm.content.lessonById(lessonId) ?: run { onBack(); return }
    val s by vm.settings.collectAsState()
    val lp by vm.lessonProgress.collectAsState()
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(emptySet<Int>()) }
    var demoJob by remember { mutableStateOf<Job?>(null) }
    var picked by remember(lessonId) { mutableIntStateOf(-1) }
    var checked by remember(lessonId) { mutableStateOf(false) }
    val completed = lp[lesson.id]?.completed == true
    val demo: List<NoteEvent> = remember(lessonId) { if (lesson.demo.isBlank()) emptyList() else SongParser.parseSequence(lesson.demo).getOrDefault(emptyList()) }

    DisposableEffect(Unit) { onDispose { demoJob?.cancel() } }

    Column(Modifier.fillMaxSize()) {
        TopBar(lesson.title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(Content.LEVEL_TITLES.getValue(lesson.level))
                if (completed) { Spacer(Modifier.width(8.dp)); Pill("✓ تکمیل‌شده") }
            }
            lesson.body.forEach { Text(it, style = MaterialTheme.typography.bodyLarge, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f) }

            Box(Modifier.fillMaxWidth().aspectRatio(1.0f)) {
                KalimbaInstrument(
                    activeKeys = active, guideKeys = lesson.highlightKeys.toSet(),
                    showNames = s.showNoteNames, showNumbers = s.showNumbers,
                    onPress = { vm.audio.playKey(it) },
                )
            }
            Text("روی کلیدها بزن تا صدایشان را بشنوی. کلیدهای سبز مربوط به این درس‌اند.", style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (demo.isNotEmpty()) FilledTonalButton(onClick = {
                    demoJob?.cancel()
                    demoJob = scope.launch { playEvents(vm.audio, demo, 80.0) { active = it } }
                }, Modifier.weight(1f)) { Icon(Icons.Filled.VolumeUp, null); Spacer(Modifier.width(6.dp)); Text("شنیدن نمونه") }
                if (lesson.exercise != null) Button(onClick = { onPractice("lesson:${lesson.id}") }, Modifier.weight(1f)) {
                    Icon(Icons.Filled.MusicNote, null); Spacer(Modifier.width(6.dp)); Text("تمرین تعاملی")
                }
            }

            // quiz
            WoodCard(Modifier.fillMaxWidth()) {
                Text("آزمونک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(lesson.quiz.question, style = MaterialTheme.typography.bodyLarge)
                lesson.quiz.options.forEachIndexed { i, opt ->
                    Row(Modifier.fillMaxWidth().selectable(selected = picked == i, onClick = { picked = i; checked = false }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = picked == i, onClick = { picked = i; checked = false })
                        Text(opt)
                    }
                }
                Button(onClick = { checked = true; if (picked == lesson.quiz.answer) vm.completeLesson(lesson.id) }, enabled = picked >= 0) { Text("بررسی پاسخ") }
                if (checked) {
                    val ok = picked == lesson.quiz.answer
                    Spacer(Modifier.height(6.dp))
                    Text(if (ok) "✅ درست است! ${lesson.quiz.explanation}" else "❌ نادرست. دوباره فکر کن و دوباره امتحان کن.",
                        color = if (ok) Wood.Good else MaterialTheme.colorScheme.error)
                }
            }
            if (!completed && lesson.exercise != null) {
                OutlinedButton(onClick = { vm.completeLesson(lesson.id) }, Modifier.fillMaxWidth()) { Text("علامت‌گذاری به‌عنوان تکمیل‌شده") }
            }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
        }
    }
}
