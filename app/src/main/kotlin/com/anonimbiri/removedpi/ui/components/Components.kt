package com.anonimbiri.removedpi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anonimbiri.removedpi.data.VpnState
import com.anonimbiri.removedpi.ui.theme.VpnConnected
import com.anonimbiri.removedpi.ui.theme.VpnConnecting
import com.anonimbiri.removedpi.ui.theme.VpnDisconnected

@Composable
fun VpnConnectionButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.CONNECTED -> VpnConnected
            VpnState.CONNECTING -> VpnConnecting
            VpnState.DISCONNECTED -> MaterialTheme.colorScheme.primary
            VpnState.ERROR -> VpnDisconnected
        },
        animationSpec = tween(300),
        label = "buttonColor"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (vpnState == VpnState.DISCONNECTED) {
            MaterialTheme.colorScheme.onPrimary 
        } else {
            Color.White
        },
        label = "iconTint"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (vpnState == VpnState.CONNECTING) 0.95f else 1f,
        animationSpec = if (vpnState == VpnState.CONNECTING) {
            infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(100)
        },
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size(200.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        buttonColor.copy(alpha = 0.3f),
                        buttonColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(
                    width = 3.dp,
                    color = buttonColor.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
        
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (vpnState) {
                        VpnState.CONNECTED -> Icons.Default.Shield
                        VpnState.CONNECTING -> Icons.Default.Sync
                        VpnState.DISCONNECTED -> Icons.Default.PowerSettingsNew
                        VpnState.ERROR -> Icons.Default.Error
                    },
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = iconTint
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    vpnState: VpnState,
    packetsProcessed: Long,
    bytesIn: Long,
    bytesOut: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (vpnState) {
                                VpnState.CONNECTED -> VpnConnected
                                VpnState.CONNECTING -> VpnConnecting
                                VpnState.DISCONNECTED -> VpnDisconnected
                                VpnState.ERROR -> VpnDisconnected
                            }
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "Bağlı"
                        VpnState.CONNECTING -> "Bağlanıyor..."
                        VpnState.DISCONNECTED -> "Bağlı Değil"
                        VpnState.ERROR -> "Hata"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            if (vpnState == VpnState.CONNECTED) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Outlined.Sync,
                        label = "Paketler",
                        value = formatNumber(packetsProcessed)
                    )
                    StatItem(
                        icon = Icons.Outlined.ArrowDownward,
                        label = "İndirme",
                        value = formatBytes(bytesIn)
                    )
                    StatItem(
                        icon = Icons.Outlined.ArrowUpward,
                        label = "Yükleme",
                        value = formatBytes(bytesOut)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// === HELPER FUNCTIONS (BURAYA EKLENDİ) ===

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number < 1000 -> "$number"
        number < 1000000 -> "${number / 1000}K"
        else -> "${number / 1000000}M"
    }
}