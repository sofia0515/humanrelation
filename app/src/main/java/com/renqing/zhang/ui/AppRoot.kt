package com.renqing.zhang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.renqing.zhang.ui.components.NoticeBanner
import com.renqing.zhang.ui.book.BookDetailScreen
import com.renqing.zhang.ui.book.BookEditScreen
import com.renqing.zhang.ui.book.BookListScreen
import com.renqing.zhang.ui.contact.ContactDetailScreen
import com.renqing.zhang.ui.contact.ContactEditScreen
import com.renqing.zhang.ui.contact.ContactListScreen
import com.renqing.zhang.ui.home.HomeScreen
import com.renqing.zhang.ui.me.CategoriesScreen
import com.renqing.zhang.ui.me.LockScreen
import com.renqing.zhang.ui.me.MeScreen
import com.renqing.zhang.ui.onboarding.OnboardingScreen
import com.renqing.zhang.ui.record.AllRecordsScreen
import com.renqing.zhang.ui.record.RecordDetailScreen
import com.renqing.zhang.ui.record.RecordEditScreen
import com.renqing.zhang.ui.search.SearchScreen
import com.renqing.zhang.ui.stats.StatsScreen
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.InkSoft

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "首页", Icons.Filled.Home),
    Tab("books", "礼簿", Icons.AutoMirrored.Filled.MenuBook),
    Tab("contacts", "亲友", Icons.Filled.People),
    Tab("stats", "统计", Icons.Filled.BarChart),
    Tab("me", "我的", Icons.Filled.Person),
)

@Composable
fun AppRoot() {
    val repo = LocalRepo.current
    val session = rememberSession()
    val settings by repo.settings.collectAsState(initial = null)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(session) {
        session.toasts.collect { snackbar.showSnackbar(it) }
    }
    CompositionLocalProvider(LocalSession provides session) {
        if (settings?.lockOn == true && !session.unlocked) {
            Box(Modifier.fillMaxSize()) {
                LockScreen(onUnlocked = { session.unlocked = true })
                NoticeHost(snackbar, Modifier.align(Alignment.TopCenter))
            }
            return@CompositionLocalProvider
        }
        val current = settings
        if (current == null) {
            Box(Modifier.fillMaxSize().background(Honey))
            return@CompositionLocalProvider
        }
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val showBar = route in tabs.map { it.route }
        CompositionLocalProvider(LocalNav provides nav) {
            Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Honey,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {},
                bottomBar = {
                    if (showBar) {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = CardWhite,
                        ) {
                            tabs.forEach { tab ->
                                val selected = route == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (tab.route == "books" && selected) session.bookTypeId = null
                                        nav.switchTab(tab.route)
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Cinnabar,
                                        selectedTextColor = Cinnabar,
                                        unselectedIconColor = InkSoft,
                                        unselectedTextColor = InkSoft,
                                        indicatorColor = Color.Transparent,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = if (current.onboardingDone) "home" else "onboarding",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding()),
                ) {
                    composable("onboarding") {
                        OnboardingScreen {
                            nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                        }
                    }
                    composable("home") { HomeScreen() }
                    composable("books") { BookListScreen() }
                    composable("contacts") { ContactListScreen() }
                    composable("stats") { StatsScreen() }
                    composable("me") { MeScreen() }
                    composable("search") { SearchScreen() }
                    composable("records") { AllRecordsScreen() }
                    composable("categories") { CategoriesScreen() }
                    composable(
                        "book/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        BookDetailScreen(entry.arguments?.getLong("id") ?: 0L)
                    }
                    composable(
                        "bookEdit?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                    ) { entry ->
                        BookEditScreen(entry.arguments?.getLong("id") ?: -1L)
                    }
                    composable(
                        "contact/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        ContactDetailScreen(entry.arguments?.getLong("id") ?: 0L)
                    }
                    composable(
                        "contactEdit?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                    ) { entry ->
                        ContactEditScreen(entry.arguments?.getLong("id") ?: -1L)
                    }
                    composable(
                        "record/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        RecordDetailScreen(entry.arguments?.getLong("id") ?: 0L)
                    }
                    composable(
                        "recordEdit?contactId={contactId}&bookId={bookId}&direction={direction}&recordId={recordId}",
                        arguments = listOf(
                            navArgument("contactId") { type = NavType.LongType; defaultValue = -1L },
                            navArgument("bookId") { type = NavType.LongType; defaultValue = -1L },
                            navArgument("direction") { type = NavType.StringType; defaultValue = "" },
                            navArgument("recordId") { type = NavType.LongType; defaultValue = -1L },
                        ),
                    ) { entry ->
                        RecordEditScreen(
                            contactId = entry.arguments?.getLong("contactId") ?: -1L,
                            bookId = entry.arguments?.getLong("bookId") ?: -1L,
                            direction = entry.arguments?.getString("direction").orEmpty(),
                            recordId = entry.arguments?.getLong("recordId") ?: -1L,
                        )
                    }
                }
            }
            NoticeHost(snackbar, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun NoticeHost(hostState: SnackbarHostState, modifier: Modifier) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.statusBarsPadding().padding(top = 8.dp, start = 24.dp, end = 24.dp),
    ) { data ->
        NoticeBanner(data.visuals.message)
    }
}
