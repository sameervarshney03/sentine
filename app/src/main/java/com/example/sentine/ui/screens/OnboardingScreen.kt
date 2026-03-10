package com.example.sentine.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sentine.ui.theme.DeepBlue
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> OnboardingPage1()
                1 -> OnboardingPage2()
                2 -> OnboardingPage3(onFinished)
            }
        }

        // Top Skip Button
        if (pagerState.currentPage < 2) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text("SKIP", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Bottom Navigation
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) DeepBlue else Color.Gray.copy(alpha = 0.5f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (pagerState.currentPage < 2) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OnboardingPage1() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "shield_anim")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "scale"
        )

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                color = DeepBlue.copy(alpha = 0.1f)
            ) {}
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(100.dp).graphicsLayer(scaleX = scale, scaleY = scale),
                tint = DeepBlue
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Your Privacy Guardian",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Monitors apps for suspicious background behavior — all on-device, nothing leaves your phone.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun OnboardingPage2() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        launch { 
            repeat(3) {
                kotlinx.coroutines.delay(1000)
                step++
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "How it works",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))

        OnboardingStep(
            visible = step >= 1,
            icon = Icons.Default.NetworkCheck,
            text = "Watches network uploads"
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingStep(
            visible = step >= 2,
            icon = Icons.Default.Visibility,
            text = "Detects silent background activity"
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Gauge simulation
        val scoreAnimate = animateIntAsState(if (step >= 3) 82 else 0, tween(1000), label = "score")
        AnimatedVisibility(visible = step >= 3, enter = fadeIn() + expandVertically()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = DeepBlue)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Scores each app on risk: ${scoreAnimate.value}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OnboardingStep(visible: Boolean, icon: ImageVector, text: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = DeepBlue)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OnboardingPage3(onFinished: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(80.dp), tint = DeepBlue)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Grant Permission",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "To analyze behavior, we need Usage Stats access. We DON'T access your mic, contacts, or messages.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("ENABLE ACCESS", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onFinished) {
            Text("Already enabled? Continue →")
        }
    }
}
