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
import com.kalimba.ava.core.Progress

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val lp by vm.lessonProgress.collectAsState()
    val sp by vm.songProgress.collectAsState()
    val logs by vm.logs.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val recs by vm.recordings.collectAsState()
    var editName by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val lessonsDone = lp.values.count { it.completed }
    val songsDone = sp.values.count { it.completed }
    val best = sessions.maxOfOrNull { it.accuracy } ?: 0.0
    val streak = vm.streak(logs)
    val totalMin = logs.sumOf { it.durationSec } / 60
    val level1 = vm.content.lessons.filter { it.level == 1 }.let { l -> l.isNotEmpty() && l.all { lp[it.id]?.completed == true } }
    val badges = Progress.badges(lessonsDone, songsDone, best, streak, totalMin, s.xp, level1)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("سطح: ${Progress.levelName(s.xp)}", color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { editName = true }) { Icon(Icons.Filled.Edit, "تغییر نام") }
        }
        WoodCard(Modifier.fillMaxWidth()) {
            Text("${faDigits(s.xp)} امتیاز تجربه", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { Progress.levelProgress(s.xp) }, Modifier.fillMaxWidth().padding(vertical = 8.dp).height(10.dp))
            val toNext = Progress.xpToNext(s.xp)
            Text(if (toNext > 0) "${faDigits(toNext)} امتیاز تا سطح بعد" else "به بالاترین سطح رسیده‌ای 🎉", style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("🔥 ${faDigits(streak)} روز پیاپی"); Pill("درس‌ها: ${faDigits(lessonsDone)}/${faDigits(vm.content.lessons.size)}")
            Pill("آهنگ‌های کامل: ${faDigits(songsDone)}"); Pill("بهترین دقت: ${pct(best)}"); Pill("تمرین کل: ${faDigits(totalMin)} دقیقه")
        }
        SectionHeader("نشان‌ها")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            badges.forEach { b ->
                WoodCard(Modifier.width(160.dp)) {
                    Text(if (b.earned) "🏅 ${b.title}" else "🔒 ${b.title}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                        color = if (b.earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Text(b.description, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        SectionHeader("پیشرفت هفتگی")
        WeeklyChart(vm.weekly(logs))

        SectionHeader("تازه‌ترین نتایج تمرین")
        if (sessions.isEmpty()) Text("هنوز تمرینی ثبت نشده.")
        sessions.take(5).forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.title, Modifier.weight(1f), maxLines = 1); Text("${pct(r.accuracy)} • ${faDigits(r.speedPct)}٪", style = MaterialTheme.typography.labelLarge)
            }
        }

        SectionHeader("تنظیمات")
        WoodCard(Modifier.fillMaxWidth()) {
            Text("پوسته", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("سیستم", "روشن", "تیره").forEachIndexed { i, t -> FilterChip(s.theme == i, { vm.setTheme(i) }, { Text(t) }) }
            }
            LabeledSlider("بلندی صدا", s.volume, 0.1f..1f, pct(s.volume * 100.0)) { vm.setVolume(it) }
        }

        SectionHeader("حریم خصوصی")
        Text("همه‌ی پیشرفت و ضبط‌ها فقط روی همین دستگاه ذخیره می‌شود. برنامه به حساب کاربری یا اینترنت نیاز ندارد. ${faDigits(recs.size)} ضبط محلی دارید.", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text("حذف همه‌ی داده‌ها و ضبط‌ها") }

        SectionHeader("درباره‌ی برنامه")
        WoodCard(Modifier.fillMaxWidth()) {
            Text("کالیمبا آوا – آکادمی کالیمبا", fontWeight = FontWeight.Bold)
            Text("سازنده: محمد طاها", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("نسخه ۱٫۰٫۰", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("صدای کالیمبا در این نسخه با مدل‌سازی فیزیکی تیغه‌ی فلزی ساخته می‌شود (بدون هیچ فایل صوتی دارای حق‌نشر). ملودی‌های کتابخانه یا حوزه‌ی عمومی‌اند یا آثار اصلی با مجوز CC0.", style = MaterialTheme.typography.bodySmall)
            if (vm.content.report.errors.isNotEmpty()) Text("هشدار محتوا: ${vm.content.report.errors.size} مورد نامعتبر نادیده گرفته شد.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (editName) {
        var t by remember { mutableStateOf(s.name) }
        AlertDialog(onDismissRequest = { editName = false }, title = { Text("نام شما") },
            text = { OutlinedTextField(t, { t = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (t.isNotBlank()) vm.setName(t.trim()); editName = false }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("انصراف") } })
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("حذف همه‌ی داده‌ها") },
        text = { Text("پیشرفت، امتیازها و همه‌ی ضبط‌ها پاک می‌شود و برگشت‌پذیر نیست.") },
        confirmButton = { TextButton(onClick = { vm.deleteAllData(); confirmDelete = false }) { Text("حذف") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } })
}
