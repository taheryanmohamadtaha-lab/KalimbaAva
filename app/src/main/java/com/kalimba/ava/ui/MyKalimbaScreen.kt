package com.kalimba.ava.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalimba.ava.audio.WavIO
import com.kalimba.ava.core.KalimbaLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MyKalimbaScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    var startNano by remember { mutableLongStateOf(0L) }
    val events = remember { mutableStateListOf<Pair<Long, Int>>() }
    var lastNote by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var startedWall by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("کالیمبای من", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(lastNote, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Box(Modifier.fillMaxWidth().aspectRatio(0.82f)) {
            KalimbaInstrument(
                showNames = s.showNoteNames, showNumbers = s.showNumbers,
                onPress = { key ->
                    vm.audio.playKey(key)
                    lastNote = KalimbaLayout.noteLabel(key) + "  (" + KalimbaLayout.numberLabel(key) + ")"
                    if (recording) events.add(((System.nanoTime() - startNano) / 1_000_000) to key)
                },
            )
        }
        WoodCard(Modifier.fillMaxWidth()) {
            LabeledSlider("بلندی صدا", s.volume, 0.1f..1f, pct(s.volume * 100.0)) { vm.setVolume(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("پژواک (Reverb)", Modifier.weight(1f)); Switch(s.reverb, { vm.setReverb(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ماندگاری صدا (Sustain)", Modifier.weight(1f)); Switch(s.sustain, { vm.setSustain(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("نام نت‌ها", Modifier.weight(1f)); Switch(s.showNoteNames, { vm.setNames(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("شماره‌ی نت‌ها", Modifier.weight(1f)); Switch(s.showNumbers, { vm.setNumbers(it) })
            }
        }
        Button(
            onClick = {
                if (!recording) {
                    events.clear(); startNano = System.nanoTime(); startedWall = System.currentTimeMillis(); recording = true; message = null
                } else {
                    recording = false
                    if (events.isEmpty()) { message = "نتی نواخته نشد؛ چیزی ذخیره نشد."; return@Button }
                    saving = true
                    val snapshot = events.toList()
                    scope.launch {
                        val name = "اجرا " + faDigits(java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US).format(java.util.Date()))
                        val f = File(vm.recordingsDir(), "perf_${System.currentTimeMillis()}.wav")
                        val dur = withContext(Dispatchers.Default) { WavIO.renderPerformance(snapshot, vm.audio, f) }
                        vm.addRecording(name, f, "performance", dur)
                        vm.logPractice("free", "نواختن آزاد", startedWall, ((System.currentTimeMillis() - startedWall) / 1000).toInt())
                        saving = false
                        message = "اجرا ذخیره شد؛ از بخش «تمرین و ابزارها ← ضبط‌ها» پخش یا ارسال کن."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
            enabled = !saving,
        ) {
            Icon(if (recording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, null); Spacer(Modifier.width(8.dp))
            Text(if (recording) "پایان ضبط (${faDigits(events.size)} نت)" else "ضبط اجرا")
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        Text("چند انگشت هم‌زمان پشتیبانی می‌شود. انگشتت را روی کلیدها بکش تا نت‌ها پشت‌سرهم پخش شوند.", style = MaterialTheme.typography.bodySmall)
    }
}
