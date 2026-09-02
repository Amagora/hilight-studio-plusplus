package com.hilight.studio

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        getSystemService(android.app.NotificationManager::class.java)?.cancel(1)
        val store = Store.get(this)
        setContent {
            val themeMode by store.themeMode.collectAsStateWithLifecycle()
            val themePalette by store.themePalette.collectAsStateWithLifecycle()
            val amoledDark by store.amoledDark.collectAsStateWithLifecycle()
            HiLightTheme(
                themeMode = themeMode,
                themePalette = themePalette,
                amoledDark = amoledDark,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    App(store)
                }
            }
        }
    }

    /** Without this the app's own notifications are dropped, including the Setup self test. */
    private fun requestNotificationPermissionIfNeeded() {
        val perm = android.Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(perm), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        Store.get(this).apply {
            syncForegroundWatcher()
            refreshStatus()
        }
    }

    /** A test the user started by hand must not outlive the screen they started it from. */
    override fun onStop() {
        super.onStop()
        Store.get(this).stopPreview()
    }
}

private enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    TEST(R.string.tab_test, Icons.Rounded.Tune),
    APPS(R.string.tab_apps, Icons.Rounded.Apps),
    SETUP(R.string.tab_setup, Icons.Rounded.DisplaySettings),
    INFO(R.string.tab_info, Icons.Outlined.Info),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(store: Store) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[tabIndex.coerceIn(0, Tab.entries.lastIndex)]
    val status by store.status.collectAsStateWithLifecycle()
    val active by store.activeTransport.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                store.refreshStatus()
                delay(1500)
            }
        }
    }

    val aiDisclosureAcknowledged by store.aiDisclosureAcknowledged.collectAsStateWithLifecycle()
    if (!aiDisclosureAcknowledged) {
        AiDisclosureDialog(
            onDismiss = { store.acknowledgeAiDisclosure() },
            confirmButtonText = stringResource(R.string.setup_ai_disclosure_dialog_understand),
        )
    }

    val config = LocalConfiguration.current
    val isWide = config.screenWidthDp >= 600
    val railPosition by store.railPosition.collectAsStateWithLifecycle()

    val navigationRailContent: @Composable () -> Unit = {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Spacer(Modifier.height(8.dp))
            Tab.entries.forEach { t ->
                NavigationRailItem(
                    selected = tab == t,
                    onClick = {
                        if (tab != t) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        tabIndex = t.ordinal
                    },
                    icon = { Icon(t.icon, contentDescription = stringResource(t.labelRes)) },
                    label = { Text(stringResource(t.labelRes)) },
                    alwaysShowLabel = true,
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.hilight_logo),
                            contentDescription = "HiLight Studio logo",
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("HiLight-Studio++", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Version: ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    val rendererConnected = store.isRendererConnectedForUi(status)
                    LivePill(
                        text = if (rendererConnected) {
                            stringResource(
                                R.string.main_connected_pill,
                                status.ledCount,
                                stringResource(active.labelRes),
                            )
                        } else {
                            stringResource(R.string.main_not_connected)
                        },
                        ok = rendererConnected,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (!isWide) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = {
                                if (tab != t) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                tabIndex = t.ordinal
                            },
                            icon = { Icon(t.icon, contentDescription = stringResource(t.labelRes)) },
                            label = { Text(stringResource(t.labelRes)) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { pad ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(pad),
        ) {
            if (isWide && railPosition == FoldableRailPosition.LEFT) {
                navigationRailContent()
            }

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(320)) { w -> dir * w / 8 } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(tween(320)) { w -> -dir * w / 8 } + fadeOut(tween(160))
                        )
                },
                label = "tab",
                modifier = Modifier.weight(1f),
            ) { current ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        Modifier
                            .widthIn(max = if (isWide) 720.dp else 500.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (current) {
                            Tab.TEST -> TestScreen(store)
                            Tab.APPS -> AppRulesScreen(store)
                            Tab.SETUP -> SetupScreen(store)
                            Tab.INFO -> InfoScreen(store)
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }

            if (isWide && railPosition == FoldableRailPosition.RIGHT) {
                navigationRailContent()
            }
        }
    }
}
