package com.kalimba.ava.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kalimba.ava.audio.AudioEngine
import com.kalimba.ava.core.NoteEvent
import kotlinx.coroutines.delay
import java.io.File

fun faDigits(n: Int): String = n.toString().map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")
fun faDigits(s: String): String = s.map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")
fun pct(v: Double) = faDigits(v.toInt()) + "٪"
fun mmss(ms: Long): String { val s = ms / 1000; return faDigits("%02d:%02d".format(s / 60, s % 60)) }

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp))
}

@Composable
fun WoodCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val shape = RoundedCornerShape(18.dp)
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors) { Column(Modifier.padding(14.dp), content = content) }
    else Card(modifier = modifier, shape = shape, colors = colors) { Column(Modifier.padding(14.dp), content = content) }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Requests the microphone permission only when needed, after explaining why. */
@Composable
fun rememberMicPermission(onGranted: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    var showWhy by remember { mutableStateOf(false) }
    val granted by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) granted() }
    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            title = { Text("دسترسی به میکروفون") },
            text = { Text("برای شنیدن صدای کالیمبای واقعی و تشخیص نت‌ها، برنامه به میکروفون نیاز دارد. صدا فقط روی همین دستگاه پردازش می‌شود و هیچ‌جا ارسال نمی‌شود. ضبط‌ها هم فقط به‌صورت محلی ذخیره می‌شوند.") },
            confirmButton = { TextButton(onClick = { showWhy = false; launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("ادامه") } },
            dismissButton = { TextButton(onClick = { showWhy = false }) { Text("انصراف") } },
        )
    }
    return {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) granted() else showWhy = true
    }
}

fun hasMicPermission(ctx: Context) = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Plays note events in real time, reporting the sounding keys so the instrument can highlight them. */
suspend fun playEvents(audio: AudioEngine, events: List<NoteEvent>, tempo: Double, onKeys: (Set<Int>) -> Unit) {
    val msPerBeat = 60000.0 / tempo
    val t0 = System.nanoTime()
    try {
        for (e in events) {
            val wait = ((t0 + (e.startBeat * msPerBeat * 1_000_000).toLong()) - System.nanoTime()) / 1_000_000
            if (wait > 0) delay(wait)
            e.keys.forEach { audio.playKey(it) }
            onKeys(e.keys.toSet())
        }
        delay(350)
    } finally { onKeys(emptySet()) }
}

fun shareFile(ctx: Context, path: String) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", File(path))
    val i = Intent(Intent.ACTION_SEND).apply { type = "audio/wav"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری ضبط").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
