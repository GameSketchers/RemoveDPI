package com.anonimbiri.removedpi.ui.screens

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.anonimbiri.removedpi.R

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    val strSystem = stringResource(R.string.lang_system)
    val strSystemDefault = stringResource(R.string.lang_system_default)
    val strTurkish = stringResource(R.string.lang_turkish)
    val strEnglish = stringResource(R.string.lang_english)
    val strJapanese = stringResource(R.string.lang_japanese)
    val strRussian = stringResource(R.string.lang_russian)

    val languages = remember(strSystem, strSystemDefault, strTurkish, strEnglish, strJapanese, strRussian) {
        listOf(
            Language("system", strSystem, strSystemDefault, R.drawable.ic_flag_system),
            Language("tr", strTurkish, "Türkçe", R.drawable.ic_flag_tr),
            Language("en", strEnglish, "English", R.drawable.ic_flag_en),
            Language("ja", strJapanese, "日本語", R.drawable.ic_flag_ja),
            Language("ru", strRussian, "Русский", R.drawable.ic_flag_ru)
        )
    }
    
    var currentLanguage by remember { mutableStateOf(getCurrentLanguageCode(context)) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_language_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.section_app_language),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
            }
            
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        languages.forEachIndexed { index, language ->
                            LanguageItem(
                                language = language,
                                isSelected = currentLanguage == language.code,
                                onClick = {
                                    currentLanguage = language.code
                                    setAppLanguage(context, language.code)
                                }
                            )
                            
                            if (index < languages.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flag_system),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        val linkColor = MaterialTheme.colorScheme.primary
                        val textColor = MaterialTheme.colorScheme.onSurface
                        
                        val fullMessageTemplate = stringResource(R.string.contrib_message)
                        val linkText = stringResource(R.string.contrib_link_text)

                        val fullText = String.format(fullMessageTemplate, linkText)
                        
                        val startIndex = fullText.indexOf(linkText)
                        val endIndex = startIndex + linkText.length

                        val annotatedString = buildAnnotatedString {
                            if (startIndex >= 0) {
                                withStyle(style = SpanStyle(color = textColor)) {
                                    append(fullText.substring(0, startIndex))
                                }

                                pushStringAnnotation(
                                    tag = "URL",
                                    annotation = "https://github.com/GameSketchers/RemoveDPI/pulls/new"
                                )
                                withStyle(
                                    style = SpanStyle(
                                        color = linkColor,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append(linkText)
                                }
                                pop()

                                withStyle(style = SpanStyle(color = textColor)) {
                                    append(fullText.substring(endIndex))
                                }
                            } else {
                                append(fullText)
                            }
                        }
                        
                        ClickableText(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodySmall,
                            onClick = { offset ->
                                annotatedString.getStringAnnotations(
                                    tag = "URL",
                                    start = offset,
                                    end = offset
                                ).firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                            }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = language.flagResId),
                contentDescription = language.name,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                language.nativeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (language.code != "system") {
                Text(
                    language.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getCurrentLanguageCode(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val locales = localeManager.applicationLocales
        if (locales.isEmpty) "system" else locales[0]?.language ?: "system"
    } else {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) "system" else locales[0]?.language ?: "system"
    }
}

private fun setAppLanguage(context: Context, languageCode: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = if (languageCode == "system") {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(languageCode)
        }
    } else {
        val localeList = if (languageCode == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}