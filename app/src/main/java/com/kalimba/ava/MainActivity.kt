package com.kalimba.ava

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kalimba.ava.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val audio get() = (application as KalimbaApp).container.audio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KalimbaRoot(onExit = { finishAffinity() }) }
    }

    override fun onStart() { super.onStart(); audio.start() }
    override fun onStop() { super.onStop(); audio.stop() }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun KalimbaRoot(onExit: () -> Unit) {
    val vm: AppViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    KalimbaTheme(settings.theme) {
        // The whole app is Persian-first, so the layout direction is right-to-left.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var splash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { delay(1800); splash = false }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (splash) SplashScreen() else AppNav(vm, onExit)
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Column(Modifier.fillMaxSize().background(Color(0xFF211A15)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.kalimba_photo), "کالیمبا", Modifier.height(260.dp))
        Spacer(Modifier.height(20.dp))
        Text("کالیمبا آوا", style = MaterialTheme.typography.displaySmall, color = Color(0xFFD5AA63), fontWeight = FontWeight.Bold)
        Text("آکادمی کالیمبا", color = Color(0xFFF3E3C8))
        Spacer(Modifier.height(24.dp))
        Text("سازنده: محمد طاها", color = Color(0xFFD9C7AF), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AppNav(vm: AppViewModel, onExit: () -> Unit) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "خانه", Icons.Filled.Home), Tab("lessons", "آموزش", Icons.Filled.School),
        Tab("kalimba", "کالیمبای من", Icons.Filled.Piano), Tab("songs", "آهنگ‌ها", Icons.Filled.LibraryMusic),
        Tab("tools", "تمرین و ابزارها", Icons.Filled.Tune), Tab("profile", "پروفایل", Icons.Filled.Person),
    )
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route == null || tabs.any { it.route == route }

    fun goTab(r: String) = nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                tabs.forEach { t ->
                    NavigationBarItem(selected = route == t.route, onClick = { goTab(t.route) }, icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) }, alwaysShowLabel = false)
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad).then(if (showBar) Modifier.statusBarsPadding() else Modifier)) {
            composable("home") { HomeScreen(vm, { nav.navigate("lesson/$it") }, { nav.navigate("song/$it") }, ::goTab, onExit) }
            composable("lessons") { LessonsScreen(vm) { nav.navigate("lesson/$it") } }
            composable("kalimba") { MyKalimbaScreen(vm) }
            composable("songs") { SongsScreen(vm) { nav.navigate("song/$it") } }
            composable("tools") { ToolsScreen(vm) }
            composable("profile") { ProfileScreen(vm) }
            composable("lesson/{id}") { e ->
                LessonDetailScreen(vm, e.arguments?.getString("id") ?: "", { nav.popBackStack() }) { nav.navigate("trainer/$it") }
            }
            composable("song/{id}") { e ->
                SongDetailScreen(vm, e.arguments?.getString("id") ?: "", { nav.popBackStack() }) { nav.navigate("trainer/$it") }
            }
            composable("trainer/{id}") { e -> TrainerScreen(vm, e.arguments?.getString("id") ?: "") { nav.popBackStack() } }
        }
    }
}
