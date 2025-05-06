package myndfulnes.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

@Composable
fun App() {
    val deviceLocker = getDeviceLocker()
    var isSessionActive by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(20 * 60) } // 20 minutes in seconds
    
    // Long press variables
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    // Animation for button size with improved spring animation
    val buttonSize by animateFloatAsState(
        targetValue = if (isLongPressing) 100f + (longPressProgress * 101f) else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )
    
    // Breathing animation for the main button when not in session
    val breatheAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Enhanced calm, dark color palette
    val darkBlue = Color(0xFF0A1128)
    val deepIndigo = Color(0xFF1C2541)
    val calmAccent = Color(0xFFADD8E6) // Light blue
    val accentSecondary = Color(0xFF90EE90) // Light green
    
    // Gradient background
    val gradientBackground = Brush.linearGradient(
        colors = listOf(
            darkBlue,
            deepIndigo
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
    
    // Background color that transitions during long press
    val currentBackgroundColors = listOf(
        Color(
            red = darkBlue.red * (1 - longPressProgress) + 0.5f * longPressProgress,
            green = darkBlue.green * (1 - longPressProgress) + 0.5f * longPressProgress,
            blue = darkBlue.blue * (1 - longPressProgress) + 0.7f * longPressProgress
        ),
        Color(
            red = deepIndigo.red * (1 - longPressProgress) + 0.7f * longPressProgress,
            green = deepIndigo.green * (1 - longPressProgress) + 0.7f * longPressProgress,
            blue = deepIndigo.blue * (1 - longPressProgress) + 0.9f * longPressProgress
        )
    )
    
    val currentGradient = Brush.linearGradient(
        colors = currentBackgroundColors,
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
    
    // Button gradient that transitions during press
    val buttonGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF1D2951),
            Color(0xFF0F1629)
        )
    )
    
    // Current button gradient that transitions during press
    val currentButtonGradient = Brush.radialGradient(
        colors = listOf(
            Color(
                red = 0.11f * (1 - longPressProgress) + 0.7f * longPressProgress,
                green = 0.16f * (1 - longPressProgress) + 0.7f * longPressProgress,
                blue = 0.32f * (1 - longPressProgress) + 0.9f * longPressProgress
            ),
            Color(
                red = 0.06f * (1 - longPressProgress) + 0.5f * longPressProgress,
                green = 0.09f * (1 - longPressProgress) + 0.5f * longPressProgress,
                blue = 0.16f * (1 - longPressProgress) + 0.7f * longPressProgress
            )
        )
    )
    
    // Ripple effect animation for the main button
    val rippleSize by animateFloatAsState(
        targetValue = if (isLongPressing) 1.2f else 0f,
        animationSpec = tween(1000)
    )
    
    // Border animation
    val borderGlow by rememberInfiniteTransition().animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Current border color with glow effect
    val currentBorderColor = if (isSessionActive) {
        calmAccent.copy(alpha = (1 - longPressProgress * 0.5f) * borderGlow)
    } else {
        calmAccent.copy(alpha = borderGlow)
    }
    
    // Progress indicator color transition
    val progressColor = lerp(
        calmAccent,
        accentSecondary,
        longPressProgress
    )

    // Timer effect
    LaunchedEffect(isSessionActive) {
        if (isSessionActive) {
            // Update session state and lock device
            deviceLocker.setSessionActive(true)
            deviceLocker.lockDevice()
            
            // Count down timer
            while (timeLeft > 0 && isSessionActive) {
                delay(1000)
                timeLeft--
            }
            
            // Session complete or interrupted
            if (timeLeft == 0) {
                isSessionActive = false
                deviceLocker.setSessionActive(false)
                deviceLocker.unlockDevice()
            }
        } else {
            // Update session state and unlock device
            deviceLocker.setSessionActive(false)
            deviceLocker.unlockDevice()
            
            timeLeft = 20 * 60
        }
    }

    // Pulsating animation for the timer
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isSessionActive) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    MaterialTheme(colors = darkColors(
        primary = calmAccent,
        background = darkBlue,
        surface = darkBlue,
        onPrimary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isSessionActive) currentGradient else gradientBackground),
            contentAlignment = Alignment.Center
        ) {
            if (!isSessionActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Myndfulness",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = calmAccent,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Ripple effect behind the button
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Main button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(220.dp * breatheAnimation)
                                .shadow(10.dp, CircleShape)
                                .clip(CircleShape)
                                .background(buttonGradient)
                                .border(4.dp, currentBorderColor, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { isSessionActive = true }
                                    )
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "🧘", 
                                    fontSize = 90.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                    
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(40.dp))
                    
                    
                    // Timer display with pulsating animation
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(260.dp * pulseAnimation)
                            .graphicsLayer {
                                scaleX = pulseAnimation
                                scaleY = pulseAnimation
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        deepIndigo.copy(alpha = 0.7f),
                                        darkBlue.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .border(4.dp, currentBorderColor, CircleShape)
                    ) {
                        Text(
                            text = timeLeft.let { seconds ->
                                val min = floor(seconds / 60.0).toInt()
                                val sec = seconds % 60
                                "%02d:%02d".format(min, sec)
                            },
                            fontSize = 70.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(bottom = 40.dp)
                    ) {
                        // Background ripple effect
                        if (isLongPressing && rippleSize > 0) {
                            Box(
                                modifier = Modifier
                                    .size((buttonSize * 1.01f).dp)
                                    .scale(rippleSize)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                progressColor.copy(alpha = 0.4f),
                                                progressColor.copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                            )
                        }
                        
                        // Progress ring
                        if (isLongPressing && longPressProgress > 0) {
                            CircularProgressIndicator(
                                progress = longPressProgress,
                                modifier = Modifier.size((buttonSize + 20).dp),
                                color = progressColor,
                                strokeWidth = 4.dp
                            )
                        }
                        
                        // Main button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(buttonSize.dp)
                                .clip(CircleShape)
                                .background(currentButtonGradient)
                                .border(2.dp, currentBorderColor, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isLongPressing = true
                                            longPressProgress = 0f
                                            
                                            coroutineScope.launch {
                                                val unlockDuration = 10000L // 10 seconds to unlock
                                                val startTime = System.currentTimeMillis()
                                                
                                                while (isLongPressing) {
                                                    val elapsedTime = System.currentTimeMillis() - startTime
                                                    longPressProgress = (elapsedTime.toFloat() / unlockDuration).coerceIn(0f, 1f)
                                                    
                                                    if (longPressProgress >= 1f) {
                                                        isSessionActive = false
                                                        break
                                                    }
                                                    
                                                    delay(16) // Update at about 60fps
                                                }
                                            }
                                            
                                            try {
                                                awaitRelease()
                                            } finally {
                                                isLongPressing = false
                                                longPressProgress = 0f
                                            }
                                        }
                                    )
                                }
                        ) {
                            Text(
                                "🔓", 
                                fontSize = 50.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to linearly interpolate between two colors
private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}