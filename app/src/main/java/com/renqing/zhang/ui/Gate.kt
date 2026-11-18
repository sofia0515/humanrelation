package com.renqing.zhang.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.renqing.zhang.data.Load
import com.renqing.zhang.data.Repository
import com.renqing.zhang.data.Settings
import com.renqing.zhang.domain.Snapshot
import com.renqing.zhang.ui.components.ErrorPane
import com.renqing.zhang.ui.components.SkeletonList
import kotlinx.coroutines.flow.MutableSharedFlow

val LocalRepo = staticCompositionLocalOf<Repository> { error("Repository 未就绪") }
val LocalNav = staticCompositionLocalOf<NavHostController> { error("导航未就绪") }

fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
val LocalSession = staticCompositionLocalOf<Session> { error("会话未就绪") }

class Session : ViewModel() {
    var bookTypeId by androidx.compose.runtime.mutableStateOf<Long?>(null)
    var scrollGroupKey by androidx.compose.runtime.mutableStateOf<String?>(null)
    var unlocked by androidx.compose.runtime.mutableStateOf(false)
    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    fun toast(message: String) {
        toasts.tryEmit(message)
    }
}

@Composable
fun rememberSession(): Session = viewModel()

@Composable
fun LedgerBody(modifier: Modifier = Modifier, content: @Composable (Snapshot, Settings) -> Unit) {
    val repo = LocalRepo.current
    var attempt by remember { mutableIntStateOf(0) }
    val load by produceState<Load<Snapshot>>(Load.Loading, attempt) {
        value = Load.Loading
        repo.ledger().collect { value = it }
    }
    val settings by repo.settings.collectAsState(initial = Settings())
    Box(modifier) {
        when (val state = load) {
            Load.Loading -> SkeletonList()
            is Load.Failed -> ErrorPane(state.message) { attempt++ }
            is Load.Ready -> content(state.value, settings)
        }
    }
}
