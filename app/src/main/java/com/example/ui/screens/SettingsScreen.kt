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
                    text = "Profil, AI Sağlayıcı (NVIDIA/DeepSeek/Groq), Konuşma Hızı ve Düzeltme Dili",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kullanıcı Profili & Hitap Şekli",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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

                // Microsoft Edge Neural TTS Engine Switch
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.useEdgeNeuralTts) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚡",
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Microsoft Edge Neural TTS (Canlı Ses)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.useEdgeNeuralTts) "Microsoft Yapay Zeka Gerçekçi İnsan Sesi (Ücretsiz)" else "Cihaz Dahili (Android Standart TTS)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.useEdgeNeuralTts,
                                onCheckedChange = { viewModel.updateUseEdgeNeuralTts(it) },
                                modifier = Modifier.testTag("edge_neural_tts_switch")
                            )
                        }

                        if (uiState.useEdgeNeuralTts) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Female Neural Voice Selector
                            Text(
                                text = "👩 MAYA (Kadın Ses Modeli):",
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

                            // Male Neural Voice Selector
                            Text(
                                text = "👨 LEO (Erkek Ses Modeli):",
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

                // Quick Preset Chips for NVIDIA, DeepSeek, Groq
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            val prefix = when(provider) {
                                LlmProvider.NVIDIA -> "🟢"
                                LlmProvider.DEEPSEEK -> "🔵"
                                LlmProvider.GROQ -> "⚡"
                                else -> "⚙️"
                            }
                            Text("$prefix ${provider.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    LlmProvider.GROQ -> "gsk_..."
                    LlmProvider.NVIDIA -> "nvapi-..."
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
                                Text(gender.avatarEmoji, fontSize = 28.sp)
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
