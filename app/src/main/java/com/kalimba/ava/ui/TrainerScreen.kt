package com.kalimba.ava.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalimba.ava.audio.MicAnalyzer
import com.kalimba.ava.core.*
import kotlinx.coroutines.*
import kotlin.math.ceil

enum class PlayMode(val fa: String, val key: String) { GUIDED("راهنما", "guided"), PRACTICE("تمرین", "practice"), PERFORMANCE("اجرا", "performance") }

private val SPEEDS = listOf(25, 50, 60, 75, 90, 100)

/** Interactive song trainer: falling-note timeline + virtual kalimba (touch) or a real kalimba (microphone). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrainerScreen(vm: AppViewModel, songId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()

    // resolve the song: a library song or an ad-hoc exercise from a lesson
    val song: Song = remember(songId) {
        if (songId.startsWith("lesson:")) {
            val l = vm.content.lessonById(songId.removePrefix("lesson:"))
            val ex = l?.exercise
            val ev = ex?.let { SongParser.parseSequence(it.notes).getOrNull() }
            if (l != null && ex != null && ev != null)
                Song(songId, "تمرین: ${l.title}", "", "", "", "beginner", "beginner", ex.tempo, "4/4", ev, "original_cc0", "")
            else null
        } else vm.content.songById(songId)
    } ?: run { onBack(); return }

    var mode by remember { mutableStateOf(PlayMode.GUIDED) }
    var speedPct by remember { mutableIntStateOf(50) }
    var useMic by remember { mutableStateOf(false) }
    var section by remember { mutableIntStateOf(-1) }
    var loop by remember { mutableStateOf(false) }
    val sections = remember(song) { song.sections() }

    var runId by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(0) } // 0 idle, 1 running
    var paused by remember { mutableStateOf(false) }
    var nowSec by remember { mutableFloatStateOf(0f) }
    var t0Nano by remember { mutableLongStateOf(0L) }
    var expected by remember { mutableStateOf(emptyList<ExpectedNote>()) }
    val played = remember { mutableListOf<PlayedNote>() }
    val hit = remember { mutableStateMapOf<Int, Boolean>() }
    var detected by remember { mutableStateOf(emptySet<Int>()) }
    var demoKeys by remember { mutableStateOf(emptySet<Int>()) }
    var result by remember { mutableStateOf<ScoreResult?>(null) }
    var lastAcc by remember { mutableStateOf<Double?>(null) }
    var flashWrong by remember { mutableLongStateOf(0L) }
    var demoJob by remember { mutableStateOf<Job?>(null) }
    var micError by remember { mutableStateOf<String?>(null) }
    val analyzerRef = remember { arrayOfNulls<MicAnalyzer>(1) }

    val from = if (section < 0) 0 else sections[section].firstEvent
    val to = if (section < 0) song.events.lastIndex else sections[section].lastEvent
    val speed = speedPct / 100.0
    val spb = 60.0 / (song.tempo * speed)
    val window = (0.4 * spb).coerceIn(0.15, 0.4) + if (useMic) 0.1 else 0.0

    fun stopMic() { analyzerRef[0]?.stop(); analyzerRef[0] = null }

    fun finishRun(startedWall: Long) {
        stopMic()
        val ex = expected
        val r = PracticeScorer.evaluate(ex, played.toList(), window)
        val elapsed = ((System.currentTimeMillis() - startedWall) / 1000).toInt()
        phase = 0; paused = false; detected = emptySet()
        lastAcc = r.accuracy
        vm.saveSession(song.takeIf { !songId.startsWith("lesson:") }, song.titleFa, r, mode.key, if (useMic) "mic" else "virtual", speedPct, startedWall, elapsed, section < 0)
        if (loop && mode == PlayMode.PRACTICE) { runId++ } else result = r
    }

    // the run loop: count-in, then drive the clock until the last note has passed
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        val startedWall = System.currentTimeMillis()
        expected = PracticeScorer.flatten(song, speed, from, to)
        played.clear(); hit.clear(); result = null; micError = null
        paused = false
        t0Nano = System.nanoTime() + (4 * spb * 1e9).toLong()
        nowSec = (-4 * spb).toFloat()
        phase = 1
        if (useMic) {
            val a = MicAnalyzer(s.noiseGate.toDouble(), s.minConfidence.toDouble(), s.a4.toDouble())
            analyzerRef[0] = a
            a.start(onFrame = { mf ->
                val on = mf.onset
                if (on != null && phase == 1 && !paused) {
                    val key = KalimbaLayout.keyForMidi(on.midi)
                    if (key != null) {
                        val t = (a.startNano + (on.timeSec * 1e9).toLong() - t0Nano) / 1e9 - s.micLatencyMs / 1000.0
                        played.add(PlayedNote(t, key))
                        detected = setOf(key)
                        val idx = expected.indices.firstOrNull { !hit.containsKey(it) && expected[it].key == key && kotlin.math.abs(expected[it].timeSec - t) <= window }
                        if (idx != null) hit[idx] = true else flashWrong = System.nanoTime()
                    } else flashWrong = System.nanoTime()
                }
            }, onError = { micError = it })
        }
        launch { for (i in 0 until 4) { val at = t0Nano - ((4 - i) * spb * 1e9).toLong(); val w = (at - System.nanoTime()) / 1_000_000; if (w > 0) delay(w); vm.audio.click(i == 0, s.clickType) } }
        val endT = (expected.maxOfOrNull { it.timeSec } ?: 0.0) + song.events[to].durBeats * spb + 0.8
        while (isActive) {
            withFrameNanos { }
            if (paused) { t0Nano = System.nanoTime() - (nowSec * 1e9).toLong(); continue }
            nowSec = ((System.nanoTime() - t0Nano) / 1e9).toFloat()
            if (nowSec > endT) break
        }
        finishRun(startedWall)
    }
    DisposableEffect(Unit) { onDispose { stopMic(); demoJob?.cancel() } }

    val micGate = rememberMicPermission { useMic = true }

    fun onKeyPressed(key: Int) {
        vm.audio.playKey(key)
        if (!useMic && phase == 1 && !paused) {
            val t = (System.nanoTime() - t0Nano) / 1e9
            played.add(PlayedNote(t, key))
            val idx = expected.indices.firstOrNull { !hit.containsKey(it) && expected[it].key == key && kotlin.math.abs(expected[it].timeSec - t) <= window }
            if (idx != null) hit[idx] = true else flashWrong = System.nanoTime()
        }
    }

    val now = nowSec
    val guide: Set<Int> = if (phase == 1 && mode == PlayMode.GUIDED)
        expected.indices.filter { !hit.containsKey(it) && expected[it].timeSec in (now - 0.12)..(now + 0.7) }.map { expected[it].key }.toSet()
    else emptySet()

    Column(Modifier.fillMaxSize()) {
        TopBar(song.titleFa, { stopMic(); demoJob?.cancel(); runId = 0; phase = 0; onBack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            if (phase == 0) {
                WoodCard(Modifier.fillMaxWidth()) {
                    Text("حالت", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PlayMode.values().toList()) { m -> FilterChip(mode == m, { mode = m }, { Text(m.fa) }) }
                    }
                    Text(when (mode) {
                        PlayMode.GUIDED -> "کلید بعدی روشن می‌شود."
                        PlayMode.PRACTICE -> "نت‌ها می‌افتند؛ بخش‌ها را تکرار و حلقه کن."
                        PlayMode.PERFORMANCE -> "راهنما پنهان است و کل اجرا ارزیابی می‌شود."
                    }, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Text("سرعت", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SPEEDS) { sp -> FilterChip(speedPct == sp, { speedPct = sp }, { Text(faDigits(sp) + "٪") }) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("ساز", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!useMic, { useMic = false }, { Text("کالیمبای مجازی") })
                        FilterChip(useMic, { micGate() }, { Text("کالیمبای واقعی (میکروفون)") }, leadingIcon = { Icon(Icons.Filled.Mic, null, Modifier.size(16.dp)) })
                    }
                    if (useMic) Text("تشخیص تک‌نت: هر بار یک نت را بنواز و صبر کن صدایش محو شود. تشخیص هم‌زمان چند نت (پلی‌فونیک) پشتیبانی نمی‌شود.", style = MaterialTheme.typography.bodySmall)
                    if (sections.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        Text("بخش", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(section < 0, { section = -1 }, { Text("همه") }) }
                            items(sections) { sec -> FilterChip(section == sec.index, { section = sec.index }, { Text(faDigits(sec.index + 1)) }) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("حلقه (در حالت تمرین)", Modifier.weight(1f)); Switch(loop, { loop = it })
                    }
                }
            }
            micError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            // transport
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (phase == 0) {
                    Button(onClick = { demoJob?.cancel(); runId++ }, Modifier.weight(1f).height(48.dp)) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("شروع") }
                    FilledTonalButton(onClick = {
                        demoJob?.cancel()
                        demoJob = scope.launch { playEvents(vm.audio, song.events.subList(from, to + 1).map { it.copy(startBeat = it.startBeat - song.events[from].startBeat) }, song.tempo * speed) { demoKeys = it } }
                    }, Modifier.weight(1f).height(48.dp)) { Icon(Icons.Filled.VolumeUp, null); Spacer(Modifier.width(6.dp)); Text("شنیدن") }
                } else {
                    FilledTonalButton(onClick = { paused = !paused }, Modifier.weight(1f).height(48.dp)) {
                        Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, null); Spacer(Modifier.width(6.dp)); Text(if (paused) "ادامه" else "توقف")
                    }
                    Button(onClick = { loop = false; stopMic(); val wall = System.currentTimeMillis(); runId = 0; phase = 0; paused = false; detected = emptySet()
                        val r = PracticeScorer.evaluate(expected.filter { it.timeSec <= nowSec - window }, played.filter { it.timeSec <= nowSec }, window)
                        if (r.total > 0) { result = r; vm.saveSession(song.takeIf { !songId.startsWith("lesson:") }, song.titleFa, r, mode.key, if (useMic) "mic" else "virtual", speedPct, wall - (nowSec.coerceAtLeast(0f) * 1000).toLong(), nowSec.toInt().coerceAtLeast(0), false) }
                    }, Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(6.dp)); Text("پایان")
                    }
                }
            }
            lastAcc?.let { if (phase == 0) Text("دقت آخرین اجرا: ${pct(it)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }

            // timeline + instrument
            val accent = MaterialTheme.colorScheme.tertiary
            Box(Modifier.fillMaxWidth().height(190.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height; val hitY = h - 6.dp.toPx()
                    drawRoundRect(Color(0x22000000), size = size, cornerRadius = CornerRadius(16f))
                    val flash = (System.nanoTime() - flashWrong) < 250_000_000L
                    for (slot in 0 until 17) { val x = KalimbaGeom.slotCenterX(w, slot); drawLine(Color(0x22888888), Offset(x, 0f), Offset(x, h), 1f) }
                    drawLine(if (flash) Color(0xFFD9534F) else Color(0xFFFF9F43), Offset(0f, hitY), Offset(w, hitY), 4f)
                    if (phase == 1 && mode != PlayMode.PERFORMANCE) {
                        val look = 2.6f
                        val tw = KalimbaGeom.slotWidth(w) * 0.7f
                        for (i in expected.indices) {
                            val e = expected[i]
                            val dt = e.timeSec.toFloat() - now
                            if (dt > look || dt < -0.6f) continue
                            val y = hitY - dt / look * hitY
                            val ev = song.events[e.eventIndex]
                            val lenPx = (ev.durBeats * spb / look * hitY).toFloat().coerceIn(14.dp.toPx(), h * 0.5f)
                            val cx = KalimbaGeom.slotCenterX(w, KalimbaLayout.slotOfKey(e.key))
                            val col = when {
                                hit[i] == true -> Color(0xFF3DDC84)
                                dt < -window.toFloat() -> Color(0x88D9534F)
                                else -> accent
                            }
                            drawRoundRect(col, Offset(cx - tw / 2, y - lenPx), Size(tw, lenPx), CornerRadius(tw * 0.4f))
                        }
                    }
                }
                val label = when {
                    phase == 1 && nowSec < 0f -> faDigits(ceil((-nowSec / spb)).toInt().coerceAtLeast(1))
                    phase == 1 && mode == PlayMode.PERFORMANCE -> "حالت اجرا: راهنما پنهان است"
                    phase == 0 -> "«شروع» را بزن"
                    paused -> "متوقف شد"
                    else -> ""
                }
                if (label.isNotEmpty()) Text(label, Modifier.align(Alignment.Center), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().aspectRatio(0.9f)) {
                KalimbaInstrument(
                    activeKeys = if (useMic) detected else demoKeys, guideKeys = guide,
                    showNames = s.showNoteNames, showNumbers = s.showNumbers, interactive = !useMic || phase == 0,
                    onPress = { onKeyPressed(it) },
                )
            }
            if (phase == 1 && !paused) {
                val total = expected.size.coerceAtLeast(1)
                LinearProgressIndicator(progress = { (hit.size.toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                Text("درست تا اینجا: ${faDigits(hit.size)} از ${faDigits(expected.size)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text("گزارش تمرین") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("دقت: ${pct(r.accuracy)}", fontWeight = FontWeight.Bold)
                    Text("کل نت‌ها: ${faDigits(r.total)}  •  درست: ${faDigits(r.correct)}  •  اشتباه: ${faDigits(r.wrong)}  •  از‌دست‌رفته: ${faDigits(r.missed)}  •  اضافه: ${faDigits(r.extra)}")
                    if (r.correct > 0) Text("امتیاز زمان‌بندی: ${pct(r.timingScore)}  •  میانگین انحراف: ${faDigits(r.meanOffsetMs.toInt())} میلی‌ثانیه")
                    Spacer(Modifier.height(6.dp))
                    PracticeScorer.suggestions(r).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { TextButton(onClick = { result = null; runId++ }) { Text("دوباره") } },
            dismissButton = { TextButton(onClick = { result = null }) { Text("بستن") } },
        )
    }
}
