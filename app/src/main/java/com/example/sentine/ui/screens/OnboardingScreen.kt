package com.example.sentine.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentine.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Skip Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Skip",
                    color = TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { onFinished() }
                        .padding(8.dp),
                    fontFamily = AppTypography.bodyLarge.fontFamily
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> PageOne()
                1 -> PageTwo(isVisible = pagerState.currentPage == 1)
                2 -> PageThree(onFinished = onFinished)
            }
        }

        // Bottom Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val isActive = index == pagerState.currentPage
                    val width by animateDpAsState(if (isActive) 24.dp else 8.dp, label = "dotWidth")
                    val color = if (isActive) Primary else TextMuted
                    
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            if (pagerState.currentPage < 2) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = if (pagerState.currentPage == 0) "Get Started" else "How It Works",
                        color = Color.White,
                        style = AppTypography.titleLarge
                    )
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Grant Permission", color = Color.White, style = AppTypography.titleLarge)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Already granted? Continue →",
                    color = Primary,
                    style = AppTypography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onFinished() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun IconHeader(icon: ImageVector) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .blur(32.dp)
                .background(PrimaryDim, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
fun PageOne() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconHeader(Icons.Filled.Security)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Your Phone's\nPrivacy Guardian",
            style = AppTypography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SentinelAI watches for apps behaving suspiciously in the background — silently protecting you 24/7",
            style = AppTypography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PageTwo(isVisible: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconHeader(Icons.Filled.Visibility)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "See What Apps\nAre Really Doing",
            style = AppTypography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "We detect unusual network uploads, background activity, and silent data transfers — all on your device",
            style = AppTypography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 32.dp)) {
            val features = listOf("📡" to "Network monitoring", "🔍" to "Behavioral analysis", "🤖" to "AI risk scoring")
            
            features.forEachIndexed { index, (emoji, text) ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        delay(index * 300L)
                        visible = true
                    } else {
                        visible = false
                    }
                }
                
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500)) + slideInHorizontally(tween(500)) { -100 }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text, style = AppTypography.titleLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun PageThree(onFinished: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconHeader(Icons.Filled.Lock)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "One Permission\nThat's All We Need",
            style = AppTypography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Usage Access lets us see which apps run in background. We never access your messages, photos, or personal data.",
            style = AppTypography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceVariant)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row {
                    Text("✅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("We CAN see", style = AppTypography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("App background activity\nNetwork usage stats", style = AppTypography.bodyLarge, color = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text("❌", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("We CANNOT see", style = AppTypography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Messages or calls\nPhotos or files\nPasswords or accounts", style = AppTypography.bodyLarge, color = TextSecondary)
                    }
                }
            }
        }
    }
}
