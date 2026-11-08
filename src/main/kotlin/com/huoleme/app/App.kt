package com.huoleme.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huoleme.app.data.AppState
import com.huoleme.app.data.DarkModePreference
import com.huoleme.app.desktop.DesktopTray
import com.huoleme.app.theme.HuolemeTheme
import com.huoleme.app.theme.scaledTypography
import com.huoleme.app.ui.BackupFiles
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.nio.file.Files
import java.security.MessageDigest

private enum class AppPage {
    DASHBOARD,
    HISTORY,
    SETTINGS
}

/** Above this width the bottom bar becomes a left-side navigation rail. */
private val WIDE_LAYOUT_THRESHOLD = 720.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuolemeApp(
    appState: AppState,
    tray: DesktopTray,
    autoStartStatus: String,
    windowWidth: Dp = 430.dp
) {
    val isWide = windowWidth >= WIDE_LAYOUT_THRESHOLD
    var currentPage by remember { mutableStateOf(AppPage.DASHBOARD) }
    var infoBar by remember { mutableStateOf<Pair<String, String>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var developerClickCount by remember { mutableStateOf(0) }
    var showDeveloperPage by remember { mutableStateOf(false) }
    var showDeveloperPasswordPage by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun cancelDeveloperPassword() {
        showDeveloperPasswordPage = false
        passwordInput = ""
        passwordError = false
    }

    fun unlockDeveloper() {
        if (sha256(passwordInput) == DEV_PASSWORD_HASH) {
            cancelDeveloperPassword()
            currentPage = AppPage.SETTINGS
            showDeveloperPage = true
            showMessage("已进入开发者选项")
        } else {
            passwordError = true
        }
    }

    fun sendTestNotification() {
        val sent = tray.notifyTest()
        showMessage(
            if (sent) "已发送 Windows 测试通知" else "系统托盘不可用，无法发送通知"
        )
    }

    fun exportBackupMessage(): String {
        val file = BackupFiles.chooseExportFile() ?: return "已取消导出"
        return runCatching {
            BackupFiles.write(file, appState.exportBackup())
            "导出成功：${file.name}"
        }.getOrElse {
            "导出失败：${it.message ?: "未知错误"}"
        }
    }

    fun importBackupMessage(): String {
        val file = BackupFiles.chooseImportFile() ?: return "已取消导入"
        return runCatching {
            val summary = appState.importBackup(BackupFiles.read(file))
            if (summary.importedCount > 0) {
                "导入成功：新增 ${summary.importedCount} 条记录"
            } else {
                "导入完成：没有新增记录"
            }
        }.getOrElse {
            "导入失败：${it.message ?: "未知错误"}"
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (appState.settings.darkMode) {
        DarkModePreference.SYSTEM -> systemDark
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
    // Whole sp sizes keep glyphs aligned to whole pixels instead of fractional
    // scaling, which is the main reason text looked blurry on Windows.
    val fontMultiplier = when (appState.settings.fontSizeLevel) {
        0 -> 0.85f
        1 -> 0.92f
        2 -> 1.0f
        3 -> 1.15f
        4 -> 1.30f
        else -> 1.0f
    }

    HuolemeTheme(
        themeMode = appState.settings.themeMode,
        darkTheme = isDark,
        customColor = Color(appState.settings.customColor),
        typography = scaledTypography(fontMultiplier)
    ) {
        if (!appState.firstLaunchDone) {
            OnboardingScreen(
                initialBirthDate = appState.settings.birthDate,
                onBirthDateSaved = { date ->
                    appState.updateSettings { it.copy(birthDate = date) }
                },
                onImport = { importBackupMessage() },
                onFinish = { appState.completeFirstLaunch() }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            Box(modifier = Modifier.size(56.dp))
                        },
                        title = {
                            Column(
                                modifier = Modifier.clickable {
                                    developerClickCount++
                                    if (developerClickCount >= 7) {
                                        developerClickCount = 0
                                        passwordInput = ""
                                        passwordError = false
                                        showDeveloperPasswordPage = true
                                    }
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Strings.appTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Strings.appSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        },
                        actions = {
                            Box(modifier = Modifier.size(56.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!isWide) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            NavigationBarItem(
                                selected = currentPage == AppPage.DASHBOARD,
                                onClick = {
                                    currentPage = AppPage.DASHBOARD
                                    showDeveloperPage = false
                                    showDeveloperPasswordPage = false
                                },
                                icon = {
                                    Icon(Icons.Filled.Home, contentDescription = Strings.navHome)
                                },
                                label = { Text(Strings.navHome) },
                                alwaysShowLabel = false,
                                colors = navigationItemColors()
                            )
                            NavigationBarItem(
                                selected = currentPage == AppPage.HISTORY,
                                onClick = {
                                    currentPage = AppPage.HISTORY
                                    showDeveloperPage = false
                                    showDeveloperPasswordPage = false
                                },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = Strings.navHistory
                                    )
                                },
                                label = { Text(Strings.navHistory) },
                                alwaysShowLabel = false,
                                colors = navigationItemColors()
                            )
                            NavigationBarItem(
                                selected = currentPage == AppPage.SETTINGS,
                                onClick = {
                                    currentPage = AppPage.SETTINGS
                                    showDeveloperPage = false
                                    showDeveloperPasswordPage = false
                                },
                                icon = {
                                    Icon(Icons.Filled.Settings, contentDescription = Strings.navSettings)
                                },
                                label = { Text(Strings.navSettings) },
                                alwaysShowLabel = false,
                                colors = navigationItemColors()
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (isWide) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationRailItem(
                                selected = currentPage == AppPage.DASHBOARD,
                                onClick = {
                                    currentPage = AppPage.DASHBOARD
                                    showDeveloperPage = false
                                    showDeveloperPasswordPage = false
                                },
                                icon = {
                                    Icon(Icons.Filled.Home, contentDescription = Strings.navHome)
                                },
                                label = { Text(Strings.navHome) },
                                alwaysShowLabel = true
                            )
                            NavigationRailItem(
                                selected = currentPage == AppPage.HISTORY,
                                onClick = {
                                    currentPage = AppPage.HISTORY
                                    showDeveloperPage = false
                                    showDeveloperPasswordPage = false
                                },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = Strings.navHistory
                                    )
                                },
                                label = { Text(Strings.navHistory) },
                                alwaysShowLabel = true
                            )
                            NavigationRailItem(
                                selected = currentPage == AppPage.SETTINGS,
                                onClick = {
                                    currentPage = AppPage.SETTINGS
                                    showDeveloperPage = false
                                    showDeveloperPasswordPage = false
                                },
                                icon = {
                                    Icon(Icons.Filled.Settings, contentDescription = Strings.navSettings)
                                },
                                label = { Text(Strings.navSettings) },
                                alwaysShowLabel = true
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = if (isWide) 760.dp else 520.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                            ) {
                                if (showDeveloperPasswordPage) {
                                    DeveloperPasswordPage(
                                        passwordInput = passwordInput,
                                        onPasswordChange = {
                                            passwordInput = it
                                            passwordError = false
                                        },
                                        passwordError = passwordError,
                                        onUnlock = { unlockDeveloper() },
                                        onCancel = { cancelDeveloperPassword() }
                                    )
                                } else {
                                    Crossfade(
                                        targetState = currentPage,
                                        animationSpec = if (appState.settings.animationsEnabled) {
                                            tween()
                                        } else {
                                            snap()
                                        },
                                        label = "page_transition"
                                    ) { page ->
                                        when (page) {
                                            AppPage.DASHBOARD -> DashboardPage(
                                                appState = appState,
                                                infoBar = infoBar,
                                                onInfoBarClose = { infoBar = null },
                                                onCheckIn = {
                                                    if (appState.checkIn()) {
                                                        infoBar = "生存判定更新" to
                                                            "打卡成功！您目前状态为 [正常存活中]，连续守护时间已递增。"
                                                    }
                                                }
                                            )

                                            AppPage.HISTORY -> HistoryPage(records = appState.records)

                                            AppPage.SETTINGS -> if (showDeveloperPage) {
                                                DeveloperPage(
                                                    onBack = { showDeveloperPage = false },
                                                    onTestNotification = { sendTestNotification() },
                                                    onInjectPastCheckIns = {
                                                        appState.injectPastCheckIns(7)
                                                        showMessage("已回填最近 7 天记录")
                                                    },
                                                    onUndoCheckIn = {
                                                        appState.undoTodayCheckIn()
                                                        showMessage("已撤销今日打卡")
                                                    },
                                                    onClearRecords = {
                                                        appState.clearAllRecords()
                                                    },
                                                    onMessage = { showMessage(it) }
                                                )
                                            } else {
                                                SettingsPage(
                                                    appState = appState,
                                                    onOpenDataFolder = {
                                                        runCatching {
                                                            val parent = appState.dataFile.parent
                                                            Files.createDirectories(parent)
                                                            Desktop.getDesktop().open(parent.toFile())
                                                        }
                                                    },
                                                    onExport = { showMessage(exportBackupMessage()) },
                                                    onImport = { showMessage(importBackupMessage()) },
                                                    onMessage = { showMessage(it) },
                                                    autoStartStatus = autoStartStatus
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val DEV_PASSWORD_HASH =
    "5b69fc44c983e8806747ad338c5dc235145873250b49b82faffc68595b310576"

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
)
