package com.kalimba.ava.ui

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalimba.ava.audio.MicAnalyzer
import com.kalimba.ava.audio.Metronome
import com.kalimba.ava.audio.WavRecorder
import com.kalimba.ava.core.KalimbaLayout
import com.kalimba.ava.core.NoteMath
import com.kalimba.ava.core.NoteReading
import com.kalimba.ava.data.RecordingE
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs

@Composable
fun ToolsScreen(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("تیونر", "مترونوم", "ضبط‌ها", "زمان‌سنج")
    Column(Modifier.fillMaxSize()) {
        Text("تمرین و ابزارها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> TunerTab(vm)
            1 -> MetronomeTab(vm)
            2 -> RecorderTab(vm)
            else -> TimerTab(vm)
        }
    }
}

// ------------------------------------------------------------------ tuner / note recognition
@Composable
private fun TunerTab(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    var listening by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<NoteReading?>(null) }
    var confidence by remember { mutableDoubleStateOf(0.0) }
    var level by remember { mutableDoubleStateOf(0.0) }
    var error by remember { mutableStateOf<String?>(null) }
    val askMic = rememberMicPermission { listening = true }

    DisposableEffect(listening, s.noiseGate, s.minConfidence, s.a4) {
        var analyzer: MicAnalyzer? = null
        if (listening) {
            var lastValid = 0L
            var lastUi = 0L
            analyzer = MicAnalyzer(s.noiseGate.toDouble(), s.minConfidence.toDouble(), s.a4.toDouble())
            analyzer.start(onFrame = { mf ->
                val now = System.nanoTime()
                val p = mf.frame.pitch
                val ok = p != null && p.confidence >= s.minConfidence && mf.frame.rms >= s.noiseGate
                if (ok && p != null) {
                    lastValid = now
                    if (now - lastUi > 40_000_000L) {
                        reading = NoteMath.freqToNote(p.frequency, s.a4.toDouble()); confidence = p.confidence; lastUi = now
                    }
                } else if (now - lastValid > 700_000_000L) reading = null
                if (now - lastUi > 40_000_000L) { level = mf.frame.rms; lastUi = now }
            }, onError = { error = it; listening = false })
        } else reading = null
        onDispose { analyzer?.stop() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("تیونر کروماتیک و تشخیص نت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(true, {}, { Text("حالت الف: تک‌نت") })
            FilterChip(false, {}, { Text("حالت ب: چندنت") }, enabled = false)
        }
        Text("تشخیص هم‌زمان چند نت (پلی‌فونیک) در این نسخه پیاده‌سازی نشده است؛ فقط تشخیص قابل‌اعتماد تک‌نت فعال است. هر نت را جدا بنواز.", style = MaterialTheme.typography.bodySmall)

        Button(onClick = { if (listening) listening = false else askMic() }, Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
            Icon(if (listening) Icons.Filled.Stop else Icons.Filled.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (listening) "توقف شنیدن" else "شروع شنیدن")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val r = reading
        WoodCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (r != null) "${r.name}${r.octave}" else "—", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (r != null) {
                    val c = r.cents
                    Text("فرکانس: ${faDigits("%.2f".format(r.frequency))} هرتز", style = MaterialTheme.typography.bodyLarge)
                    Text("انحراف: ${if (c >= 0) "+" else "−"}${faDigits(abs(c).toInt())} سنت", style = MaterialTheme.typography.bodyLarge)
                    Text("اطمینان: ${pct(confidence * 100)}", style = MaterialTheme.typography.bodyLarge)
                    Text(when {
                        abs(c) < 5 -> "✓ کوک است"
                        c > 0 -> "کمی بالاتر از نت است؛ کوک را پایین‌تر بیاور"
                        else -> "کمی پایین‌تر از نت است؛ کوک را بالاتر ببر"
                    }, style = MaterialTheme.typography.bodyMedium)
                } else Text(if (listening) "در انتظار صدا…" else "برای شروع، دکمه‌ی شنیدن را بزن", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                CentsGauge(r?.cents)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { (level * 8).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                Text("سطح ورودی میکروفون", style = MaterialTheme.typography.labelSmall)
            }
        }
        val key = r?.let { KalimbaLayout.keyForMidi(it.midi) }
        Box(Modifier.fillMaxWidth().aspectRatio(1.1f)) {
            KalimbaInstrument(activeKeys = if (key != null) setOf(key) else emptySet(), showNames = true, showNumbers = true, interactive = false)
        }
        if (r != null && key == null) Text("این نت روی کالیمبای ۱۷ کلیدِ دو ماژور وجود ندارد.", style = MaterialTheme.typography.bodySmall)

        SectionHeader("تنظیمات تشخیص")
        WoodCard(Modifier.fillMaxWidth()) {
            LabeledSlider("آستانه‌ی نویز (بالاتر = حساسیت کمتر)", s.noiseGate, 0.003f..0.06f, faDigits("%.3f".format(s.noiseGate))) { vm.setGate(it) }
            LabeledSlider("حداقل اطمینان تشخیص", s.minConfidence, 0.5f..0.98f, pct(s.minConfidence * 100.0)) { vm.setConf(it) }
            LabeledSlider("فرکانس مرجع A4 (کالیبراسیون)", s.a4, 430f..450f, faDigits("%.0f".format(s.a4)) + " هرتز") { vm.setA4(Math.round(it).toFloat()) }
            LabeledSlider("جبران تأخیر میکروفون (در تمرین)", s.micLatencyMs.toFloat(), 0f..200f, faDigits(s.micLatencyMs) + " میلی‌ثانیه") { vm.setLatency(it.toInt()) }
        }
    }
}

@Composable
private fun CentsGauge(cents: Double?) {
    val track = MaterialTheme.colorScheme.outline
    val good = Wood.Good
    val needleColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(46.dp)) {
        val w = size.width; val cy = size.height / 2
        drawLine(track, Offset(0f, cy), Offset(w, cy), 3f)
        for (c in -50..50 step 10) {
            val x = w * (c + 50) / 100f
            drawLine(if (c == 0) good else track, Offset(x, cy - (if (c == 0) 18f else 9f)), Offset(x, cy + (if (c == 0) 18f else 9f)), if (c == 0) 4f else 2f)
        }
        if (cents != null) {
            val x = w * (cents.coerceIn(-50.0, 50.0).toFloat() + 50f) / 100f
            drawLine(if (abs(cents) < 5) good else needleColor, Offset(x, 2f), Offset(x, size.height - 2f), 6f)
        }
    }
}

