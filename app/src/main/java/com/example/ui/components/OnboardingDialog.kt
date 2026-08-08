package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.Emerald500
import com.example.ui.viewmodel.CoachViewModel

val NATIVE_LANGUAGES = listOf(
    "Türkçe",
    "Español",
    "Deutsch",
    "Français",
    "Italiano",
    "Português",
    "Русский",
    "العربية",
    "English"
)

val CEFR_LEVEL_OPTIONS = listOf(
    "CEFR A1" to "Elementary - Temel Seviye (Kısa ve basit cümleler)",
    "CEFR A2" to "Pre-Intermediate - Günlük Konular (Basit zamanlar)",
    "CEFR B1" to "Intermediate - Orta Seviye (Günlük doğal diyaloglar - Varsayılan)",
    "CEFR B2" to "Upper-Intermediate - İyi Seviye (Akıcı ve detaylı konular)",
    "CEFR C1" to "Advanced - İleri Seviye (Karmaşık ve zengin kelimeler)"
)

@Composable
fun OnboardingDialog(
    viewModel: CoachViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val partialText by viewModel.partialSpeechText.collectAsState()

    var nameInput by remember { mutableStateOf(uiState.userName) }
    var selectedNativeLang by remember { mutableStateOf(if (uiState.nativeLanguage.isBlank()) "Türkçe" else uiState.nativeLanguage) }
    var selectedLevel by remember { mutableStateOf(if (uiState.cefrLevel.isBlank()) "CEFR B1" else uiState.cefrLevel) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var customInterestInput by remember { mutableStateOf("") }

    var langDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        }
    }

    // Update name if speech recognizer captures text while onboarding
    LaunchedEffect(partialText) {
        if (isListening && partialText.isNotBlank()) {
            nameInput = partialText
        }
    }

    Dialog(
        onDismissRequest = { /* Force explicit completion */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize()
            ) {
                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Hoş Geldiniz!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Yapay zeka İngilizce Koçunuz sizi tanımak istiyor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Name Input Section (Voice / Keyboard)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("1. İsminiz (Name)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Adınız veya Takma Adınız") },
                                placeholder = { Text("Örn: Mehmet, Ayşe, Alex") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isListening) {
                                                viewModel.stopVoiceRecording()
                                            } else {
                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                    context, Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPermission) {
                                                    viewModel.startVoiceRecording()
                                                } else {
                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Voice input for name",
                                            tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (isListening) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🎙️ Dinleniyor... İsminizi söyleyebilirsiniz.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Native Language Selection
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("2. Anadiliniz (Native Language)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Text(
                                text = "Hatalarınız ve dilbilgisi açıklamaları bu dilde verilecektir.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedNativeLang,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Anadil Seçimi") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (langDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand native language dropdown",
                                            modifier = Modifier.clickable { langDropdownExpanded = !langDropdownExpanded }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { langDropdownExpanded = !langDropdownExpanded }
                                        .testTag("onboarding_language_select"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                DropdownMenu(
                                    expanded = langDropdownExpanded,
                                    onDismissRequest = { langDropdownExpanded = false },
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
                                                langDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. CEFR English Level Selection
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("3. İngilizce Seviyeniz (CEFR Level)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            CEFR_LEVEL_OPTIONS.forEach { (levelKey, levelDesc) ->
                                val isSelected = selectedLevel.contains(levelKey.split(" ").last(), ignoreCase = true) || selectedLevel == levelKey
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedLevel = levelKey }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedLevel = levelKey }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(text = levelKey, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = levelDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. User Interests & Hobbies
                    val interestOptions = remember {
                        listOf(
                            "🎬 Sinema & Dizi",
                            "💻 Teknoloji & Yazılım",
                            "⚽ Spor & Fitness",
                            "✈️ Seyahat & Gezi",
                            "🎵 Müzik & Sanat",
                            "📚 Kitap & Edebiyat",
                            "☕ Kahve & Yeme-İçme",
                            "🎮 Oyunlar & Espor",
                            "💼 İş & Kariyer"
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("4. İlgi Alanlarınız (Interests)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Text(
                                text = "Koçunuz sizinle günlük samimi sohbetler yaparken bu konulardan bahseder.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Grid of selectable interest chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                interestOptions.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { interest ->
                                            val isSelected = selectedInterests.contains(interest)
                                            Surface(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedInterests = if (isSelected) {
                                                        selectedInterests - interest
                                                    } else {
                                                        selectedInterests + interest
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = interest,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customInterestInput,
                                onValueChange = { customInterestInput = it },
                                label = { Text("Diğer Özel İlgi Alanları") },
                                placeholder = { Text("Örn: Astronomi, Felsefe, Satranç") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Audio Output Preference (Sesli / Yazılı)
                    var autoPlayTts by remember { mutableStateOf(uiState.autoPlayTts) }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("5. Cevap Tercihi (Sesli / Yazılı)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (autoPlayTts) "🔊 Sesli ve Yazılı Cevap" else "🔇 Sadece Yazı (Sessiz Ortam)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (autoPlayTts) "Koç cevapları sesli okur" else "Cevaplar sadece ekranda yazılı kalır",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = autoPlayTts,
                                    onCheckedChange = {
                                        autoPlayTts = it
                                        viewModel.updateAutoPlayTts(it)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fixed Sticky Bottom Complete Onboarding Button
                Button(
                    onClick = {
                        val finalName = nameInput.ifBlank { "Öğrenci" }
                        val englishInterests = selectedInterests.map { interest ->
                            when {
                                interest.contains("Sinema") -> "Movies & TV Shows"
                                interest.contains("Teknoloji") -> "Technology & Software"
                                interest.contains("Spor") -> "Sports & Fitness"
                                interest.contains("Seyahat") -> "Travel & Tourism"
                                interest.contains("Müzik") -> "Music & Art"
                                interest.contains("Kitap") -> "Books & Literature"
                                interest.contains("Kahve") -> "Coffee & Dining"
                                interest.contains("Oyunlar") -> "Gaming & Esports"
                                interest.contains("Kariyer") -> "Business & Career"
                                else -> interest.trim()
                            }
                        }
                        val customItems = customInterestInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val allInterests = (englishInterests + customItems).distinct().joinToString(", ")
                        viewModel.completeOnboarding(
                            name = finalName,
                            nativeLang = selectedNativeLang,
                            level = selectedLevel,
                            interests = allInterests
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_complete_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Koçla Tanış ve Başla", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
