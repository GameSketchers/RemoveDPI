package com.anonimbiri.removedpi.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DpiSettings
import com.anonimbiri.removedpi.data.SettingsRepository
import com.anonimbiri.removedpi.vpn.BypassVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val density = LocalDensity.current
    
    var settings by remember { mutableStateOf(DpiSettings()) }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var domainToEdit by remember { mutableStateOf<String?>(null) }
    var domainInput by remember { mutableStateOf("") }
    
    val searchBarHeight = 72.dp
    val searchBarHeightPx = with(density) { searchBarHeight.toPx() }
    var searchBarOffsetY by remember { mutableFloatStateOf(0f) }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                searchBarOffsetY = (searchBarOffsetY + delta).coerceIn(-searchBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }
    
    val visibleHeight by remember {
        derivedStateOf {
            ((searchBarHeightPx + searchBarOffsetY) / density.density).coerceAtLeast(0f).dp
        }
    }
    
    val searchBarAlpha by remember {
        derivedStateOf {
            ((searchBarHeightPx + searchBarOffsetY) / searchBarHeightPx).coerceIn(0f, 1f)
        }
    }
    
    LaunchedEffect(Unit) {
        repository.settings.collect { loadedSettings ->
            settings = loadedSettings
            BypassVpnService.settings = loadedSettings
        }
    }
    
    fun updateWhitelist(newSet: Set<String>) {
        val newSettings = settings.copy(whitelist = newSet)
        settings = newSettings
        BypassVpnService.settings = newSettings
        scope.launch { repository.updateSettings(newSettings) }
    }

    val filteredList = remember(settings.whitelist, searchQuery) {
        if (searchQuery.isBlank()) {
            settings.whitelist.toList().sorted()
        } else {
            settings.whitelist.filter { it.contains(searchQuery, ignoreCase = true) }.sorted()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .clipToBounds()
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.screen_whitelist_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = { 
                        IconButton(onClick = onNavigateBack) { 
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back)) 
                        } 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visibleHeight)
                        .graphicsLayer { alpha = searchBarAlpha }
                        .padding(horizontal = 16.dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (visibleHeight > 8.dp) {
                        ModernSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp) 
        ) {
            item {
                AddSiteCard(onClick = {
                    domainToEdit = null
                    domainInput = ""
                    showDialog = true
                })
            }

            if (filteredList.isEmpty() && searchQuery.isNotEmpty()) {
                item { 
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp), 
                        contentAlignment = Alignment.Center
                    ) { 
                        Text(
                            stringResource(R.string.no_results_found), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    } 
                }
            } else {
                items(filteredList) { domain ->
                    WhitelistItem(
                        domain = domain,
                        onClick = {
                            domainToEdit = domain
                            domainInput = domain
                            showDialog = true
                        },
                        onDelete = {
                            val newSet = settings.whitelist.toMutableSet()
                            newSet.remove(domain)
                            updateWhitelist(newSet)
                        }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (domainToEdit == null) stringResource(R.string.dialog_add_site_title) else stringResource(R.string.dialog_edit_site_title)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = domainInput,
                            onValueChange = { domainInput = it },
                            label = { Text(stringResource(R.string.label_domain_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.dialog_domain_desc), 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = { 
                    TextButton(onClick = {
                        if (domainInput.isNotBlank()) {
                            val clean = domainInput.trim()
                                .replace("https://", "")
                                .replace("http://", "")
                                .replace("www.", "")
                                .trim('/')
                            val newSet = settings.whitelist.toMutableSet()
                            if (domainToEdit != null) newSet.remove(domainToEdit)
                            newSet.add(clean)
                            updateWhitelist(newSet)
                            showDialog = false
                        }
                    }) { 
                        Text(if (domainToEdit == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_save)) 
                    } 
                },
                dismissButton = { 
                    TextButton(onClick = { showDialog = false }) { 
                        Text(stringResource(R.string.btn_cancel)) 
                    } 
                }
            )
        }
    }
}

@Composable
fun AddSiteCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                stringResource(R.string.card_add_site), 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WhitelistItem(
    domain: String, 
    onClick: () -> Unit, 
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncFavicon(domain = domain, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = domain, 
                style = MaterialTheme.typography.bodyLarge, 
                modifier = Modifier.weight(1f)
            )
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .clickable(onClick = onClick), 
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .clickable(onClick = onDelete), 
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.error, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ModernSearchBar(
    query: String, 
    onQueryChange: (String) -> Unit, 
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.search_hint), 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = query, 
                    onValueChange = onQueryChange, 
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontSize = 16.sp
                    ), 
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), 
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { 
                    Icon(
                        Icons.Default.Clear, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                }
            }
        }
    }
}

@Composable
fun AsyncFavicon(
    domain: String, 
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    LaunchedEffect(domain) {
        if (bitmap != null) return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.IO) {
            val sources = listOf(
                "https://www.google.com/s2/favicons?sz=64&domain_url=$domain",
                "https://icons.duckduckgo.com/ip3/$domain.ico",
                "https://$domain/favicon.ico",
                "https://favicon.yandex.net/favicon/$domain"
            )
            
            for (url in sources) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 2500
                    connection.readTimeout = 2500
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.doInput = true
                    connection.connect()
                    
                    if (connection.responseCode == 200) {
                        val contentType = connection.contentType ?: ""
                        if (contentType.contains("image") || 
                            url.endsWith(".ico") || 
                            url.endsWith(".png")) {
                            val stream = connection.inputStream
                            val decoded = BitmapFactory.decodeStream(stream)
                            stream.close()
                            if (decoded != null && decoded.width > 1 && decoded.height > 1) {
                                bitmap = decoded
                                break
                            }
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) { 
                    continue 
                }
            }
            isLoading = false
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(), 
                    contentDescription = null, 
                    contentScale = ContentScale.Fit, 
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
            else -> {
                Text(
                    text = domain.firstOrNull()?.uppercase(Locale.getDefault()) ?: "?", 
                    fontWeight = FontWeight.Bold, 
                    color = textColor, 
                    fontSize = 18.sp
                )
            }
        }
    }
}