// ------------------------------------------------------------------ metronome
@Composable
private fun MetronomeTab(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    var bpm by rememberSaveable { mutableIntStateOf(90) }
    var sig by rememberSaveable { mutableIntStateOf(2) }
    var running by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(-1) }
    val sigs = listOf(2 to 4, 3 to 4, 4 to 4, 5 to 4, 6 to 8)
    val met = remember { Metronome(vm.audio) }
    val (num, den) = sigs[sig]
    met.bpm = bpm; met.beatsPerBar = num; met.denominator = den; met.clickType = s.clickType
    DisposableEffect(Unit) { onDispose { met.stop() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(faDigits(bpm), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("ضرب در دقیقه", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { bpm = (bpm - 1).coerceAtLeast(30) }) { Icon(Icons.Filled.Remove, "کمتر") }
            Slider(value = bpm.toFloat(), onValueChange = { bpm = it.toInt() }, valueRange = 30f..240f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            FilledTonalIconButton(onClick = { bpm = (bpm + 1).coerceAtMost(240) }) { Icon(Icons.Filled.Add, "بیشتر") }
        }
        Text("میزان", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sigs.forEachIndexed { i, (n, d) -> FilterChip(sig == i, { sig = i; beat = -1 }, { Text(faDigits("$n/$d")) }) }
        }
        Text("صدای کلیک", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("بیپ نرم", "چوبی", "تیک").forEachIndexed { i, t -> FilterChip(s.clickType == i, { vm.setClick(i) }, { Text(t) }) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            for (i in 0 until num) {
                Box(Modifier.padding(4.dp).size(if (i == 0) 34.dp else 26.dp).clip(CircleShape)
                    .background(if (beat == i) (if (i == 0) Wood.Bad else MaterialTheme.colorScheme.tertiary) else MaterialTheme.colorScheme.surfaceVariant))
            }
        }
        Button(onClick = {
            if (running) { met.stop(); running = false; beat = -1 } else { running = true; met.start { beat = it } }
        }, Modifier.fillMaxWidth().height(54.dp)) {
            Icon(if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (running) "توقف" else "شروع")
        }
        Text("ضرب اول هر میزان با صدای تیزتر (تأکید) نواخته می‌شود.", style = MaterialTheme.typography.bodySmall)
    }
}

// ------------------------------------------------------------------ recorder + recordings library
@Composable
private fun RecorderTab(vm: AppViewModel) {
    val ctx = LocalContext.current
    val recs by vm.recordings.collectAsState()
    var recorder by remember { mutableStateOf<WavRecorder?>(null) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var level by remember { mutableFloatStateOf(0f) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var playingId by remember { mutableLongStateOf(-1L) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var renaming by remember { mutableStateOf<RecordingE?>(null) }
    var deleting by remember { mutableStateOf<RecordingE?>(null) }
    var startedWall by remember { mutableLongStateOf(0L) }

    fun startRec() {
        val f = File(vm.recordingsDir(), "mic_${System.currentTimeMillis()}.wav")
        val r = WavRecorder(f)
        if (r.start { error = it }) { recorder = r; currentFile = f; startedWall = System.currentTimeMillis(); error = null }
    }
    val askMic = rememberMicPermission { startRec() }
    fun stopRec() {
        val r = recorder ?: return
        val dur = r.stop(); recorder = null
        val f = currentFile
        if (f != null && dur > 500) {
            vm.addRecording("ضبط " + faDigits(java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US).format(java.util.Date())), f, "audio", dur)
            vm.logPractice("record", "ضبط صدا", startedWall, (dur / 1000).toInt())
        } else f?.delete()
    }
    fun stopPlay() { runCatching { player?.stop() }; player?.release(); player = null; playingId = -1 }
    LaunchedEffect(recorder) { while (recorder != null) { elapsed = recorder?.elapsedMs ?: 0; level = recorder?.level ?: 0f; delay(100) } }
    DisposableEffect(Unit) { onDispose { recorder?.stop(); stopPlay() } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            WoodCard(Modifier.fillMaxWidth()) {
                Text("ضبط صدای کالیمبای واقعی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("ضبط‌ها فقط روی همین دستگاه ذخیره می‌شوند و بدون اجازه‌ی تو ارسال نمی‌شوند.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (recorder != null) { Text(mmss(elapsed), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error); LinearProgressIndicator(progress = { level }, Modifier.fillMaxWidth()) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (recorder != null) stopRec() else { stopPlay(); askMic() } }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (recorder != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                    Icon(if (recorder != null) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, null); Spacer(Modifier.width(8.dp)); Text(if (recorder != null) "پایان ضبط" else "شروع ضبط")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item { SectionHeader("کتابخانه‌ی ضبط‌ها (${faDigits(recs.size)})") }
        if (recs.isEmpty()) item { Text("هنوز ضبطی نداری. اجراهای «کالیمبای من» هم این‌جا ذخیره می‌شوند.", style = MaterialTheme.typography.bodyMedium) }
        items(recs, key = { it.id }) { r ->
            WoodCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (r.kind == "audio") Icons.Filled.Mic else Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text(r.name, fontWeight = FontWeight.Bold); Text(mmss(r.durationMs) + if (r.kind == "audio") "  •  میکروفون" else "  •  کالیمبای مجازی", style = MaterialTheme.typography.labelSmall) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = {
                        if (playingId == r.id) stopPlay() else {
                            stopPlay()
                            val mp = MediaPlayer()
                            runCatching { mp.setDataSource(r.path); mp.prepare(); mp.setOnCompletionListener { stopPlay() }; mp.start(); player = mp; playingId = r.id }
                                .onFailure { mp.release(); error = "پخش ممکن نشد" }
                        }
                    }) { Icon(if (playingId == r.id) Icons.Filled.Stop else Icons.Filled.PlayArrow, "پخش") }
                    IconButton(onClick = { renaming = r }) { Icon(Icons.Filled.Edit, "تغییر نام") }
                    IconButton(onClick = { runCatching { shareFile(ctx, r.path) } }) { Icon(Icons.Filled.Share, "اشتراک") }
                    IconButton(onClick = { deleting = r }) { Icon(Icons.Filled.Delete, "حذف") }
                }
            }
        }
    }
    renaming?.let { r ->
        var text by remember(r.id) { mutableStateOf(r.name) }
        AlertDialog(onDismissRequest = { renaming = null }, title = { Text("تغییر نام") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (text.isNotBlank()) vm.renameRecording(r.id, text.trim()); renaming = null }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("انصراف") } })
    }
    deleting?.let { r ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف ضبط") }, text = { Text("«${r.name}» برای همیشه پاک شود؟") },
            confirmButton = { TextButton(onClick = { if (playingId == r.id) stopPlay(); vm.deleteRecording(r); deleting = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } })
    }
}

// ------------------------------------------------------------------ practice timer
@Composable
private fun TimerTab(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val logs by vm.logs.collectAsState()
    var running by remember { mutableStateOf(false) }
    var startedWall by remember { mutableLongStateOf(0L) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) { while (running) { elapsedSec = ((System.currentTimeMillis() - startedWall) / 1000).toInt(); delay(500) } }
    DisposableEffect(Unit) { onDispose { if (running) vm.logPractice("timer", "زمان‌سنج تمرین", startedWall, ((System.currentTimeMillis() - startedWall) / 1000).toInt()) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(mmss(elapsedSec * 1000L), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Button(onClick = {
            if (running) { running = false; vm.logPractice("timer", "زمان‌سنج تمرین", startedWall, elapsedSec); elapsedSec = 0 }
            else { startedWall = System.currentTimeMillis(); elapsedSec = 0; running = true }
        }, Modifier.fillMaxWidth().height(54.dp)) {
            Icon(if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (running) "پایان و ذخیره" else "شروع تمرین")
        }
        WoodCard(Modifier.fillMaxWidth()) {
            Text("هدف روزانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                FilledTonalIconButton(onClick = { vm.setGoal((s.dailyGoalMin - 5).coerceAtLeast(5)) }) { Icon(Icons.Filled.Remove, "کمتر") }
                Text("${faDigits(s.dailyGoalMin)} دقیقه", style = MaterialTheme.typography.titleLarge)
                FilledTonalIconButton(onClick = { vm.setGoal((s.dailyGoalMin + 5).coerceAtMost(180)) }) { Icon(Icons.Filled.Add, "بیشتر") }
            }
            Text("امروز: ${faDigits(vm.minutesToday(logs))} دقیقه  •  رکورد پیاپی: ${faDigits(vm.streak(logs))} روز", style = MaterialTheme.typography.bodyMedium)
        }
        SectionHeader("آمار این هفته (دقیقه)")
        WeeklyChart(vm.weekly(logs))
        SectionHeader("تاریخچه‌ی جلسه‌ها")
        if (logs.isEmpty()) Text("هنوز جلسه‌ای ثبت نشده.")
        logs.take(10).forEach { l ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(l.label); Text(faDigits(java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(l.startedAt))) + "  •  " + mmss(l.durationSec * 1000L), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
