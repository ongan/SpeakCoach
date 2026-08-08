package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.LlmProvider
import com.example.ui.components.CoachAvatar
import com.example.ui.components.NATIVE_LANGUAGES
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.viewmodel.CoachViewModel
import com.example.ui.viewmodel.ConnectionTestState

@Composable
fun SettingsScreen(viewModel: CoachViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var apiKeyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var baseUrlInput by remember(uiState.baseUrl) { mutableStateOf(uiState.baseUrl) }
    var modelNameInput by remember(uiState.modelName) { mutableStateOf(uiState.modelName) }

    var userNameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    var selectedNativeLang by remember(uiState.nativeLanguage) { mutableStateOf(uiState.nativeLanguage) }

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    var providerDropdownExpanded by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var nativeLangDropdownExpanded by remember { mutableStateOf(false) }

    var showProfileSelectionDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }

    if (showProfileSelectionDialog) {
        com.example.ui.components.ProfileSelectionDialog(
            profiles = uiState.profiles,
            activeProfileId = uiState.activeProfileId,
            onSelectProfile = { profile ->
                viewModel.selectProfile(profile)
                showProfileSelectionDialog = false
            },
            onAddNewProfile = {
                showProfileSelectionDialog = false
                showOnboardingDialog = true
            },
            onDeleteProfile = { profile ->
                viewModel.deleteProfile(profile)
            },
            onDismiss = { showProfileSelectionDialog = false }
        )
    }

    if (showOnboardingDialog) {
        com.example.ui.components.OnboardingDialog(
            viewModel = viewModel,
            onDismiss = { showOnboardingDialog = false }
        )
    }

    var sliderSpeechRate by remember(uiState.speechRate) { mutableFloatStateOf(uiState.speechRate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Ayarlar & Kişiselleştirme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Profil, AI Sağlayıcı (Cerebras/Gemini/DeepSeek/Groq), Konuşma Hızı ve Düzeltme Dili",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1. User Profile & Native Language Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kullanıcı Profili",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kayıtlı Profil Sayısı: ${uiState.profiles.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showProfileSelectionDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⇄ Profil Yönet / Değiştir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = {
                        userNameInput = it
                        viewModel.updateUserName(it)
                    },
                    label = { Text("İsminiz (Name)") },
                    placeholder = { Text("Örn: Mehmet") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_user_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Native Language Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedNativeLang,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Anadiliniz (Grammar & Hata Açıklama Dili)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Language, contentDescription = null) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (nativeLangDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Expand native language dropdown",
                                modifier = Modifier.clickable { nativeLangDropdownExpanded = !nativeLangDropdownExpanded }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nativeLangDropdownExpanded = !nativeLangDropdownExpanded }
                            .testTag("settings_native_lang_select"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = nativeLangDropdownExpanded,
                        onDismissRequest = { nativeLangDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        NATIVE_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang,
                                        fontWeight = if (selectedNativeLang == lang) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedNativeLang == lang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedNativeLang = lang
                                    viewModel.updateNativeLanguage(lang)
                                    nativeLangDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Memory & User Interests Card
        var userInterestsInput by remember(uiState.userMemory?.interests) {
            mutableStateOf(uiState.userMemory?.interests ?: "")
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🧠 Arka Plan Hafızası & İlgi Alanları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = userInterestsInput,
                    onValueChange = {
                        userInterestsInput = it
                        viewModel.updateUserInterests(it)
                    },
                    label = { Text("İlgi Alanlarınız & Hobileriniz") },
                    placeholder = { Text("Örn: Sinema, Teknoloji, Spor, Seyahat, Kahve") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!uiState.userMemory?.conversationSummary.isNullOrBlank() || !uiState.userMemory?.learnedFacts.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📌 Hafızadaki Son Konuşma Özeti:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.userMemory?.conversationSummary?.ifBlank { "Henüz özet bulunmuyor." } ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!uiState.userMemory?.learnedFacts.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 Hakkınızda Öğrenilen Bilgiler:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.userMemory?.learnedFacts ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearUserMemory() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Konuşma Hafızasını Sıfırla", fontSize = 13.sp)
                    }
                } else {
                    Text(
                        text = "Siz Koçla konuştukça sohbet özetleri ve hakkınızda öğrenilen detaylar otomatik olarak SQLite hafızasına kaydedilecektir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Speech Rate & Voice Mode Controls
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sesli Cevap & Konuşma Hızı Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quiet vs Voice Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (uiState.autoPlayTts) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = if (uiState.autoPlayTts) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (uiState.autoPlayTts) "🔊 Sesli & Yazılı Cevap" else "🔇 Sadece Yazı (Kalabalık Ortam)",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.autoPlayTts) "Koç cevapları otomatik seslendirir" else "Sessiz mod - Sadece ekranda gösterilir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.autoPlayTts,
                        onCheckedChange = { viewModel.updateAutoPlayTts(it) },
                        modifier = Modifier.testTag("auto_play_tts_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. TTS Provider & Mode Selector Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔊 Ses Motoru",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Koçun konuşurken kullanacağı sesi seçin.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Edge Consumer is temporarily unavailable. Keep the implementation
                        // dormant so it can return later behind a maintained proxy service.
                        val modes = listOf(
                            com.example.audio.TtsEngineMode.KOKORO_OFFLINE,
                            com.example.audio.TtsEngineMode.ANDROID_SYSTEM
                        )
                        modes.forEach { mode ->
                            val isSelected = uiState.ttsEngineMode == mode
                            val title = when (mode) {
                                com.example.audio.TtsEngineMode.KOKORO_OFFLINE -> "Kokoro Neural"
                                com.example.audio.TtsEngineMode.ANDROID_SYSTEM -> "Android Sistem Sesi"
                                com.example.audio.TtsEngineMode.EDGE_EXPERIMENTAL -> "Microsoft Edge"
                            }
                            val subtitle = when (mode) {
                                com.example.audio.TtsEngineMode.KOKORO_OFFLINE -> "Doğal yapay zekâ sesi cihazda çalışır; internet gerekmez."
                                com.example.audio.TtsEngineMode.ANDROID_SYSTEM -> "Cihazın yerleşik ses motorunu kullanır; hızlı ve düşük kaynak tüketir."
                                com.example.audio.TtsEngineMode.EDGE_EXPERIMENTAL -> "Geçici olarak kullanılamıyor."
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateTtsEngineMode(mode) }
                                    .testTag("tts_mode_${mode.name}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateTtsEngineMode(mode) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (mode == com.example.audio.TtsEngineMode.KOKORO_OFFLINE) {
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Surface(
                                                color = Emerald500.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "ÖNERİLEN • ÇEVRİMDIŞI",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Emerald500,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = subtitle,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Seçili",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(9.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Engine Status Indicator Box
                        val activeEngineName = when (uiState.ttsEngineMode) {
                            com.example.audio.TtsEngineMode.KOKORO_OFFLINE -> "Kokoro Neural"
                            com.example.audio.TtsEngineMode.ANDROID_SYSTEM -> "Android Sistem Sesi"
                            com.example.audio.TtsEngineMode.EDGE_EXPERIMENTAL -> "Microsoft Edge"
                        }
                        val activeStatusText = when (uiState.ttsStatus) {
                            is com.example.audio.TtsStatus.Idle -> "Hazır"
                            is com.example.audio.TtsStatus.ModelNotDownloaded -> "Model İndirilmedi"
                            is com.example.audio.TtsStatus.Downloading -> "Model İndiriliyor..."
                            is com.example.audio.TtsStatus.Verifying -> "Model Doğrulanıyor..."
                            is com.example.audio.TtsStatus.Initializing -> "Motor Başlatılıyor..."
                            is com.example.audio.TtsStatus.Synthesizing -> "Ses Üretiliyor..."
                            is com.example.audio.TtsStatus.Playing -> "Çalınıyor 🔊"
                            is com.example.audio.TtsStatus.Error -> "Hata: ${(uiState.ttsStatus as com.example.audio.TtsStatus.Error).message}"
                            else -> "Beklemede"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🎯 Seçili: $activeEngineName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "⚡ Durum: $activeStatusText",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // KOKORO OFFLINE MODEL MANAGEMENT CARD
                        if (uiState.ttsEngineMode == com.example.audio.TtsEngineMode.KOKORO_OFFLINE) {
                            Spacer(modifier = Modifier.height(12.dp))

                            val isModelReady = viewModel.ttsManager.kokoroModelManager.isModelReady()
                            val downloadStatus = viewModel.ttsManager.kokoroModelManager.status.collectAsState().value

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Kokoro v1.0 int8 Offline Model",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = if (isModelReady) Emerald500.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isModelReady) "HAZIR • ${String.format("%.1f", uiState.kokoroModelDiskSizeMb)} MB" else "İNDİRİLMEDİ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isModelReady) Emerald500 else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Model: kokoro-int8-multi-lang-v1_0 ONNX (yaklaşık 100 MB tar.bz2)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Download Progress Info
                                    when (downloadStatus) {
                                        is com.example.audio.TtsStatus.Downloading -> {
                                            val progressPercent = (downloadStatus.progress * 100).toInt()
                                            val downloadedMb = downloadStatus.downloadedBytes / (1024f * 1024f)
                                            val totalMb = downloadStatus.totalBytes / (1024f * 1024f)

                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.material3.LinearProgressIndicator(
                                                progress = { downloadStatus.progress },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "İndiriliyor: %$progressPercent (${String.format("%.1f", downloadedMb)} / ${String.format("%.1f", totalMb)} MB)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                TextButton(
                                                    onClick = { viewModel.cancelKokoroDownload() },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("İptal Et", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                        is com.example.audio.TtsStatus.Verifying -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "📦 Model dosyaları doğrulanıyor ve kuruluyor... Lütfen bekleyin.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        is com.example.audio.TtsStatus.Error -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "❌ İndirme Hatası: ${downloadStatus.message}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                        else -> {}
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons for Kokoro model
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!isModelReady && downloadStatus !is com.example.audio.TtsStatus.Downloading) {
                                            Button(
                                                onClick = { viewModel.downloadKokoroModel() },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                                            ) {
                                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Kokoro Modelini İndir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (isModelReady) {
                                            OutlinedButton(
                                                onClick = { viewModel.deleteKokoroModel() },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Modeli Sil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Kokoro Female Voice Selector
                                    Text(
                                        text = "👩 MAYA Kokoro Ses Modeli:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    val kokoroFemaleOptions = listOf(
                                        "af_heart" to "af_heart (Sıcak & Doğal Kadın - Önerilen)",
                                        "af_bella" to "af_bella (Akıcı & Samimi)",
                                        "af_sarah" to "af_sarah (Net & Resmi)"
                                    )

                                    var kFemaleExpanded by remember { mutableStateOf(false) }

                                    Box {
                                        OutlinedButton(
                                            onClick = { kFemaleExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = kokoroFemaleOptions.firstOrNull { it.first == uiState.kokoroFemaleVoice }?.second ?: uiState.kokoroFemaleVoice,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = kFemaleExpanded,
                                            onDismissRequest = { kFemaleExpanded = false }
                                        ) {
                                            kokoroFemaleOptions.forEach { (vId, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                                    onClick = {
                                                        viewModel.updateKokoroFemaleVoice(vId)
                                                        kFemaleExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Kokoro Male Voice Selector
                                    Text(
                                        text = "👨 LEO Kokoro Ses Modeli:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    val kokoroMaleOptions = listOf(
                                        "am_michael" to "am_michael (Sıcak Erkek - Önerilen)",
                                        "am_adam" to "am_adam (Derin & Karizmatik)",
                                        "am_liam" to "am_liam (Genç & Enerjik)"
                                    )

                                    var kMaleExpanded by remember { mutableStateOf(false) }

                                    Box {
                                        OutlinedButton(
                                            onClick = { kMaleExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = kokoroMaleOptions.firstOrNull { it.first == uiState.kokoroMaleVoice }?.second ?: uiState.kokoroMaleVoice,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = kMaleExpanded,
                                            onDismissRequest = { kMaleExpanded = false }
                                        ) {
                                            kokoroMaleOptions.forEach { (vId, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                                    onClick = {
                                                        viewModel.updateKokoroMaleVoice(vId)
                                                        kMaleExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Audio Cache Management
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💾 Ses Önbelleği: ${String.format("%.2f", uiState.kokoroCacheSizeMb)} MB / 100 MB",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearAudioCache() },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Önbelleği Temizle", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // FALLBACK ENGINE CONFIGURATION
                        Text(
                            text = "🔄 Otomatik Yedek Motor Seçeneği:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val fallbackOptions = com.example.audio.FallbackEngineOption.values()
                        fallbackOptions.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateFallbackEngineOption(option) }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = (uiState.fallbackEngineOption == option),
                                    onClick = { viewModel.updateFallbackEngineOption(option) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = option.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val desc = when(option) {
                                        com.example.audio.FallbackEngineOption.OFF -> "Yedek motor kullanma - Başarısızlık durumunda hatayı bildir"
                                        com.example.audio.FallbackEngineOption.KOKORO -> "Ana motor başarısız olursa (Kokoro hazırsa) Kokoro'ya düş"
                                        com.example.audio.FallbackEngineOption.ANDROID_SYSTEM -> "Ana motor başarısız olursa yerleşik Android System TTS'ye düş (Robotik olabilir)"
                                    }
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.ttsEngineMode == com.example.audio.TtsEngineMode.EDGE_EXPERIMENTAL) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Female Edge Neural Voice Selector
                            Text(
                                text = "👩 MAYA Edge Ses Modeli:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val femaleOptions = listOf(
                                "en-US-AvaNeural" to "Ava (Doğal & Samimi - Önerilen)",
                                "en-US-AriaNeural" to "Aria (Net & Akıcı)",
                                "en-US-EmmaNeural" to "Emma (Dışadönük & Canlı)",
                                "en-US-AnaNeural" to "Ana (Yumuşak & Genç)"
                            )

                            var femaleExpanded by remember { mutableStateOf(false) }

                            Box {
                                OutlinedButton(
                                    onClick = { femaleExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = femaleOptions.firstOrNull { it.first == uiState.femaleEdgeVoice }?.second ?: uiState.femaleEdgeVoice,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                DropdownMenu(
                                    expanded = femaleExpanded,
                                    onDismissRequest = { femaleExpanded = false }
                                ) {
                                    femaleOptions.forEach { (voiceId, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                            onClick = {
                                                viewModel.updateFemaleEdgeVoice(voiceId)
                                                femaleExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Male Edge Neural Voice Selector
                            Text(
                                text = "👨 LEO Edge Ses Modeli:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val maleOptions = listOf(
                                "en-US-AndrewNeural" to "Andrew (Sıcak & Doğal - Önerilen)",
                                "en-US-BrianNeural" to "Brian (Pürüzsüz & Tonlamalı)",
                                "en-US-GuyNeural" to "Guy (Erkek Sohbet Ses)",
                                "en-US-ChristopherNeural" to "Christopher (Derin & Derli Toplu)"
                            )

                            var maleExpanded by remember { mutableStateOf(false) }

                            Box {
                                OutlinedButton(
                                    onClick = { maleExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = maleOptions.firstOrNull { it.first == uiState.maleEdgeVoice }?.second ?: uiState.maleEdgeVoice,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                DropdownMenu(
                                    expanded = maleExpanded,
                                    onDismissRequest = { maleExpanded = false }
                                ) {
                                    maleOptions.forEach { (voiceId, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                            onClick = {
                                                viewModel.updateMaleEdgeVoice(voiceId)
                                                maleExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Microsoft TTS Connection Test Button
                            OutlinedButton(
                                onClick = { viewModel.testMicrosoftTtsConnection() },
                                enabled = !uiState.isTestingTts,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_microsoft_tts_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.isTestingTts) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Microsoft TTS Test Ediliyor...", fontSize = 12.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ Edge TTS Bağlantısını Test Et", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Test Result Banner
                            uiState.ttsTestResult?.let { testRes ->
                                Spacer(modifier = Modifier.height(8.dp))
                                val cardBg = if (testRes.success) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                                val contentColor = if (testRes.success) Emerald500 else MaterialTheme.colorScheme.onErrorContainer

                                Surface(
                                    color = cardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (testRes.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = null,
                                                tint = contentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (testRes.success) "TTS Bağlantısı Başarılı!" else "TTS Bağlantı Hatası",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = contentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Motor: ${testRes.engineName} • Ses: ${testRes.voiceId}" +
                                                    (testRes.httpStatusCode?.let { " • HTTP $it" } ?: ""),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentColor
                                        )
                                        Text(
                                            text = testRes.message,
                                            fontSize = 11.sp,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speech Rate Presets & Slider
                Text(
                    text = "Konuşma Hızı: ${String.format("%.2fx", sliderSpeechRate)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Speed Presets
                val speedPresets = listOf(
                    0.5f to "0.5x Çok Yavaş",
                    0.75f to "0.75x Net",
                    0.9f to "0.9x Öğrenici",
                    1.0f to "1.0x Normal",
                    1.25f to "1.25x Hızlı"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    speedPresets.forEach { (presetRate, label) ->
                        val isSelected = Math.abs(sliderSpeechRate - presetRate) < 0.05f
                        Button(
                            onClick = {
                                sliderSpeechRate = presetRate
                                viewModel.setSpeechRate(presetRate)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(label.split(" ").first(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = sliderSpeechRate,
                    onValueChange = {
                        sliderSpeechRate = it
                        viewModel.setSpeechRate(it)
                    },
                    valueRange = 0.5f..1.5f,
                    steps = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speech_rate_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Test Speech Rate Button
                OutlinedButton(
                    onClick = { viewModel.testTtsPlayback() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔊 Konuşma Hızını Test Et & Dinle", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. AI Provider & Model Configuration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LLM Sağlayıcı & Model Seçimi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Provider Select Box
                Text(
                    text = "Aktif LLM Servisi (Provider):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Servis Seçimi (Select Box)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.SmartToy, contentDescription = null) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (providerDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Expand provider dropdown",
                                modifier = Modifier.clickable { providerDropdownExpanded = !providerDropdownExpanded }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { providerDropdownExpanded = !providerDropdownExpanded }
                            .testTag("provider_select_box"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = providerDropdownExpanded,
                        onDismissRequest = { providerDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        LlmProvider.values().forEach { provider ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = provider.displayName,
                                            fontWeight = if (uiState.selectedProvider == provider) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.selectedProvider == provider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = provider.defaultBaseUrl,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectProvider(provider)
                                    providerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Preset Chips for Cerebras, Gemini, DeepSeek, Groq
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LlmProvider.values().filter { it != LlmProvider.CUSTOM }.forEach { provider ->
                        val isSelected = uiState.selectedProvider == provider
                        Button(
                            onClick = { viewModel.selectProvider(provider) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            val prefix = when(provider) {
                                LlmProvider.CEREBRAS -> "🧠"
                                LlmProvider.GEMINI -> "✨"
                                LlmProvider.DEEPSEEK -> "🔵"
                                LlmProvider.GROQ -> "⚡"
                                else -> "⚙️"
                            }
                            Text("$prefix ${provider.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selection Dropdown
                Text(
                    text = "${uiState.selectedProvider.displayName} Modeli:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = modelNameInput,
                        onValueChange = {
                            modelNameInput = it
                            viewModel.updateModelName(it)
                        },
                        label = { Text("Model Seçimi (Select Box)") },
                        placeholder = { Text(uiState.selectedProvider.defaultModel) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.selectedProvider.availableModels.isNotEmpty()) {
                                IconButton(onClick = { modelDropdownExpanded = !modelDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (modelDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand models dropdown"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_select_box"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (uiState.selectedProvider.availableModels.isNotEmpty()) {
                        DropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            uiState.selectedProvider.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = model,
                                            fontWeight = if (modelNameInput == model) FontWeight.Bold else FontWeight.Normal,
                                            color = if (modelNameInput == model) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        modelNameInput = model
                                        viewModel.updateModelName(model)
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // API Key Input
                val apiKeyPlaceholder = when (uiState.selectedProvider) {
                    LlmProvider.CEREBRAS -> "csk-..."
                    LlmProvider.GEMINI -> "AIzaSy..."
                    LlmProvider.GROQ -> "gsk_..."
                    LlmProvider.DEEPSEEK -> "sk-..."
                    LlmProvider.CUSTOM -> "api-key-..."
                }

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        viewModel.updateApiKey(it)
                    },
                    label = { Text("${uiState.selectedProvider.displayName} API Key") },
                    placeholder = { Text(apiKeyPlaceholder) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Base URL Input
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = {
                        baseUrlInput = it
                        viewModel.updateBaseUrl(it)
                    },
                    label = { Text("API Base URL") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base_url_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Test Button
                OutlinedButton(
                    onClick = { viewModel.testApiConnection() },
                    enabled = uiState.testState !is ConnectionTestState.Testing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_connection_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.testState is ConnectionTestState.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Ediliyor (${uiState.selectedProvider.displayName})...")
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${uiState.selectedProvider.displayName} Bağlantısını Test Et", fontWeight = FontWeight.Bold)
                    }
                }

                // Connection Test Result Display
                when (val state = uiState.testState) {
                    is ConnectionTestState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Emerald500.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${state.providerName} bağlantısı başarılı! Model: ${state.modelName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                            }
                        }
                    }
                    is ConnectionTestState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${state.providerName} hatası: ${state.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. CEFR English Level Selection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "İngilizce Hedef Seviyesi (CEFR)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val levels = listOf(
                    "CEFR A1" to "Elementary - Ultra basit kelimeler & kısa cümleler",
                    "CEFR A2" to "Pre-Intermediate - Günlük temel diyaloglar & net sorular",
                    "CEFR B1" to "Intermediate - Günlük doğal İngilizce konuşma",
                    "CEFR B2" to "Upper-Intermediate - Akıcı iş ve sosyal sohbetler",
                    "CEFR C1" to "Advanced - Zengin kelimeler & derin akademik diyaloglar"
                )

                levels.forEach { (levelKey, levelDesc) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateCefrLevel(levelKey) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = uiState.cefrLevel.contains(levelKey.split(" ").last(), ignoreCase = true) || uiState.cefrLevel == levelKey,
                            onClick = { viewModel.updateCefrLevel(levelKey) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = levelKey, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = levelDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Coach Character & Visual Interface Display Modes Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Koç Karakteri & Görünüm Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Koçunuzu Seçin:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.example.data.model.CoachGender.values().forEach { gender ->
                        val isSelected = uiState.selectedCoachGender == gender
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateCoachGender(gender) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CoachAvatar(
                                    coachGender = gender,
                                    size = 92.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(gender.coachName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (gender == com.example.data.model.CoachGender.MAYA) "Kadın Ses & Karakter" else "Erkek Ses & Karakter",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Arayüz & Sohbet Görünümü:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show Avatars Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Koç ve Kullanıcı Avatarlarını Göster", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Sohbet listesinde profil simgelerini gösterir", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.showCoachAvatar,
                        onCheckedChange = { viewModel.updateViewModes(showAvatar = it, showBubbles = uiState.showChatBubbles) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Show Chat Bubbles Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mesaj Balonlarını Göster (Bubbles)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Açık: Kart balonları, Kapalı: Temiz düz yazı görünümü", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.showChatBubbles,
                        onCheckedChange = { viewModel.updateViewModes(showAvatar = uiState.showCoachAvatar, showBubbles = it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Unlock All Story Chapters Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔓 Tüm Hikaye Bölümlerini Aç", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("11 hikaye bölümü ve senaryoların kilitlerini doğrudan açar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.unlockAllStoryChapters,
                        onCheckedChange = { viewModel.updateUnlockAllStoryChapters(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Clear Chat History Button
        OutlinedButton(
            onClick = { showClearDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sohbet Geçmişini Temizle", fontWeight = FontWeight.Bold)
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Geçmişi Temizle?") },
                text = { Text("Koç ile olan tüm sohbet geçmişiniz silinecektir. Kaydedilen gramer notlarınız Defterinizde kalmaya devam edecektir.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                    ) {
                        Text("Temizle")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}
