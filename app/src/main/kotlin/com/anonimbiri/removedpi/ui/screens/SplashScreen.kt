// ui/screens/SplashScreen.kt
package com.anonimbiri.removedpi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anonimbiri.removedpi.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startLogoAnimation by remember { mutableStateOf(false) }
    var startBrandingAnimation by remember { mutableStateOf(false) }
    
    // Logo animasyonları
    val logoScale by animateFloatAsState(
        targetValue = if (startLogoAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutBack
        ),
        label = "logoScale"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (startLogoAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logoAlpha"
    )
    
    // Pulse animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Branding animasyonları
    val brandingAlpha by animateFloatAsState(
        targetValue = if (startBrandingAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "brandingAlpha"
    )
    
    val brandingTranslateY by animateFloatAsState(
        targetValue = if (startBrandingAnimation) 0f else 30f,
        animationSpec = tween(
            durationMillis = 500,
            easing = EaseOutCubic
        ),
        label = "brandingTranslateY"
    )
    
    LaunchedEffect(key1 = true) {
        delay(100)
        startLogoAnimation = true
        delay(600)
        startBrandingAnimation = true
        delay(1800) // Toplam ~2.5 saniye
        onSplashFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // HomeScreen'deki gibi gradient arka plan
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Ortada Logo ve App Adı
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo - Icon olarak (tint ile renk alabilir)
            Icon(
                painter = painterResource(id = R.drawable.ic_removedpi),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale * pulseScale)
                    .alpha(logoAlpha),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Adı
            Text(
                text = "Remove DPI",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .scale(logoScale)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Alt yazı
            Text(
                text = "DPI Engellerini Kaldır",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.alpha(logoAlpha)
            )
        }
        
        // Altta Branding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(brandingAlpha)
                .graphicsLayer {
                    translationY = brandingTranslateY
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ic_branding drawable
            Image(
                painter = painterResource(id = R.drawable.ic_branding),
                contentDescription = "by: Anonimbiri",
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

// Easing fonksiyonları
private val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutCubic: Easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInOutSine: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)