package com.kalimba.ava.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalimba.ava.core.Song
import com.kalimba.ava.data.Content
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val sp by vm.songProgress.collectAsState()
    val ctx = LocalContext.current
    var filter by remember { mutableStateOf("all") }
    var favOnly by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var importMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } }.getOrNull()
            importMsg = if (text == null) "فایل خوانده نشد." else vm.importPack(text, uri.lastPathSegment?.substringAfterLast('/') ?: "pack.json")
                ?: "بسته افزوده شد. برای دیدن آهنگ‌های جدید برنامه را یک‌بار ببند و دوباره باز کن."
        }
    }
    val songs = vm.content.songs.filter { s ->
        (filter == "all" || (filter == "iranian" && s.category.startsWith("iranian")) || (filter == "intl" && !s.category.startsWith("iranian"))) &&
            (!favOnly || sp[s.id]?.favorite == true) &&
            (query.isBlank() || s.titleFa.contains(query) || s.titleEn.contains(query, ignoreCase = true))
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("کتابخانه‌ی آهنگ‌ها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Icon(Icons.Filled.Add, "افزودن بسته‌ی آهنگ") }
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("جستجو") }, leadingIcon = { Icon(Icons.Filled.Search, null) })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                item { FilterChip(filter == "all", { filter = "all" }, { Text("همه") }) }
                item { FilterChip(filter == "iranian", { filter = "iranian" }, { Text("ایرانی") }) }
                item { FilterChip(filter == "intl", { filter = "intl" }, { Text("بین‌المللی") }) }
                item { FilterChip(favOnly, { favOnly = !favOnly }, { Text("علاقه‌مندی‌ها") }, leadingIcon = { Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp)) }) }
            }
        }
        val grouped = songs.groupBy { it.category }
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Content.CATEGORY_TITLES.forEach { (cat, title) ->
                val list = grouped[cat] ?: return@forEach
                item(key = "h_$cat") { SectionHeader(title) }
                items(list, key = { it.id }) { song -> SongRow(song, sp[song.id], vm.settings.value.name, onOpen = { onOpen(song.id) }, onFav = { vm.toggleFavorite(song.id) }) }
            }
            if (songs.isEmpty()) item { Text("آهنگی پیدا نشد.", Modifier.padding(16.dp)) }
        }
    }
    importMsg?.let { m -> AlertDialog(onDismissRequest = { importMsg = null }, confirmButton = { TextButton(onClick = { importMsg = null }) { Text("باشه") } }, text = { Text(m) }) }
}

@Composable
private fun SongRow(song: Song, p: com.kalimba.ava.data.SongProgressE?, @Suppress("UNUSED_PARAMETER") user: String, onOpen: () -> Unit, onFav: () -> Unit) {
    WoodCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.titleFa, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (song.titleEn.isNotBlank()) Text(song.titleEn, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(Content.difficultyFa(song.difficulty)); Pill("${faDigits(song.tempo)} BPM"); Pill(faDigits(song.timeSignature))
                    if (p?.completed == true) Pill("✓")
                }
            }
            IconButton(onClick = onFav) {
                Icon(if (p?.favorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "علاقه‌مندی", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SongDetailScreen(vm: AppViewModel, songId: String, onBack: () -> Unit, onPractice: (String) -> Unit) {
    val song = vm.content.songById(songId) ?: run { onBack(); return }
    val p = vm.songProgress.collectAsState().value[songId]
    val s by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(emptySet<Int>()) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    Column(Modifier.fillMaxSize()) {
        TopBar(song.titleFa, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WoodCard(Modifier.fillMaxWidth()) {
                if (song.titleEn.isNotBlank()) Text(song.titleEn, style = MaterialTheme.typography.titleMedium)
                Text("آهنگساز: ${song.composer.ifBlank { "—" }}", style = MaterialTheme.typography.bodyMedium)
                if (song.artist.isNotBlank()) Text("اجراکننده: ${song.artist}", style = MaterialTheme.typography.bodyMedium)
                Text("سختی: ${Content.difficultyFa(song.difficulty)}  •  تمپو: ${faDigits(song.tempo)} BPM  •  میزان: ${faDigits(song.timeSignature)}", style = MaterialTheme.typography.bodyMedium)
                Text("مدت: ${mmss((song.durationSeconds * 1000).toLong())}  •  تعداد نت‌ها: ${faDigits(song.noteCount)}  •  بخش‌ها: ${faDigits(song.sections().size)}", style = MaterialTheme.typography.bodyMedium)
                if (p != null && p.plays > 0) Text("بهترین دقت: ${pct(p.bestAccuracy)}  •  دفعات تمرین: ${faDigits(p.plays)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = {
                    job?.cancel()
                    job = scope.launch { playEvents(vm.audio, song.events, song.tempo.toDouble()) { active = it } }
                }, Modifier.weight(1f)) { Icon(Icons.Filled.VolumeUp, null); Spacer(Modifier.width(6.dp)); Text("شنیدن نمونه") }
                Button(onClick = { onPractice(song.id) }, Modifier.weight(1f)) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("شروع تمرین") }
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1.0f)) {
                KalimbaInstrument(activeKeys = active, showNames = s.showNoteNames, showNumbers = s.showNumbers, onPress = { vm.audio.playKey(it) })
            }
            SectionHeader("تبلچر")
            val tab = song.tablature()
            val sections = song.sections()
            sections.forEach { sec ->
                Text("بخش ${faDigits(sec.index + 1)}:  " + tab.subList(sec.firstEvent, sec.lastEvent + 1).joinToString("  "),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
            }
            Text("اعداد ۱ تا ۷ = دو تا سی؛ «•» یعنی یک اُکتاو بالاتر. علامت «+» یعنی نواختن هم‌زمان.", style = MaterialTheme.typography.bodySmall)
            SectionHeader("مجوز و منبع")
            Text("${Content.licenseFa(song.license)}. ${song.sourceNote}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
