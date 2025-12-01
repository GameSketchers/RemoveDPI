package com.anonimbiri.removedpi.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anonimbiri.removedpi.vpn.LogManager
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onNavigateBack: () -> Unit
) {
    val logs by LogManager.logs.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    // Logları dosyaya kaydetme işleyicisi
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { saveLogsToFile(context, it, logs) }
    }

    // Yeni log gelince otomatik aşağı kaydır
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Kayıtları") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Kaydet Butonu (Small FAB)
                SmallFloatingActionButton(
                    onClick = {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        saveFileLauncher.launch("removedpi_log_$timeStamp.txt")
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Save, "Kaydet")
                }

                // Sil Butonu (Normal FAB)
                FloatingActionButton(
                    onClick = { LogManager.clear() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(Icons.Default.Delete, "Temizle")
                }
            }
        }
    ) { paddingValues ->
        // Yazıların seçilebilir olması için SelectionContainer
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = log.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = when(log.level) {
                                LogManager.Level.ERROR -> MaterialTheme.colorScheme.error
                                LogManager.Level.WARN -> MaterialTheme.colorScheme.tertiary
                                LogManager.Level.BYPASS -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                // FAB'ların altında boşluk kalsın
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

private fun saveLogsToFile(context: Context, uri: Uri, logs: List<LogManager.LogEntry>) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                logs.forEach { log ->
                    writer.write("[${log.time}] [${log.level}] ${log.message}\n")
                }
            }
        }
        Toast.makeText(context, "Loglar kaydedildi", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
    }
}