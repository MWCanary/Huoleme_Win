package com.huoleme.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huoleme.app.data.AppState
import javax.imageio.ImageIO
import com.huoleme.app.data.DarkModePreference
import com.huoleme.app.theme.AppThemeMode
import com.huoleme.app.theme.BluePrimary
import com.huoleme.app.theme.GreenPrimary
import com.huoleme.app.theme.OrangePrimary
import com.huoleme.app.theme.RedPrimary
import com.huoleme.app.ui.BirthDateEditor
import com.huoleme.app.ui.ColorCode
import com.huoleme.app.ui.touchKeyboard
import com.huoleme.app.ui.touchScrollable

private data class ThemeOption(
    val mode: AppThemeMode,
    val label: String,
    val color: Color
)

private fun themeOptions(customColor: Color) = listOf(
    ThemeOption(AppThemeMode.CLASSIC_BLUE, Strings.themeBlue, BluePrimary),
    ThemeOption(AppThemeMode.SURVIVAL_RED, Strings.themeRed, RedPrimary),
    ThemeOption(AppThemeMode.FOREST_GREEN, Strings.themeGreen, GreenPrimary),
    ThemeOption(AppThemeMode.SUNSET_ORANGE, Strings.themeOrange, OrangePrimary),
    ThemeOption(AppThemeMode.CUSTOM, Strings.themeCustom, customColor)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(
    appState: AppState,
    onOpenDataFolder: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onMessage: (String) -> Unit,
    autoStartStatus: String
) {
    val settings = appState.settings
    val options = themeOptions(Color(settings.customColor))
    val scrollState = rememberScrollState()
    var testamentInput by remember { mutableStateOf(settings.testament) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showSponsorDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(settings.testament) {
        testamentInput = settings.testament
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .touchScrollable(scrollState)
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSectionTitle(Strings.settingsThemeSection)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                Column(
                    modifier = Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 48dp target keeps the color picker usable for touch and pen input.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .selectable(
                                selected = settings.themeMode == option.mode,
                                role = Role.RadioButton,
                                onClick = {
                                    appState.updateSettings { it.copy(themeMode = option.mode) }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (settings.themeMode == option.mode) 3.dp else 1.dp,
                                    color = if (settings.themeMode == option.mode) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (settings.themeMode == option.mode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // The color picker only appears after the "自定义" theme dot is selected.
        if (settings.themeMode == AppThemeMode.CUSTOM) {
            CustomColorEditor(
                currentColor = Color(settings.customColor),
                onApply = { color ->
                    appState.updateSettings {
                        it.copy(
                            themeMode = AppThemeMode.CUSTOM,
                            customColor = color.toArgb()
                        )
                    }
                }
            )
        }

        SettingsSectionTitle(Strings.settingsDarkMode)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModeChip(
                label = Strings.darkSystem,
                selected = settings.darkMode == DarkModePreference.SYSTEM,
                onClick = {
                    appState.updateSettings { it.copy(darkMode = DarkModePreference.SYSTEM) }
                }
            )
            DarkModeChip(
                label = Strings.darkLight,
                selected = settings.darkMode == DarkModePreference.LIGHT,
                onClick = {
                    appState.updateSettings { it.copy(darkMode = DarkModePreference.LIGHT) }
                }
            )
            DarkModeChip(
                label = Strings.darkDark,
                selected = settings.darkMode == DarkModePreference.DARK,
                onClick = {
                    appState.updateSettings { it.copy(darkMode = DarkModePreference.DARK) }
                }
            )
        }

        SettingsSectionTitle(Strings.settingsFontSize)
        Text(
            text = Strings.settingsFontSizeDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Strings.fontSize0,
                Strings.fontSize1,
                Strings.fontSize2,
                Strings.fontSize3,
                Strings.fontSize4
            ).forEachIndexed { index, label ->
                FilterChip(
                    selected = settings.fontSizeLevel == index,
                    onClick = {
                        appState.updateSettings { it.copy(fontSizeLevel = index) }
                    },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.animationTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Strings.animationDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.animationsEnabled,
                onCheckedChange = { enabled ->
                    appState.updateSettings { it.copy(animationsEnabled = enabled) }
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        SettingsSectionTitle(Strings.profileSection)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = Strings.birthDateTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Strings.birthDateDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BirthDateEditor(
                    initialDate = settings.birthDate,
                    onSave = { date ->
                        appState.updateSettings { it.copy(birthDate = date) }
                        onMessage(if (date.isBlank()) "生日已清除" else "生日已保存")
                    },
                    saveLabel = Strings.saveBirthDate
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        SettingsSectionTitle(Strings.testamentSection)
        Text(
            text = Strings.testamentDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = testamentInput,
            onValueChange = { testamentInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .touchKeyboard(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text(Strings.testamentDefault) }
        )
        Button(
            onClick = {
                appState.updateSettings { it.copy(testament = testamentInput.trim()) }
                onMessage("遗言已保存")
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(Strings.saveTestament)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        SettingsSectionTitle(Strings.notificationSection)
        Text(
            text = Strings.notificationDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = Strings.notificationTimeLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Strings.notificationTimeHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // A single HH:MM field replaces the old hour/minute steppers and
            // the extra dialog: any minute can be typed directly.
            NotificationTimeField(
                hour = settings.notificationHour,
                minute = settings.notificationMinute,
                onTimeChange = { hour, minute ->
                    appState.updateSettings {
                        it.copy(notificationHour = hour, notificationMinute = minute)
                    }
                    onMessage(Strings.notificationTimeSaved.format(hour, minute))
                }
            )
        }

        Text(
            text = Strings.notificationNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.minimizeToTrayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Strings.minimizeToTrayDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.minimizeToTray,
                onCheckedChange = { enabled ->
                    appState.updateSettings { it.copy(minimizeToTray = enabled) }
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        SettingsSectionTitle(Strings.autoStartTitle)
        Text(
            text = Strings.autoStartDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 17.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.autoStartTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (settings.autoStartEnabled) "已开启" else "已关闭",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.autoStartEnabled,
                onCheckedChange = { enabled ->
                    appState.updateSettings {
                        it.copy(autoStartEnabled = enabled)
                    }
                    onMessage(if (enabled) "正在注册开机自启动…" else "正在移除开机自启动…")
                }
            )
        }
        Text(
            text = autoStartStatus,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (autoStartStatus.contains("失败")) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Text(
            text = Strings.autoStartNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        SettingsSectionTitle(Strings.dataSection)
        Text(
            text = Strings.backupDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text(Strings.exportJson)
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text(Strings.importJson)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = Strings.dataFileLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = appState.dataFile.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                OutlinedButton(
                    onClick = onOpenDataFolder,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(Strings.openDataFolder)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // Android-style "About" card: header + info rows + 下载更新 / 联系作者
        // rows inside the same card, separated by dividers.
        SettingsSectionTitle(Strings.settingsAbout)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = AppInfo.NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${AppInfo.NAME} · V${AppInfo.VERSION}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                AboutRow(Icons.Filled.Build, Strings.versionLabel, "V${AppInfo.VERSION}")
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showDownloadDialog = true }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Strings.downloadUpdate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showContactDialog = true }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Strings.contactAuthor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

            }
        }

        // Android-style author card at the very bottom of the settings page.
        AuthorSponsorCard(onSponsor = { showSponsorDialog = true })
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text(Strings.downloadConfirmTitle) },
            text = { Text(Strings.downloadConfirmText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadDialog = false
                    uriHandler.openUri("https://huoleme.pages.dev/download")
                }) {
                    Text(Strings.downloadConfirmOpen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text(Strings.cancel)
                }
            }
        )
    }

    if (showSponsorDialog) {
        SponsorDialog(
            onDismiss = { showSponsorDialog = false },
            onOpenLink = { uriHandler.openUri(Strings.sponsorAlipayUrl) }
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text(Strings.contactAuthor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = Strings.contactAuthorHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ContactButton(Strings.telegramContact) {
                        showContactDialog = false
                        uriHandler.openUri("https://t.me/mwcanary")
                    }
                    ContactButton(Strings.whatsappContact) {
                        showContactDialog = false
                        uriHandler.openUri("https://wa.me/qr/D5YEI5LG6RG3F1")
                    }
                    ContactButton(Strings.teamsContact) {
                        showContactDialog = false
                        uriHandler.openUri("https://teams.live.com/l/invite/FEAKgipM-NHSstOjg?v=g1")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DarkModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) }
    )
}

/**
 * Color picker shown only for the CUSTOM theme. Mirrors the Android version:
 * a collapsed card by default; expanding it reveals RGB sliders with a live
 * preview plus the HEX input. Sliders apply the theme when the thumb is released.
 */
@Composable
private fun CustomColorEditor(
    currentColor: Color,
    onApply: (Color) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(ColorCode.format(currentColor)) }
    var invalid by remember { mutableStateOf(false) }
    val parsed = ColorCode.parse(input)

    LaunchedEffect(currentColor) {
        input = ColorCode.format(currentColor)
        invalid = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { expanded = !expanded },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                    Column {
                        Text(
                            text = Strings.customColorTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (expanded) {
                                Strings.customColorExpandedHint
                            } else {
                                Strings.customColorCollapsedHint
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "折叠颜色选择" else "展开颜色选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var rValue by remember(currentColor) {
                    mutableStateOf(((currentColor.toArgb() shr 16) and 0xFF).toFloat())
                }
                var gValue by remember(currentColor) {
                    mutableStateOf(((currentColor.toArgb() shr 8) and 0xFF).toFloat())
                }
                var bValue by remember(currentColor) {
                    mutableStateOf((currentColor.toArgb() and 0xFF).toFloat())
                }
                val previewColor = Color(rValue.toInt(), gValue.toInt(), bValue.toInt())
                val applyPreview = {
                    onApply(Color(rValue.toInt(), gValue.toInt(), bValue.toInt()))
                }

                Text(
                    text = Strings.rgbSlidersTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(previewColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.rgbPreviewHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ColorSlider("红 (R)", rValue, previewColor, { rValue = it }, applyPreview)
                ColorSlider("绿 (G)", gValue, previewColor, { gValue = it }, applyPreview)
                ColorSlider("蓝 (B)", bValue, previewColor, { bValue = it }, applyPreview)

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Text(
                    text = Strings.hexInputTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Strings.customColorDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(parsed ?: currentColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            invalid = false
                        },
                        label = { Text(Strings.colorCodeLabel) },
                        placeholder = { Text("#00897B") },
                        singleLine = true,
                        isError = invalid,
                        supportingText = {
                            if (invalid) {
                                Text(Strings.colorCodeInvalid)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .touchKeyboard()
                    )
                }
                OutlinedButton(
                    onClick = {
                        val color = ColorCode.parse(input)
                        if (color == null) {
                            invalid = true
                        } else {
                            onApply(color)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(Strings.applyColor)
                }
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    previewColor: Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ${value.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(76.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = previewColor,
                thumbColor = previewColor
            )
        )
    }
}

@Composable
private fun ContactButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = CircleShape
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Author card: title + intro on the left, sponsor pill on the right.
 * Colors come from the active MaterialTheme, so in every light/dark and accent
 * scheme it matches the rest of the settings page instead of clashing with it.
 */
@Composable
private fun AuthorSponsorCard(onSponsor: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.authorCardTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.aboutDesc,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onSponsor,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Strings.sponsorAuthor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/** Shows the Alipay sponsorship QR code bundled as a resource. */
@Composable
private fun SponsorDialog(onDismiss: () -> Unit, onOpenLink: () -> Unit) {
    val qr = remember {
        runCatching {
            AppInfo::class.java.getResourceAsStream("/sponsor_qr.png")?.use { stream ->
                ImageIO.read(stream).toComposeImageBitmap()
            }
        }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.sponsorDialogTitle) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Strings.sponsorDialogHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (qr != null) {
                    Image(
                        bitmap = qr,
                        contentDescription = "支付宝赞助二维码",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    text = Strings.sponsorAlipayUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(); onOpenLink() }) {
                Text(Strings.openAlipayLink)
            }
        }
    )
}

private fun parseNotificationTime(input: String): Pair<Int, Int>? {
    val parts = input.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}

/**
 * The single HH:MM input box for the daily reminder time. A complete, valid
 * time is saved as it is typed; pressing Enter also accepts short forms such
 * as 7:3 and normalises them to 07:03. Any minute 00-59 is accepted.
 */
@Composable
private fun NotificationTimeField(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    var text by remember(hour, minute) { mutableStateOf("%02d:%02d".format(hour, minute)) }
    var invalid by remember { mutableStateOf(false) }

    fun commit() {
        val parsed = parseNotificationTime(text)
        if (parsed == null) {
            invalid = true
            return
        }
        invalid = false
        text = "%02d:%02d".format(parsed.first, parsed.second)
        if (parsed.first != hour || parsed.second != minute) {
            onTimeChange(parsed.first, parsed.second)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.take(5)
            val parsed = parseNotificationTime(text)
            val minuteComplete = text.substringAfter(':', "").length == 2
            if (parsed != null && minuteComplete) {
                invalid = false
                if (parsed.first != hour || parsed.second != minute) {
                    onTimeChange(parsed.first, parsed.second)
                }
            } else {
                invalid = text.length == 5 && parsed == null
            }
        },
        placeholder = { Text("HH:MM") },
        supportingText = {
            Text(
                text = if (invalid) {
                    Strings.notificationTimeInvalid
                } else {
                    Strings.notificationCustomHint
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (invalid) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        isError = invalid,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .touchKeyboard(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() })
    )
}
